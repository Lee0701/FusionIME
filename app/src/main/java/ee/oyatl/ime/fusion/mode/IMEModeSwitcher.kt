package ee.oyatl.ime.fusion.mode

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ExpandedCandidateView
import ee.oyatl.ime.fusion.PreferenceUtil
import ee.oyatl.ime.fusion.databinding.InputViewWrapperBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBarBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBinding
import kotlin.math.ceil
import androidx.core.view.isVisible

class IMEModeSwitcher(
    private val context: Context,
    val entries: List<Entry>,
    private val callback: Callback
) {
    val handler = Handler(Looper.getMainLooper())

    val size: Int get() = entries.size
    var currentModeIndex: Int = 0
        private set
    private val currentEntry: Entry get() = entries[currentModeIndex]
    val currentMode: IMEMode get() = currentEntry.imeMode

    private var inputViewWrapper: InputViewWrapperBinding? = null
    private var tabs: List<ModeSwitcherTabBinding> = listOf()
    private var clipboardText: String? = null

    val expandedCandidateView: ExpandedCandidateView? get() = inputViewWrapper?.expandedCandidateView

    private val expandAnimator: Animator get() = AnimatorSet().apply {
        val inputViewWrapper = inputViewWrapper ?: return@apply
        playTogether(
            ObjectAnimator.ofFloat(inputViewWrapper.keyboardView, "translationY", 200f),
            ObjectAnimator.ofFloat(inputViewWrapper.keyboardView, "alpha", 0f),
            ObjectAnimator.ofFloat(inputViewWrapper.expandButton, "rotation", 180f)
        )
        duration = EXPAND_COLLAPSE_DURATION
        interpolator = AccelerateDecelerateInterpolator()
        doOnStart {
            animationIsRunning = true
        }
        doOnEnd {
            inputViewWrapper.keyboardView.visibility = View.INVISIBLE
            candidateViewExpanded = true
            animationIsRunning = false
        }
    }

    private val collapseAnimator get() = AnimatorSet().apply {
        val inputViewWrapper = inputViewWrapper ?: return@apply
        playTogether(
            ObjectAnimator.ofFloat(inputViewWrapper.keyboardView, "translationY", 0f),
            ObjectAnimator.ofFloat(inputViewWrapper.keyboardView, "alpha", 1f),
            ObjectAnimator.ofFloat(inputViewWrapper.expandButton, "rotation", 0f)
        )
        duration = EXPAND_COLLAPSE_DURATION
        interpolator = AccelerateDecelerateInterpolator()
        doOnStart {
            animationIsRunning = true
            inputViewWrapper.keyboardView.visibility = View.VISIBLE
        }
        doOnEnd {
            inputViewWrapper.keyboardView.requestLayout()
            candidateViewExpanded = false
            animationIsRunning = false
        }
    }

    private var candidateViewExpanded: Boolean = false
    private var animationIsRunning: Boolean = false

    private var inputConnection: InputConnection? = null
    private var editorInfo: EditorInfo? = null

    private val preference: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.inputConnection = inputConnection
        this.editorInfo = editorInfo
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
        showTabBar()
    }

    fun onFinish() {
        this.inputConnection = null
        this.editorInfo = null
        currentEntry.imeMode.onFinish()
    }

    fun createInputView(): View {
        val inflater = LayoutInflater.from(context)
        val inputView = InputViewWrapperBinding.inflate(inflater)

        inputView.tabViewFrame.addView(this.initTabBarView(context))
        inputView.closeButton.setOnClickListener {
            if(animationIsRunning) return@setOnClickListener
            collapseCandidateView()
            if(inputView.idleView.isVisible && inputView.clipboardCandidate.isVisible) {
                hideClipboardCandidateView(inputView)
            } else {
                showTabBar()
            }
        }
        inputView.expandButton.setOnClickListener {
            if(animationIsRunning) return@setOnClickListener
            if(!candidateViewExpanded) expandCandidateView()
            else collapseCandidateView()
        }

        inputView.clipboardCandidate.setOnClickListener {
            clipboardText?.let(callback::onClipboardCandidateSelected)
        }
        inputView.clipboardCandidate.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            hideClipboardCandidateView(inputView)
            true
        }

        @SuppressLint("ClickableViewAccessibility")
        inputView.touchBlocker.setOnTouchListener { _, event ->
            // Intercept touch events to input view while blocking
            inputView.keyboardView.dispatchTouchEvent(event)
        }

        // Make expanded candidate view 1dp shorter than keyboard view
        // because actual keyboard size may be smaller
        val oneDp = ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)).toInt()
        inputView.expandedCandidateView.layoutParams = FrameLayout.LayoutParams(
            context.resources.displayMetrics.widthPixels,
            PreferenceUtil.getKeyboardHeight(context) - oneDp
        )
        inputView.expandedCandidateView.listener = object : CandidateView.Listener {
            override fun onCandidateSelected(candidate: CandidateView.Candidate) {
                val currentMode = currentMode
                if(currentMode is CandidateView.Listener) currentMode.onCandidateSelected(candidate)
            }
        }

        this.inputViewWrapper = inputView
        return inputView.root
    }

    fun resetInputViews() {
        entries.forEach { it.inputView = null }
    }

    fun resetCandidateViews() {
        entries.forEach { it.candidateView = null }
    }

    private fun updateInputView() {
        val wrapper = inputViewWrapper ?: return

        wrapper.keyboardView.removeAllViews()
        val inputView = currentEntry.inputView ?: currentEntry.imeMode.createInputView(context)
        currentEntry.inputView = inputView
        (inputView.parent as ViewGroup?)?.removeView(inputView)
        wrapper.keyboardView.addView(inputView)

        wrapper.candidateView.removeAllViews()
        val view = currentEntry.candidateView ?: currentEntry.imeMode.createCandidateView(context)
        currentEntry.candidateView = view
        (view.parent as ViewGroup?)?.removeView(view)
        wrapper.candidateView.addView(view)

        val alwaysShowSoftKeyboard = preference.getBoolean("always_show_soft_keyboard", false)
        val hardwareKeyboard = context.resources.configuration.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        inputView.visibility = if(alwaysShowSoftKeyboard || !hardwareKeyboard) View.VISIBLE else View.GONE
    }

    fun switchMode(index: Int) {
        val inputConnection = this.inputConnection ?: return
        val editorInfo = this.editorInfo ?: return
        currentEntry.imeMode.onFinish()
        currentModeIndex = index
        updateInputView()
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
        tabs.forEach { it.root.isSelected = false }
        tabs[index].root.isSelected = true
    }

    fun showCandidates() {
        val inputView = inputViewWrapper ?: return
        inputView.clipboardCandidate.stopScrolling()
        inputView.candidateView.visibility = View.VISIBLE
        inputView.tabViewFrame.visibility = View.GONE
        // Block touch events while view height is being changed
        inputView.touchBlocker.visibility = View.VISIBLE
        handler.postDelayed({ inputView.touchBlocker.visibility = View.GONE }, SWITCH_DELAY)
    }

    fun setClipboardCandidate(text: String?) {
        clipboardText = text
        inputViewWrapper?.let(::updateClipboardCandidateView)
    }

    private fun updateClipboardCandidateView(inputView: InputViewWrapperBinding) {
        var displayChanged = false
        inputView.clipboardCandidate.apply {
            val newText = clipboardText.orEmpty()
            if(text.toString() != newText) {
                stopScrolling()
                text = newText
                showClipboardIcon()
                displayChanged = true
            }
            val newVisibility = if(clipboardText == null) View.GONE else View.VISIBLE
            if(visibility != newVisibility) {
                visibility = newVisibility
                displayChanged = true
            }
        }
        if(displayChanged) updateClipboardScrolling(inputView)
    }

    private fun hideClipboardCandidateView(inputView: InputViewWrapperBinding) {
        inputView.clipboardCandidate.showDeleteIcon()
        if(!callback.onClipboardCandidateClearRequested()) {
            inputView.clipboardCandidate.showClipboardIcon()
        }
    }

    private fun updateClipboardScrolling(inputView: InputViewWrapperBinding) {
        val textView = inputView.clipboardCandidate
        if(clipboardText != null && inputView.idleView.isVisible) {
            textView.startScrolling()
        } else {
            textView.stopScrolling()
        }
    }

    fun showTabBar() {
        val inputView = inputViewWrapper ?: return
        inputView.tabViewFrame.visibility = View.VISIBLE
        inputView.candidateView.visibility = View.GONE
        updateClipboardScrolling(inputView)
    }

    fun initTabBarView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)
        val tabBar = ModeSwitcherTabBarBinding.inflate(layoutInflater, null, false)
        val showVoiceInputButton = preference.getBoolean("show_voice_input_button", false)
        tabBar.voiceInputButton.visibility = View.GONE
        if(showVoiceInputButton) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val entry = imm.shortcutInputMethodsAndSubtypes?.entries?.firstOrNull()
            if(entry != null) {
                val shortcutIme = entry.key
                val shortcutSubtype = entry.value.firstOrNull()
                if(shortcutSubtype != null) {
                    tabBar.voiceInputButton.visibility = View.VISIBLE
                    tabBar.voiceInputButton.setOnClickListener {
                        callback.onSwitchInputMethod(shortcutIme.id, shortcutSubtype)
                    }
                }
            }
        }
        tabs = entries.mapIndexed { index, entry ->
            val tab = ModeSwitcherTabBinding.inflate(layoutInflater, tabBar.content, true)
            tab.label.text = entry.label
            tab.root.setOnClickListener {
                callback.onSwitchInputMode(index)
            }
            return@mapIndexed tab
        }
        return tabBar.root
    }

    fun expandCandidateView() {
        expandAnimator.start()
    }

    fun collapseCandidateView() {
        collapseAnimator.start()
    }

    interface Callback {
        fun onSwitchInputMode(index: Int)
        fun onSwitchInputMethod(id: String, subtype: InputMethodSubtype)
        fun onClipboardCandidateSelected(text: String)
        fun onClipboardCandidateClearRequested(): Boolean
    }

    data class Entry(
        val label: String,
        val imeMode: IMEMode
    ) {
        internal var inputView: View? = null
        internal var candidateView: View? = null
    }

    companion object {
        const val SWITCH_DELAY: Long = 10
        const val EXPAND_COLLAPSE_DURATION: Long = 300
    }
}
