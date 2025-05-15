package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.Keyboard

interface KeyboardSet {
    fun initView(context: Context): View
    fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View
}