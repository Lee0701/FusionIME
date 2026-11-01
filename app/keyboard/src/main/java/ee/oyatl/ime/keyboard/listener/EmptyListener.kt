package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent

object EmptyListener: KeyboardListener {
    override fun onKeyEvent(event: FusionKeyEvent): Boolean = false
}