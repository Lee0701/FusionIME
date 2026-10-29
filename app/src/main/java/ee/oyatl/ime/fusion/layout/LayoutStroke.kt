package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent

object LayoutStroke {
    // Same key positions as the 12-key kana layout: strokes on top, punctuation below.
    val ROWS_12KEY: List<String> = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    val TABLE_12KEY: Map<Int, List<Int>> = mapOf(
        // 橫 豎 撇 點 折, then a wildcard for a stroke one isn't sure of
        KeyEvent.KEYCODE_1 to '一',
        KeyEvent.KEYCODE_2 to '丨',
        KeyEvent.KEYCODE_3 to '丿',
        KeyEvent.KEYCODE_4 to '丶',
        KeyEvent.KEYCODE_5 to '乙',
        KeyEvent.KEYCODE_6 to '＊',
        KeyEvent.KEYCODE_7 to '，',
        KeyEvent.KEYCODE_8 to '。',
        KeyEvent.KEYCODE_9 to '？',
        KeyEvent.KEYCODE_COMMA to '、',
        KeyEvent.KEYCODE_0 to '！',
        KeyEvent.KEYCODE_PERIOD to '：'
    ).mapValues { (_, c) -> listOf(c.code) }
}
