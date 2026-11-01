package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent

class ParallelKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener {
    constructor(vararg listeners: KeyboardListener): this(listeners.toList())

    override fun onKeyEvent(event: FusionKeyEvent): Boolean {
        return listeners.map { it.onKeyEvent(event) }.all { it }
    }
}