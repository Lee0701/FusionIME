package ee.oyatl.ime.fusion.mode

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import ee.oyatl.ime.fusion.databinding.CandidateViewWrapperBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBarBinding
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBinding
import ee.oyatl.ime.keyboard.PaleViewFilter

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

    private var inputView: FrameLayout? = null
    private var candidateView: CandidateViewWrapperBinding? = null
    private var tabs: List<ModeSwitcherTabBinding> = listOf()
    private var tabBarContent: View? = null
    private val tabBarFilter = PaleViewFilter()

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
        val inputView = FrameLayout(context)
        this.inputView = inputView
        return inputView
    }

    fun createCandidateView(): View {
        val inflater = LayoutInflater.from(context)
        val candidateView = CandidateViewWrapperBinding.inflate(inflater)
        candidateView.tabViewFrame.addView(this.initTabBarView(context))
        candidateView.closeButton.setOnClickListener { showTabBar() }
        @SuppressLint("ClickableViewAccessibility")
        candidateView.touchBlocker.setOnTouchListener { _, event ->
            // Intercept touch events to input view while blocking
            inputView?.dispatchTouchEvent(event) ?: false
        }
        this.candidateView = candidateView
        return candidateView.root
    }

    fun resetInputViews() {
        entries.forEach { it.inputView = null }
    }

    fun resetCandidateViews() {
        entries.forEach { it.candidateView = null }
    }

    private fun updateInputView() {
        inputView?.removeAllViews()
        val view = currentEntry.inputView ?: currentEntry.imeMode.createInputView(context)
        currentEntry.inputView = view
        (view.parent as ViewGroup?)?.removeView(view)
        inputView?.addView(view)

        val alwaysShowSoftKeyboard = preference.getBoolean("always_show_soft_keyboard", false)
        val hardwareKeyboard = context.resources.configuration.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        view.visibility = if(alwaysShowSoftKeyboard || !hardwareKeyboard) View.VISIBLE else View.GONE
    }

    private fun updateCandidateView() {
        val candidateView = candidateView ?: return
        candidateView.candidateView.removeAllViews()
        val view = currentEntry.candidateView ?: currentEntry.imeMode.createCandidateView(context)
        currentEntry.candidateView = view
        (view.parent as ViewGroup?)?.removeView(view)
        candidateView.candidateView.addView(view)
    }

    fun switchMode(index: Int) {
        tabBarFilter.clear()
        val inputConnection = this.inputConnection ?: return
        val editorInfo = this.editorInfo ?: return
        currentEntry.imeMode.onFinish()
        currentModeIndex = index
        updateInputView()
        updateCandidateView()
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
        tabs.forEach { it.root.isSelected = false }
        tabs[index].root.isSelected = true
    }

    fun setTabsPale(active: Boolean) {
        if(active) {
            tabBarContent?.let(tabBarFilter::apply)
        } else {
            tabBarFilter.clear()
        }
    }

    fun showCandidates() {
        val candidateView = candidateView ?: return
        candidateView.candidateView.visibility = View.VISIBLE
        candidateView.tabViewFrame.visibility = View.GONE
        // Block touch events while view height is being changed
        candidateView.touchBlocker.visibility = View.VISIBLE
        handler.postDelayed({ candidateView.touchBlocker.visibility = View.GONE }, SWITCH_DELAY)
    }

    fun showTabBar() {
        val candidateView = candidateView ?: return
        candidateView.tabViewFrame.visibility = View.VISIBLE
        candidateView.candidateView.visibility = View.GONE
    }

    fun initTabBarView(context: Context): View {
        tabBarFilter.clear()
        val layoutInflater = LayoutInflater.from(context)
        val tabBar = ModeSwitcherTabBarBinding.inflate(layoutInflater, null, false)
        tabBarContent = tabBar.content
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

    interface Callback {
        fun onSwitchInputMode(index: Int)
        fun onSwitchInputMethod(id: String, subtype: InputMethodSubtype)
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
    }
}
