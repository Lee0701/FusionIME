package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

interface KeyboardSet {
    fun initView(context: Context): View
    fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View
}