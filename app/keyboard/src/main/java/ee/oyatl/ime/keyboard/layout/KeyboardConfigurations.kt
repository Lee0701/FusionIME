package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object KeyboardConfigurations {
    fun mobileBottom(
        left: Int = KeyEvent.KEYCODE_COMMA,
        right: Int = KeyEvent.KEYCODE_PERIOD
    ): KeyboardConfiguration {
        return KeyboardConfiguration(listOf(
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
                KeyboardConfiguration.Item.TemplateKey(left),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 4f),
                KeyboardConfiguration.Item.TemplateKey(right),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1.5f, true)
            )
        ))
    }

    fun mobileNumbers(): KeyboardConfiguration {
        return KeyboardConfiguration(listOf(
            listOf(
                KeyboardConfiguration.Item.ContentRow(3)
            )
        ))
    }

    fun mobileAlpha(
        semicolon: Boolean = false,
        shiftDeleteWidth: Float = 1.5f,
        shift: Boolean = true,
        delete: Boolean = true
    ): KeyboardConfiguration {
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
            row2.add(0, KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SHIFT_LEFT, shiftDeleteWidth, true))
        }
        if(delete) {
            row2.add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, shiftDeleteWidth, true))
        }
        return KeyboardConfiguration(listOf(
            listOf(
                KeyboardConfiguration.Item.ContentRow(2)
            ),
            row1,
            row2
        ))
    }
}