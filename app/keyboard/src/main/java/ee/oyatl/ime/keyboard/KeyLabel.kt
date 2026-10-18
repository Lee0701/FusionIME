package ee.oyatl.ime.keyboard

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

interface KeyLabel {
    data class Default(
        val text: String? = null,
        val icon: Int? = null
    ): KeyLabel

    data class Flick(
        val text: String? = null,
        val up: String? = null,
        val down: String? = null,
        val left: String? = null,
        val right: String? = null,
        val showAsHint: Boolean = true
    ): KeyLabel {
        fun forDirection(direction: FlickDirection): String? {
            return when(direction) {
                FlickDirection.Up -> up
                FlickDirection.Down -> down
                FlickDirection.Left -> left
                FlickDirection.Right -> right
                else -> null
            }
        }
    }
}