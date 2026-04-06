package ee.oyatl.ime.keyboard

object FlickKeyCode {
    const val FLAG = 0x2000
    const val FLAG_UP = FLAG or 0x0100
    const val FLAG_DOWN = FLAG or 0x0200
    const val FLAG_LEFT = FLAG or 0x0300
    const val FLAG_RIGHT = FLAG or 0x0400
    const val FLAG_UP_LEFT = FLAG or 0x0500
    const val FLAG_UP_RIGHT = FLAG or 0x0600
    const val FLAG_DOWN_LEFT = FLAG or 0x0700
    const val FLAG_DOWN_RIGHT = FLAG or 0x0800
}