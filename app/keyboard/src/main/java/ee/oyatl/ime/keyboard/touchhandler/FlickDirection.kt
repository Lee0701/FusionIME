package ee.oyatl.ime.keyboard.touchhandler

import kotlin.math.PI

enum class FlickDirection(
    val angle: Double,
    val diagonal: Boolean
) {
    Up(0.5 * PI, false),
    Down(1.5 * PI, false),
    Left(0.0 * PI, false),
    Right(1.0 * PI, false),
    UpLeft(0.25 * PI, true),
    UpRight(0.75 * PI, true),
    DownLeft(1.75 * PI, true),
    DownRight(1.25 * PI, true);

    fun contains(angle: Double, range: Double): Boolean {
        val start = this.angle - range / 2
        val end = this.angle + range / 2
        val range = start .. end
        val range1 = start + 2 * PI .. 2 * PI
        val range2 = 0.0 .. end - 2 * PI
        return angle in range || (start < -0.0 && angle in range1) || (end > 2 * PI && angle in range2)
    }
}