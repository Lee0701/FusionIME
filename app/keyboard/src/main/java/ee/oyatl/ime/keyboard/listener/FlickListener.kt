package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FlickDirection

interface FlickListener {
    fun onFlick(keyCode: Int, direction: FlickDirection): Boolean
    fun onReset()
}