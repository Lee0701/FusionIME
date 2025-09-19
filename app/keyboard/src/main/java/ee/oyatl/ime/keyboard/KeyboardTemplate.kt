package ee.oyatl.ime.keyboard

interface KeyboardTemplate {
    fun inflate(inflater: KeyboardInflater): Keyboard

    data class Basic(
        val configuration: KeyboardConfiguration,
        val contentRows: List<String>,
        val codeMapper: KeyCodeMapper = KeyCodeMapper()
    ): KeyboardTemplate {
        override fun inflate(inflater: KeyboardInflater): Keyboard {
            return inflater.inflate(configuration, contentRows, codeMapper)
        }
    }

    data class ByScreenMode(
        val mobile: KeyboardTemplate,
        val tablet: KeyboardTemplate = mobile,
        val full: KeyboardTemplate = tablet
    ): KeyboardTemplate {
        override fun inflate(inflater: KeyboardInflater): Keyboard {
            return when(inflater.keyboardParams.screenMode) {
                KeyboardState.ScreenMode.Mobile -> mobile.inflate(inflater)
                KeyboardState.ScreenMode.Tablet -> tablet.inflate(inflater)
                KeyboardState.ScreenMode.Full -> full.inflate(inflater)
            }
        }
    }
}