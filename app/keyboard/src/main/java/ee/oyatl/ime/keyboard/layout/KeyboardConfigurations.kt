package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object KeyboardConfigurations {
    val MOBILE: KeyboardConfiguration = KeyboardConfiguration(listOf(
        listOf(
            KeyboardConfiguration.Item.ContentRow(2)
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.Spacer(0.5f)),
            KeyboardConfiguration.Item.ContentRow(1),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.Spacer(0.5f))
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f)),
            KeyboardConfiguration.Item.ContentRow(0),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_DEL, 1.5f))
        ),
        listOf(
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_SYM, 1.5f)),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.NormalKey(KeyEvent.KEYCODE_COMMA)),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH)),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_SPACE, 4f)),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.NormalKey(KeyEvent.KEYCODE_PERIOD)),
            KeyboardConfiguration.Item.TemplateKey(Keyboard.KeyItem.SpecialKey(KeyEvent.KEYCODE_ENTER, 1.5f))
        ),
    ))
}