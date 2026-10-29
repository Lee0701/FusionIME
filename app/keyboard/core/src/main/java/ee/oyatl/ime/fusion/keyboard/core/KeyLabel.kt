package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class KeyLabel(
    val text: String? = null,
    val icon: Int? = null,
    val up: String? = null,
    val down: String? = null,
    val left: String? = null,
    val right: String? = null,
    val showFlickHint: Boolean = true
) {
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