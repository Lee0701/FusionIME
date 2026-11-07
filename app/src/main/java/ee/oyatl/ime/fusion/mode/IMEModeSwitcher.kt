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
import android.util.TypedValue
import android.view.HapticFeedbackConstants
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
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ExpandedCandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.cjkv.CjkvDictionaryViewState
import ee.oyatl.ime.fusion.cjkv.CjkvDictionaryLanguage
import ee.oyatl.ime.fusion.cjkv.CjkvDictionaryPresentation
import ee.oyatl.ime.fusion.cjkv.CjkvGlyphLocale
import ee.oyatl.ime.fusion.util.PreferenceUtil
import ee.oyatl.ime.fusion.databinding.InputViewWrapperBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBarBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBinding
import ee.oyatl.ime.theme.CandidateItemType
import ee.oyatl.ime.theme.KeyboardTheme
import kotlin.math.ceil

class IMEModeSwitcher(
    private val context: Context,
    val entries: List<Entry>,
    var keyboardTheme: KeyboardTheme,
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
    private var dictionaryTab: ModeSwitcherTabBinding? = null
    private var dictionaryVisible = false
    private var keyboardVisibilityBeforeDictionary = View.VISIBLE
    private var dictionaryKeyboardHeight = 0
    // The tallest entry fitted since the panel opened; it does not shrink until closed.
    private var dictionaryFitHeight = 0
    private var selectionStart = -1
    private var selectionEnd = -1
    // The candidate the dictionary was opened from, while its conversion is still going.
    private var dictionaryCandidate: CandidateView.Candidate? = null
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
            inputViewWrapper.expandedCandidateView.visibility = View.VISIBLE
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
            inputViewWrapper.expandedCandidateView.visibility = View.GONE
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
        closeDictionaryPanel()
        this.inputConnection = inputConnection
        this.editorInfo = editorInfo
        selectionStart = editorInfo.initialSelStart
        selectionEnd = editorInfo.initialSelEnd
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
        showTabBar()
    }

    fun onFinish() {
        closeDictionaryPanel()
        this.inputConnection = null
        this.editorInfo = null
        currentEntry.imeMode.onFinish()
    }

    fun createInputView(): View {
        val inflater = LayoutInflater.from(context)
        val inputView = InputViewWrapperBinding.inflate(inflater)

        inputView.expandedCandidateView.keyboardTheme = keyboardTheme
        inputView.dictionaryPanel.applyTheme(keyboardTheme)
        inputView.dictionaryPanel.onTextSelected = { text ->
            val mode = currentMode as? CommonIMEMode
            if(dictionaryCandidate != null) {
                // Opened from a candidate (製造): drop the conversion and type only the character
                // being shown, then the tapped text after a space (造 zào).
                callback.onDictionaryCompositionDiscarded()
                mode?.onReset()
                val character = inputView.dictionaryPanel.currentCharacter
                callback.onDictionaryTextSelected(
                    if(character == null || character == text) text else "$character $text"
                )
            } else {
                // Settle any composition first, so the tapped text goes after it instead of replacing it.
                mode?.onReset()
                callback.onDictionaryTextSelected(text)
            }
            closeDictionaryPanel()
        }
        inputView.dictionaryPanel.onLookupRequested = callback::onDictionaryLookup
        inputView.dictionaryPanel.onCloseRequested = ::closeDictionaryPanel
        inputView.dictionaryPanel.onLayoutNeedsFit = {
            if(dictionaryVisible) applyDictionaryHeight(inputView)
        }

        val itemTheme = keyboardTheme.candidatesTheme.itemThemes.getValue(CandidateItemType.Default)
        inputView.idleView.setBackgroundColor(context.getColor(keyboardTheme.tabBarTheme.backgroundColor))
        inputView.buttonsView.setBackgroundColor(context.getColor(keyboardTheme.tabBarTheme.backgroundColor))
        inputView.expandedCandidateView.setBackgroundColor(context.getColor(keyboardTheme.candidatesTheme.backgroundColor))
        inputView.clipboardCandidateText.setTextColor(context.getColor(itemTheme.foregroundColor))
        TextViewCompat.setCompoundDrawableTintList(
            inputView.clipboardCandidateText,
            context.getColorStateList(itemTheme.foregroundColor)
        )
        inputView.clipboardCandidateCloseButton.imageTintList = context.getColorStateList(itemTheme.foregroundColor)
        inputView.closeButton.imageTintList = context.getColorStateList(itemTheme.foregroundColor)
        inputView.expandButton.imageTintList = context.getColorStateList(itemTheme.foregroundColor)
        inputView.voiceInputButton.setBackgroundColor(context.getColor(keyboardTheme.backgroundColor))
        inputView.voiceInputButton.imageTintList = context.getColorStateList(itemTheme.foregroundColor)

        inputView.tabViewFrame.addView(this.initTabBarView(context))
        inputView.closeButton.setOnClickListener {
            if(animationIsRunning) return@setOnClickListener
            collapseCandidateView()
            showTabBar()
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
        inputView.clipboardCandidateCloseButton.setOnClickListener {
            hideClipboardCandidateView(inputView)
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
        inputView.dictionaryPanel.layoutParams = FrameLayout.LayoutParams(
            context.resources.displayMetrics.widthPixels,
            PreferenceUtil.getKeyboardHeight(context) - oneDp
        )
        inputView.expandedCandidateView.listener = object : CandidateView.Listener {
            override fun onCandidateSelected(candidate: CandidateView.Candidate) {
                val currentMode = currentMode
                if(currentMode is CandidateView.Listener) currentMode.onCandidateSelected(candidate)
            }

            override fun onCandidateLongPressed(candidate: CandidateView.Candidate) {
                val currentMode = currentMode
                if(currentMode is CandidateView.Listener) currentMode.onCandidateLongPressed(candidate)
            }
        }

        val showVoiceInputButton = preference.getBoolean("show_voice_input_button", false)
        inputView.voiceInputButton.visibility = View.GONE
        if(showVoiceInputButton) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val entry = imm.shortcutInputMethodsAndSubtypes?.entries?.firstOrNull()
            if(entry != null) {
                val shortcutIme = entry.key
                val shortcutSubtype = entry.value.firstOrNull()
                if(shortcutSubtype != null) {
                    inputView.voiceInputButton.visibility = View.VISIBLE
                    inputView.voiceInputButton.setOnClickListener {
                        callback.onSwitchInputMethod(shortcutIme.id, shortcutSubtype)
                    }
                }
            }
        }

        this.inputViewWrapper = inputView
        dictionaryVisible = false
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

        currentEntry.imeMode.keyboardTheme = keyboardTheme

        wrapper.keyboardView.removeAllViews()
        val inputView = currentEntry.inputView ?: currentEntry.imeMode.createInputView(context)
        currentEntry.inputView = inputView
        (inputView.parent as ViewGroup?)?.removeView(inputView)
        wrapper.keyboardView.addView(inputView)

        wrapper.candidateView.removeAllViews()
        val candidateView = currentEntry.candidateView ?: currentEntry.imeMode.createCandidateView(context)
        currentEntry.candidateView = candidateView
        (candidateView.parent as ViewGroup?)?.removeView(candidateView)
        wrapper.candidateView.addView(candidateView)

        val alwaysShowSoftKeyboard = preference.getBoolean("always_show_soft_keyboard", false)
        val hardwareKeyboard = context.resources.configuration.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        inputView.visibility = if(alwaysShowSoftKeyboard || !hardwareKeyboard) View.VISIBLE else View.GONE
    }

    fun switchMode(index: Int) {
        val inputConnection = this.inputConnection ?: return
        val editorInfo = this.editorInfo ?: return
        closeDictionaryPanel()
        currentEntry.imeMode.onFinish()
        currentModeIndex = index
        updateInputView()
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
        tabs.forEach { it.root.isSelected = false }
        tabs[index].root.isSelected = true
    }

    fun showCandidates() {
        // The dictionary stays open: it can be opened from a candidate, and has its own close button.
        val inputView = inputViewWrapper ?: return
        inputView.clipboardCandidate.stopScrolling()
        inputView.candidateView.visibility = View.VISIBLE
        inputView.idleView.visibility = View.GONE
        // Block touch events while view height is being changed
        inputView.touchBlocker.visibility = View.VISIBLE
        handler.postDelayed({ inputView.touchBlocker.visibility = View.GONE }, SWITCH_DELAY)
    }

    fun showTabBar() {
        val inputView = inputViewWrapper ?: return
        inputView.idleView.visibility = View.VISIBLE
        inputView.candidateView.visibility = View.GONE
        updateClipboardScrolling(inputView)
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
                displayChanged = true
            }
        }
        inputView.clipboardCandidateWrapper.apply {
            val newVisibility = if(clipboardText == null) View.GONE else View.VISIBLE
            if(visibility != newVisibility) {
                visibility = newVisibility
                displayChanged = true
            }
        }
        if(displayChanged) updateClipboardScrolling(inputView)
    }

    private fun hideClipboardCandidateView(inputView: InputViewWrapperBinding) {
        callback.onClipboardCandidateClearRequested()
    }

    private fun updateClipboardScrolling(inputView: InputViewWrapperBinding) {
        val textView = inputView.clipboardCandidate
        if(clipboardText != null && inputView.idleView.isVisible) {
            textView.startScrolling()
        } else {
            textView.stopScrolling()
        }
    }

    fun initTabBarView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)
        val tabBar = ModeSwitcherTabBarBinding.inflate(layoutInflater, null, false)
        tabs = entries.mapIndexed { index, entry ->
            val tab = ModeSwitcherTabBinding.inflate(layoutInflater, tabBar.content, true)

            val theme = keyboardTheme.tabBarTheme.itemTheme
            tab.root.setBackgroundResource(theme.backgroundDrawable)
            tab.root.backgroundTintList = context.getColorStateList(theme.backgroundColor)
            tab.label.setTextColor(context.getColor(theme.foregroundColor))

            tab.label.text = entry.label
            tab.root.setOnClickListener {
                if(dictionaryVisible && index == currentModeIndex) closeDictionaryPanel()
                else callback.onSwitchInputMode(index)
            }
            return@mapIndexed tab
        }
        dictionaryTab = ModeSwitcherTabBinding.inflate(layoutInflater, tabBar.content, true).apply {
            val theme = keyboardTheme.tabBarTheme.itemTheme
            root.setBackgroundResource(theme.backgroundDrawable)
            root.backgroundTintList = context.getColorStateList(theme.backgroundColor)
            label.setTextColor(context.getColor(theme.foregroundColor))
            label.setText(R.string.cjkv_dictionary_tab)
            root.setOnClickListener { openDictionaryPanel() }
        }
        return tabBar.root
    }

    private fun openDictionaryPanel() {
        val inputView = inputViewWrapper ?: return
        dictionaryCandidate = null
        inputView.dictionaryPanel.render(callback.onDictionaryRequested(), dictionaryPresentation())
        showDictionaryPanel(inputView)
    }

    /** Opens the dictionary on a candidate long-pressed before committing it. */
    fun openDictionaryPanel(candidate: CandidateView.Candidate) {
        val inputView = inputViewWrapper ?: return
        inputView.dictionaryPanel.renderWord(candidate.text.toString(), dictionaryPresentation())
        showDictionaryPanel(inputView)
        dictionaryCandidate = candidate
    }

    private fun showDictionaryPanel(inputView: InputViewWrapperBinding) {
        // The keyboard is already hidden when the expanded candidate list is open.
        if(!dictionaryVisible) {
            keyboardVisibilityBeforeDictionary = inputView.keyboardView.visibility
            // Take exactly the space the keyboard had, so the window does not change size.
            val keyboardHeight = inputView.keyboardView.height
            if(keyboardHeight > 0) dictionaryKeyboardHeight = keyboardHeight
            dictionaryFitHeight = 0
            applyDictionaryHeight(inputView)
        }
        inputView.keyboardView.visibility = View.GONE
        inputView.dictionaryPanel.visibility = View.VISIBLE
        dictionaryVisible = true
        tabs.forEach { it.root.isSelected = false }
        dictionaryTab?.root?.isSelected = true
    }

    /**
     * Follows the cursor while the dictionary is open, showing the character before it.
     *
     * The dictionary closes once something is typed or committed: a tapped item
     * closes it at once, and a dictionary opened from a candidate closes when the
     * conversion ends, however the candidate was chosen.
     */
    fun onSelectionChanged(newSelStart: Int, newSelEnd: Int, composing: Boolean) {
        selectionStart = newSelStart
        selectionEnd = newSelEnd
        // While converting, the panel may be showing a candidate; the cursor is not what to look up.
        if(composing || !dictionaryVisible) return
        if(dictionaryCandidate != null) {
            closeDictionaryPanel()
            return
        }
        val inputView = inputViewWrapper ?: return
        inputView.dictionaryPanel.render(callback.onDictionaryRequested(), dictionaryPresentation())
    }

    private fun dictionaryPresentation(): CjkvDictionaryPresentation {
        val languages = mutableListOf<CjkvDictionaryLanguage>()
        val glyphs = mutableListOf<CjkvGlyphLocale>()
        entries.forEach { entry ->
            when(val mode = entry.imeMode) {
                is MozcIMEMode -> {
                    languages += CjkvDictionaryLanguage.Japanese
                    glyphs += CjkvGlyphLocale.Japanese
                }
                is KoreanIMEMode -> {
                    languages += CjkvDictionaryLanguage.Korean
                    glyphs += CjkvGlyphLocale.Korean
                }
                is PinyinIMEMode -> {
                    languages += CjkvDictionaryLanguage.Mandarin
                    glyphs += if(mode.chineseTraditional) {
                        CjkvGlyphLocale.TraditionalChinese
                    } else {
                        CjkvGlyphLocale.SimplifiedChinese
                    }
                }
                is ZhuyinIMEMode, is CangjieIMEMode -> {
                    languages += CjkvDictionaryLanguage.Mandarin
                    glyphs += CjkvGlyphLocale.TraditionalChinese
                }
                is JyutpingIMEMode -> {
                    languages += CjkvDictionaryLanguage.Cantonese
                    glyphs += CjkvGlyphLocale.HongKongChinese
                }
                is VietIMEMode -> {
                    languages += CjkvDictionaryLanguage.Vietnamese
                    glyphs += CjkvGlyphLocale.Vietnamese
                }
            }
        }
        return CjkvDictionaryPresentation(
            languageOrder = (languages + CjkvDictionaryLanguage.entries).distinct(),
            glyphOrder = (glyphs + CjkvGlyphLocale.entries).distinct(),
        )
    }

    private fun closeDictionaryPanel() {
        dictionaryCandidate = null
        hideDictionaryPanel()
    }

    /**
     * The panel is as tall as the keyboard it replaces. Expanded, it grows to fit
     * its entry, up to where the whole input view (candidate bar included) takes
     * a share of the screen; it does not shrink back while open, so moving to a
     * shorter entry does not make the app above jump.
     */
    private fun applyDictionaryHeight(inputView: InputViewWrapperBinding) {
        val panel = inputView.dictionaryPanel
        val base = dictionaryKeyboardHeight.takeIf { it > 0 } ?: panel.layoutParams.height
        if(!panel.expanded) {
            dictionaryFitHeight = 0
            setDictionaryHeight(inputView, base)
            return
        }
        dictionaryFitHeight = maxOf(dictionaryFitHeight, panel.fittingHeight())
        val max = (context.resources.displayMetrics.heightPixels * DICTIONARY_MAX_SCREEN_SHARE).toInt() -
            inputView.buttonsView.height
        setDictionaryHeight(inputView, maxOf(base, minOf(dictionaryFitHeight, max)))
    }

    private fun setDictionaryHeight(inputView: InputViewWrapperBinding, height: Int) {
        if(height > 0 && inputView.dictionaryPanel.layoutParams.height != height) {
            inputView.dictionaryPanel.layoutParams = inputView.dictionaryPanel.layoutParams.apply { this.height = height }
        }
    }

    /** Puts the keyboard back in place of the dictionary. */
    private fun hideDictionaryPanel() {
        if(!dictionaryVisible) return
        val inputView = inputViewWrapper ?: return
        inputView.dictionaryPanel.visibility = View.GONE
        inputView.keyboardView.visibility = keyboardVisibilityBeforeDictionary
        dictionaryVisible = false
        dictionaryTab?.root?.isSelected = false
        tabs.getOrNull(currentModeIndex)?.root?.isSelected = true
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
        fun onDictionaryRequested(): CjkvDictionaryViewState
        fun onDictionaryTextSelected(text: String)
        /** Drops the text being composed without committing it. */
        fun onDictionaryCompositionDiscarded()
        fun onDictionaryLookup(text: String): CjkvDictionaryViewState?
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
        private const val DICTIONARY_MAX_SCREEN_SHARE = 0.8f
    }
}
