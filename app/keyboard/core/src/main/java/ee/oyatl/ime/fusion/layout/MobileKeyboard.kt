package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration

object MobileKeyboard {
    fun bottom(
        left: Int = KeyCode.KEYCODE_COMMA,
        right: Int = KeyCode.KEYCODE_PERIOD,
        dpad: Boolean = false,
        languageKeyCode: Int = KeyCode.KEYCODE_LANGUAGE_SWITCH
    ): KeyboardConfiguration {
        val row = mutableListOf<KeyboardConfiguration.Item>()
        row += listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(left),
            KeyboardConfiguration.Item.TemplateKey(languageKeyCode, 1f, true)
        )
        val spaceWidth = if(dpad) 2f else 4f
        row += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SPACE, spaceWidth)
        row += KeyboardConfiguration.Item.TemplateKey(right)
        if(dpad) {
            row += listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DPAD_LEFT, 1f, true),
                KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DPAD_RIGHT, 1f, true)
            )
        }
        row += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_ENTER, 1.5f, true)
        return KeyboardConfiguration(listOf(row))
    }

    fun numbers(): KeyboardConfiguration {
        return KeyboardConfiguration(listOf(
            listOf(
                KeyboardConfiguration.Item.ContentRow(3)
            )
        ))
    }

    fun alphabetic(
        semicolon: Boolean = false,
        shiftDeleteWidth: Float = 1.5f,
        shift: Boolean = true,
        delete: Boolean = true
    ): KeyboardConfiguration {
        val row0 = mutableListOf<KeyboardConfiguration.Item>(
            KeyboardConfiguration.Item.ContentRow(2)
        )
        val row1 = mutableListOf<KeyboardConfiguration.Item>(
            KeyboardConfiguration.Item.ContentRow(1)
        )
        if(!semicolon) {
            row1.add(0, KeyboardConfiguration.Item.Spacer(0.5f))
            row1.add(KeyboardConfiguration.Item.Spacer(0.5f))
        }
        val row2 = mutableListOf<KeyboardConfiguration.Item>(
            KeyboardConfiguration.Item.ContentRow(0)
        )
        if(shift) {
            row2.add(0, KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SHIFT_LEFT, shiftDeleteWidth, true))
        }
        if(delete) {
            row2.add(KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DEL, shiftDeleteWidth, true))
        }
        return KeyboardConfiguration(listOf(
            row0,
            row1,
            row2
        ))
    }
}