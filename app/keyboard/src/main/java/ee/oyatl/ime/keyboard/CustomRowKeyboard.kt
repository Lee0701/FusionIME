package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener

class CustomRowKeyboard(
    private val configuration: List<KeyType>
): DefaultKeyboard() {
    override val numRows: Int = 1

    override fun buildRows(context: Context, listener: KeyboardListener): List<KbdRowBinding> {
        val row = buildRow(context, listener, listOf())

        configuration.forEach { keyType ->
            when(keyType) {
                is KeyType.Symbols -> {
                    row.root.addView(buildSpecialKey(
                        context,
                        listener,
                        Keyboard.SpecialKey.Symbols,
                        R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                        R.drawable.keyic_option,
                        keyType.width
                    ))
                }
                is KeyType.Shift -> {
                    val shiftKey = KbdKeyBinding.bind(buildSpecialKey(
                        context,
                        listener,
                        Keyboard.SpecialKey.Shift,
                        R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                        R.drawable.keyic_shift,
                        keyType.width
                    ))
                    shiftKeys += shiftKey
                    row.root.addView(shiftKey.root)
                }
                is KeyType.Language -> {
                    row.root.addView(buildSpecialKey(
                        context,
                        listener,
                        Keyboard.SpecialKey.Language,
                        R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                        R.drawable.keyic_language,
                        keyType.width
                    ))
                }
                is KeyType.Space -> {
                    row.root.addView(buildSpecialKey(
                        context,
                        listener,
                        Keyboard.SpecialKey.Space,
                        R.style.Theme_FusionIME_Keyboard_Key,
                        R.drawable.keyic_space,
                        keyType.width
                    ))
                }
                is KeyType.Return -> {
                    row.root.addView(buildSpecialKey(
                        context,
                        listener,
                        Keyboard.SpecialKey.Return,
                        R.style.Theme_FusionIME_Keyboard_Key_Return,
                        R.drawable.keyic_return,
                        keyType.width
                    ))
                }
                is KeyType.Delete -> {
                    row.root.addView(buildSpecialKey(
                        context,
                        RepeatableKeyListener(listener),
                        Keyboard.SpecialKey.Delete,
                        R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                        R.drawable.keyic_backspace,
                        keyType.width
                    ))
                }
                is KeyType.Extra -> {
                    val key = buildKey(context, listener, keyType.code, keyType.code.toChar().toString())
                    row.root.addView(key.root)
                }
            }
        }

        return listOf(row)
    }

    interface KeyType {
        val width: Float
        data class Space(override val width: Float = 1f): KeyType
        data class Return(override val width: Float = 1f): KeyType
        data class Delete(override val width: Float = 1f): KeyType
        data class Shift(override val width: Float = 1f): KeyType
        data class Symbols(override val width: Float = 1f): KeyType
        data class Language(override val width: Float = 1f): KeyType
        data class Extra(val code: Int): KeyType {
            override val width: Float = 1f
        }
    }
}