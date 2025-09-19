package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object NumberKeyboard {
    val ROWS_COMMON: List<String> = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    val ROWS_LEFT: List<String> = listOf(
        "+-",
        "*/",
        "()",
        ""
    )

    val ROWS_RIGHT: List<String> = listOf(
        "=",
        "#",
        "$",
        "%",
    )

    fun mobile(): KeyboardConfiguration {
        val rows = ROWS_COMMON.zip(ROWS_LEFT).map { (center, left) ->
            (left.map { KeyboardConfiguration.Item.TemplateKey(-it.code, 0.5f) } +
                    center.map { KeyboardConfiguration.Item.TemplateKey(-it.code) })
                .toMutableList()
        }
        rows[0].add(KeyboardConfiguration.Item.TemplateKey(-'='.code))
        rows[1].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 1f, true))
        rows[2].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        rows[3].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1f, true))
        rows[3].add(0, KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1f, true))
        return KeyboardConfiguration(rows)
    }

    fun tablet(): KeyboardConfiguration {
        val rows = ROWS_COMMON.indices.map { i ->
            val center = ROWS_COMMON[i]
            val left = ROWS_LEFT[i]
            val right = ROWS_RIGHT[i]
            (left.map { KeyboardConfiguration.Item.TemplateKey(-it.code, 0.5f) } +
                    center.map { KeyboardConfiguration.Item.TemplateKey(-it.code) } +
                    right.map { KeyboardConfiguration.Item.TemplateKey(-it.code, 0.5f) })
                .toMutableList()
        }
        rows[0].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 0.5f, true))
        rows[1].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 0.5f, true))
        rows[2].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 0.5f, true))
        rows[3].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 0.5f, true))
        rows[3].add(0, KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1f, true))
        return KeyboardConfiguration(rows)
    }
}