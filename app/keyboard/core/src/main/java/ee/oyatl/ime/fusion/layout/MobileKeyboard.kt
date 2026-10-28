package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration

import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration.Item

object MobileKeyboard {
    fun bottom(
        left: Int = KeyCode.KEYCODE_COMMA,
        right: Int = KeyCode.KEYCODE_PERIOD
    ): KeyboardConfiguration {
        val row = mutableListOf<Item>()
        row += listOf(
            Item.TemplateKey(KeyCode.KEYCODE_SYM, 1.5f, true),
            Item.TemplateKey(left),
            Item.TemplateKey(KeyCode.KEYCODE_LANGUAGE_SWITCH, 1f, true)
        )
        row += Item.IfEq("cursor_keys", "true", Item.TemplateKey(KeyCode.KEYCODE_SPACE, 2f))
        row += Item.IfNEq("cursor_keys", "true", Item.TemplateKey(KeyCode.KEYCODE_SPACE, 4f))
        row += Item.TemplateKey(right)
        row += Item.IfEq("cursor_keys", "true", Item.Multiple(
            Item.TemplateKey(KeyCode.KEYCODE_DPAD_LEFT, 1f, true),
            Item.TemplateKey(KeyCode.KEYCODE_DPAD_RIGHT, 1f, true)
        ))
        row += Item.TemplateKey(KeyCode.KEYCODE_ENTER, 1.5f, true)
        return KeyboardConfiguration(listOf(row))
    }

    fun numbers(): KeyboardConfiguration {
        return KeyboardConfiguration(listOf(listOf(
            Item.IfEq("number_row", "true", Item.ContentRow(3))
        )))
    }

    fun alphabetic(
        semicolon: Boolean = false,
        shiftDeleteWidth: Float = 1.5f,
        shift: Boolean = true,
        delete: Boolean = true
    ): KeyboardConfiguration {
        val row0 = mutableListOf<Item>(
            Item.ContentRow(2)
        )
        val row1 = mutableListOf<Item>(
            Item.ContentRow(1)
        )
        if(!semicolon) {
            row1.add(0, Item.Spacer(0.5f))
            row1.add(Item.Spacer(0.5f))
        }
        val row2 = mutableListOf<Item>(
            Item.ContentRow(0)
        )
        if(shift) {
            row2.add(0, Item.TemplateKey(KeyCode.KEYCODE_SHIFT_LEFT, shiftDeleteWidth, true))
        }
        if(delete) {
            row2.add(Item.TemplateKey(KeyCode.KEYCODE_DEL, shiftDeleteWidth, true))
        }
        return KeyboardConfiguration(listOf(
            row0,
            row1,
            row2
        ))
    }
}