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
    val entries: List<Entry>,
    private val callback: Callback
) {
    val size: Int get() = entries.size
    var currentModeIndex: Int = 0
        private set
    val currentMode: IMEMode get() = entries[currentModeIndex].imeMode

    private lateinit var inputView: FrameLayout
    private lateinit var candidateView: FrameLayout

    private var inputConnection: InputConnection? = null
    private var editorInfo: EditorInfo? = null

    var isShown: Boolean
        get() = tabBar?.isVisible == true
        set(v) {
            if(v) tabBar?.bringToFront()
        }

    private var tabBar: ViewGroup? = null
    private var tabs: List<ModeSwitcherTabBinding> = listOf()

    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.inputConnection = inputConnection
        this.editorInfo = editorInfo
        currentMode.onStart(inputConnection, editorInfo)
    }

    fun onFinish() {
        this.inputConnection = null
        this.editorInfo = null
        currentMode.onFinish()
    }

    fun createInputView(context: Context): View {
        inputView = FrameLayout(context)
        entries.forEach { inputView.addView(it.imeMode.createInputView(context)) }
        return inputView
    }

    fun createCandidateView(context: Context): View {
        candidateView = FrameLayout(context)
        entries.forEach {
            val view = it.imeMode.createCandidateView(context)
            view.visibility = View.GONE
            candidateView.addView(view)
        }
        return candidateView
    }

    fun switchMode(index: Int) {
        val inputConnection = this.inputConnection ?: return
        val editorInfo = this.editorInfo ?: return
        currentMode.onFinish()
        currentModeIndex = index
        currentMode.onStart(inputConnection, editorInfo)
        currentMode.getInputView().bringToFront()
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
    )
}