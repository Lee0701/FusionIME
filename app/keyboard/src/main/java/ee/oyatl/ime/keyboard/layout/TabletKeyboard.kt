package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object TabletKeyboard {
    fun bottom(): KeyboardConfiguration {
        return KeyboardConfiguration(listOf(
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 5f),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_LEFT),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_RIGHT),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true)
            )
        ))
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
        shift: Boolean = true,
        delete: Boolean = true
    ): KeyboardConfiguration {
        val row0: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.ContentRow(2)
        )
        if(delete) {
            row0.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        } else {
            row0.add(KeyboardConfiguration.Item.Spacer(1f))
        }
        val row1: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.Spacer(0.5f),
            KeyboardConfiguration.Item.ContentRow(1),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 2f, true)
        )
        val row2: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.ContentRow(0)
        )
        if(shift) {
            row2.add(0, KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1f, true))
            row2.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1f, true))
        }
        return KeyboardConfiguration(listOf(
            row0,
            row1,
            row2
        ))
    }
}