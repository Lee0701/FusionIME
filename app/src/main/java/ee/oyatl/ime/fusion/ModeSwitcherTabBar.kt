package ee.oyatl.ime.fusion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ee.oyatl.ime.fusion.databinding.ModeSwitcherTabBinding
import androidx.core.view.isVisible

class ModeSwitcherTabBar(
    private val labels: List<String>,
    private val callback: Callback
) {
    var isShown: Boolean
        get() = tabBar.isVisible
        set(v) {
            tabBar.visibility = if (v) View.VISIBLE else View.GONE
        }

    private lateinit var tabBar: ViewGroup
    private lateinit var tabs: List<ModeSwitcherTabBinding>

    fun initView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)
        tabBar = layoutInflater.inflate(R.layout.mode_switcher_tab_bar, null) as ViewGroup
        tabs = labels.mapIndexed { index, label ->
            val tab = ModeSwitcherTabBinding.inflate(layoutInflater, tabBar, true)
            tab.label.text = label
            tab.root.setOnClickListener {
                callback.onSwitchInputMode(index)
            }
            return@mapIndexed tab
        }
        return tabBar
    }

    fun activate(index: Int) {
        tabs.forEach { it.root.isSelected = false }
        tabs[index].root.isSelected = true
    }

    interface Callback {
        fun onSwitchInputMode(index: Int)
    }
}