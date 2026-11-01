package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent

interface KeyboardListener {
    fun onKeyEvent(event: FusionKeyEvent): Boolean
}