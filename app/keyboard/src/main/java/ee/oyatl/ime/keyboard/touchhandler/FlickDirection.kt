package ee.oyatl.ime.keyboard.touchhandler

import ee.oyatl.ime.keyboard.FlickKeyCode
import kotlin.math.PI

enum class FlickDirection(
    val angle: Double,
    val diagonal: Boolean,
    val keyCodeFlag: Int
) {
    Up(0.5 * PI, false, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_UP),
    Down(1.5 * PI, false, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_DOWN),
    Left(0.0 * PI, false, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_LEFT),
    Right(1.0 * PI, false, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_RIGHT),
    UpLeft(0.25 * PI, true, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_UP_LEFT),
    UpRight(0.75 * PI, true, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_UP_RIGHT),
    DownLeft(1.75 * PI, true, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_DOWN_LEFT),
    DownRight(1.25 * PI, true, FlickKeyCode.FLAG_FLICK or FlickKeyCode.DIRECTION_DOWN_RIGHT);

    fun contains(angle: Double, range: Double): Boolean {
        val start = this.angle - range / 2
        val end = this.angle + range / 2
        val range = start .. end
        val range1 = start + 2 * PI .. 2 * PI
        val range2 = 0.0 .. end - 2 * PI
        return angle in range || (start < -0.0 && angle in range1) || (end > 2 * PI && angle in range2)
    }
}