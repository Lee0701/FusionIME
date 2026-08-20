package ee.oyatl.ime.keyboard

import android.view.KeyCharacterMap

interface SoftKeyCodeMapper {
    operator fun get(
        params: KeyboardParams,
        keyCode: Int
    ): Int

    object Empty: SoftKeyCodeMapper {
        override fun get(
            params: KeyboardParams,
            keyCode: Int
        ): Int = keyCode
    }

    class Basic(
        val map: Map<Int, Int> = emptyMap()
    ): SoftKeyCodeMapper {
        override fun get(
            params: KeyboardParams,
            keyCode: Int
        ): Int {
            return map[keyCode] ?: keyCode
        }
    }

    class ByScreenMode(
        val mobile: SoftKeyCodeMapper = Basic(),
        val tablet: SoftKeyCodeMapper = mobile,
        val full: SoftKeyCodeMapper = tablet
    ): SoftKeyCodeMapper {
        override fun get(
            params: KeyboardParams,
            keyCode: Int
        ): Int {
            return when(params.screenMode) {
                KeyboardState.ScreenMode.Mobile -> mobile[params, keyCode]
                KeyboardState.ScreenMode.Tablet -> tablet[params, keyCode]
                KeyboardState.ScreenMode.Full -> full[params, keyCode]
            }
        }
    }

    companion object {
        private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

        fun keyCharToKeyCode(keyChar: Char): Int {
            return keyCharacterMap.getEvents(charArrayOf(keyChar)).firstOrNull()?.keyCode ?: 0
        }
    }
}