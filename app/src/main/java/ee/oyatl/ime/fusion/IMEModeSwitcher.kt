package ee.oyatl.ime.fusion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBinding
import androidx.core.view.isVisible
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBarBinding

class IMEModeSwitcher(
    private val context: Context,
    val entries: List<Entry>,
    private val callback: Callback
) {
    val size: Int get() = entries.size
    var currentModeIndex: Int = 0
        private set
    private val currentEntry: Entry get() = entries[currentModeIndex]
    val currentMode: IMEMode get() = currentEntry.imeMode

    val inputView: FrameLayout = FrameLayout(context)
    val candidateView: FrameLayout = FrameLayout(context)

    private var inputConnection: InputConnection? = null
    private var editorInfo: EditorInfo? = null

    var isShown: Boolean
        get() = tabBar?.isVisible == true
        set(v) {
            if(v) tabBar?.bringToFront()
            tabBar?.visibility = if(v) View.VISIBLE else View.INVISIBLE
        }

    private var tabBar: ViewGroup? = null
    private var tabs: List<ModeSwitcherTabBinding> = listOf()

    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.inputConnection = inputConnection
        this.editorInfo = editorInfo
        currentEntry.imeMode.onStart(inputConnection, editorInfo)
    }

    fun onFinish() {
        this.inputConnection = null
        this.editorInfo = null
        currentEntry.imeMode.onFinish()
    }

    private fun updateInputView() {
        inputView.removeAllViews()
        val view = currentEntry.inputView ?: currentEntry.imeMode.createInputView(context)
        currentEntry.inputView = view
        inputView.addView(view)
    }

    private fun updateCandidateView() {
        candidateView.removeAllViews()
        val view = currentEntry.candidateView ?: currentEntry.imeMode.createCandidateView(context)
        currentEntry.candidateView = view
        candidateView.addView(view)
    }

    fun switchMode(index: Int) {
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

    fun initTabBarView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)
        val tabBar = ModeSwitcherTabBarBinding.inflate(layoutInflater, null, false)
        tabs = entries.mapIndexed { index, entry ->
            val tab = ModeSwitcherTabBinding.inflate(layoutInflater, tabBar.content, true)
            tab.label.text = entry.label
            tab.root.setOnClickListener {
                callback.onSwitchInputMode(index)
            }
            return@mapIndexed tab
        }
        this.tabBar = tabBar.root
        return tabBar.root
    }

interface Callback {
        fun onSwitchInputMode(index: Int)
    }

    data class Entry(
        val label: String,
        val imeMode: IMEMode
    ) {
        internal var inputView: View? = null
        internal var candidateView: View? = null
    }
}