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

    fun numbers(
        delete: Boolean = false,
        spacerOnDelete: Boolean = true
    ): KeyboardConfiguration {
        val row0: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.ContentRow(3)
        )
        if(delete) {
            row0.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        } else if(spacerOnDelete) {
            row0.add(KeyboardConfiguration.Item.Spacer(1f))
        }
        return KeyboardConfiguration(listOf(
            row0
        ))
    }

    fun alphabetic(
        semicolon: Boolean = false,
        rightShift: Boolean = true,
        delete: Boolean = true,
        spacerOnDelete: Boolean = true
    ): KeyboardConfiguration {
        val row0: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.ContentRow(2)
        )
        if(delete) {
            row0.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        } else if(spacerOnDelete) {
            row0.add(KeyboardConfiguration.Item.Spacer(1f))
        }
        val spacerWidth = if(semicolon) 0.25f else 0.5f
        val returnWidth = if(semicolon) 0.75f else 1.5f
        val row1: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.Spacer(spacerWidth),
            KeyboardConfiguration.Item.ContentRow(1),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, returnWidth, true)
        )
        val row2: MutableList<KeyboardConfiguration.Item> = mutableListOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1f, true),
            KeyboardConfiguration.Item.ContentRow(0)
        )
        if(rightShift) {
            row2.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1f, true))
        }
        return KeyboardConfiguration(listOf(
            row0,
            row1,
            row2
        ))
    }
}