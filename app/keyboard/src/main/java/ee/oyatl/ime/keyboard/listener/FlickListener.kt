package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

interface FlickListener {
    fun onFlick(keyCode: Int, direction: FlickDirection)
}