package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SoftKeyCodeMapper {
    operator fun get(
        params: KeyboardParams,
        keyCode: Int
    ): Int

    @SerialName("empty")
    @Serializable
    object Empty: SoftKeyCodeMapper {
        override fun get(
            params: KeyboardParams,
            keyCode: Int
        ): Int = keyCode
    }

    @SerialName("basic")
    @Serializable
    data class Basic(
        val map: Map<Int, Int> = emptyMap()
    ): SoftKeyCodeMapper {
        override fun get(
            params: KeyboardParams,
            keyCode: Int
        ): Int {
            return map[keyCode] ?: keyCode
        }
    }

    @SerialName("by-screen-mode")
    @Serializable
    data class ByScreenMode(
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
                else -> mobile[params, keyCode]
            }
        }
    }
}