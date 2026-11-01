package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent

class InputOnKeyUp(
    val listener: KeyboardListener,
    val ignoreKeyCodes: Set<Int> = emptySet()
): KeyboardListener {
    override fun onKeyEvent(event: FusionKeyEvent): Boolean = when(event) {
        is FusionKeyEvent.Press -> {
            if(event.keyCode in ignoreKeyCodes) return false
            return true
        }
        is FusionKeyEvent.Release -> {
            if(event.keyCode in ignoreKeyCodes) return false
            listener.onKeyEvent(FusionKeyEvent.Press(event.keyCode, event.shiftState, event.altGrState, event.source))
            listener.onKeyEvent(FusionKeyEvent.Release(event.keyCode, event.shiftState, event.altGrState, event.source))
            return true
        }
        else -> listener.onKeyEvent(event)
    }
}