package ee.oyatl.ime.keyboard

object FlickKeyCode {
    const val FLAG_FLICK = 0x2000000
    const val MASK_KEYCODE = 0x00fffff
    const val MASK_DIRECTION = 0x0f00000
    const val DIRECTION_NONE = 0x0000000
    const val DIRECTION_UP = 0x0100000
    const val DIRECTION_DOWN = 0x0200000
    const val DIRECTION_LEFT = 0x0300000
    const val DIRECTION_RIGHT = 0x0400000
    const val DIRECTION_UP_LEFT = 0x0500000
    const val DIRECTION_UP_RIGHT = 0x0600000
    const val DIRECTION_DOWN_LEFT = 0x0700000
    const val DIRECTION_DOWN_RIGHT = 0x0800000
}