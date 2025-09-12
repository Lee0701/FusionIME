package ee.oyatl.ime.keyboard.switcher

import android.view.View

interface KeyboardSwitcher<T> {
    val view: View
    fun switch(state: T)
}