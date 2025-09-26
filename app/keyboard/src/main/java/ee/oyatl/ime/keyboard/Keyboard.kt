package ee.oyatl.ime.keyboard

import android.content.Context

interface Keyboard {
    fun createView(context: Context, listener: KeyboardListener): KeyboardViewManager

    sealed interface KeyItem {
        val width: Float
        data class Spacer(
            override val width: Float
        ): KeyItem
        data class NormalKey(
            override val keyCode: Int,
            override val width: Float = 1f
        ): Key
        data class SpecialKey(
            override val keyCode: Int,
            override val width: Float = 1f
        ): Key
        interface Key: KeyItem {
            val keyCode: Int
        }
    }
}