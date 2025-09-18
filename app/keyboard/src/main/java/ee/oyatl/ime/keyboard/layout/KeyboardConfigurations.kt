package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object KeyboardConfigurations {
    val COMMON_BOTTOM: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_COMMA),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 4f),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_PERIOD),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1.5f, true)
        )
    ))

    val MOBILE: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.ContentRow(2)
        ),
        listOf(
            KeyboardConfiguration.Item.Spacer(0.5f),
            KeyboardConfiguration.Item.ContentRow(1),
            KeyboardConfiguration.Item.Spacer(0.5f)
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, true),
            KeyboardConfiguration.Item.ContentRow(0),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1.5f, true)
        )
    )) + COMMON_BOTTOM

    val MOBILE_EXT1: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.ContentRow(2)
        ),
        listOf(
            KeyboardConfiguration.Item.ContentRow(1),
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, true),
            KeyboardConfiguration.Item.ContentRow(0),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1.5f, true)
        )
    )) + COMMON_BOTTOM

    val MOBILE_EXT2: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.ContentRow(2)
        ),
        listOf(
            KeyboardConfiguration.Item.ContentRow(1),
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1f, true),
            KeyboardConfiguration.Item.ContentRow(0),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true)
        )
    )) + COMMON_BOTTOM

    val MOBILE_DVORAK: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.ContentRow(2)
        ),
        listOf(
            KeyboardConfiguration.Item.ContentRow(1),
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, true),
            KeyboardConfiguration.Item.ContentRow(0),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1.5f, true)
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_X),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 4f),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SLASH),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1.5f, true)
        )
    ))

}