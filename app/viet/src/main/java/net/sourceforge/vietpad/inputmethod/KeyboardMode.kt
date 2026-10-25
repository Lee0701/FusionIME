package net.sourceforge.vietpad.inputmethod

enum class KeyboardMode {
    Qwerty, Telex, VIQR, VNI;

    fun createInputMethod(): InputMethod {
        return when(this) {
            Qwerty -> QwertyIM()
            Telex -> TelexIM()
            VIQR -> ViqrIM()
            VNI -> VniIM()
        }
    }
}