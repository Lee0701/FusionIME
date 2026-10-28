package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration

import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration.Item

object TabletKeyboard {
    fun bottom(): KeyboardConfiguration {
        return KeyboardConfiguration(
            listOf(
                listOf(
                    Item.TemplateKey(KeyCode.KEYCODE_SYM, 1.5f, true),
                    Item.TemplateKey(KeyCode.KEYCODE_LANGUAGE_SWITCH, 1f, true),
                    Item.TemplateKey(KeyCode.KEYCODE_SPACE, 5f),
                    Item.TemplateKey(KeyCode.KEYCODE_DPAD_LEFT, 1f, true),
                    Item.TemplateKey(KeyCode.KEYCODE_DPAD_RIGHT, 1f, true),
                    Item.TemplateKey(KeyCode.KEYCODE_SYM, 1.5f, true)
                )
            )
        )
    }

    fun numbers(
        fillDeleteSpace: Boolean = true
    ): KeyboardConfiguration {
        val row0: MutableList<Item> = mutableListOf(
            Item.ContentRow(3)
        )
        row0.add(Item.IfEq("number_row", "true", Item.TemplateKey(KeyCode.KEYCODE_DEL, 1f, true)))
        if(fillDeleteSpace) row0.add(Item.IfNEq("number_row", "true", Item.Spacer(1f)))
        return KeyboardConfiguration(listOf(
            row0
        ))
    }

    fun alphabetic(
        semicolon: Boolean = false,
        rightShift: Boolean = true,
        fillDeleteSpace: Boolean = true
    ): KeyboardConfiguration {
        val row0: MutableList<Item> = mutableListOf(
            Item.ContentRow(2)
        )
        row0.add(Item.IfNEq("number_row", "true", Item.TemplateKey(KeyCode.KEYCODE_DEL, 1f, true)))
        if(fillDeleteSpace) row0.add(Item.IfEq("number_row", "true", Item.Spacer(1f)))
        val spacerWidth = if(semicolon) 0.25f else 0.5f
        val returnWidth = if(semicolon) 0.75f else 1.5f
        val row1: MutableList<Item> = mutableListOf(
            Item.Spacer(spacerWidth),
            Item.ContentRow(1),
            Item.TemplateKey(KeyCode.KEYCODE_ENTER, returnWidth, true)
        )
        val row2: MutableList<Item> = mutableListOf(
            Item.TemplateKey(KeyCode.KEYCODE_SHIFT_LEFT, 1f, true),
            Item.ContentRow(0)
        )
        if(rightShift) {
            row2.add(Item.TemplateKey(KeyCode.KEYCODE_SHIFT_LEFT, 1f, true))
        }
        return KeyboardConfiguration(listOf(
            row0,
            row1,
            row2
        ))
    }
}