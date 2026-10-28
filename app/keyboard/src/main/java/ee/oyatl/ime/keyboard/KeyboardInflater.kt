package ee.oyatl.ime.keyboard

import android.view.KeyCharacterMap
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.Keyboard
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardParams
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.SpecialKeyType
import ee.oyatl.ime.theme.KeyBackground
import ee.oyatl.ime.theme.SpecialKeyIcon
import kotlin.collections.map
import kotlin.text.get

object KeyboardInflater {
    private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    private fun keyCharToKeyCode(keyChar: Char): Int {
        return keyCharacterMap.getEvents(charArrayOf(keyChar)).firstOrNull()?.keyCode ?: 0
    }

    fun inflate(
        preset: KeyboardLayoutPreset,
        params: KeyboardParams
    ): Keyboard {
        return inflate(preset.keyboardTemplate, preset.softKeyCodeMapper, params)
    }

    private fun inflate(
        template: KeyboardTemplate,
        softKeyCodeMapper: SoftKeyCodeMapper,
        params: KeyboardParams
    ): Keyboard {
        return when(template) {
            is KeyboardTemplate.Basic -> inflateBasic(template, softKeyCodeMapper, params)
            is KeyboardTemplate.ByScreenMode -> inflateByScreenMode(template, softKeyCodeMapper, params)
        }
    }

    private fun inflateByScreenMode(
        template: KeyboardTemplate.ByScreenMode,
        softKeyCodeMapper: SoftKeyCodeMapper,
        params: KeyboardParams
    ): Keyboard {
        return when(params.screenMode) {
            KeyboardState.ScreenMode.Mobile -> inflate(template.mobile, softKeyCodeMapper, params)
            KeyboardState.ScreenMode.Tablet -> inflate(template.tablet, softKeyCodeMapper, params)
            KeyboardState.ScreenMode.Full -> inflate(template.full, softKeyCodeMapper, params)
            else -> inflate(template.mobile, softKeyCodeMapper, params)
        }
    }

    private fun inflateBasic(
        template: KeyboardTemplate.Basic,
        softKeyCodeMapper: SoftKeyCodeMapper,
        params: KeyboardParams
    ): Keyboard {
        val keyCodeRows = template.contentRows.map { row -> row.map { keyCharToKeyCode(it) } }
        val result = mutableListOf<List<Keyboard.KeyItem>>()

        fun inflateItem(item: KeyboardConfiguration.Item): List<Keyboard.KeyItem> = when(item) {
            is KeyboardConfiguration.Item.ContentKey -> {
                val rowIndex = keyCodeRows.size - item.rowId - 1
                val keyCode = softKeyCodeMapper[params, keyCodeRows[rowIndex][item.index]]
                listOf(inflateKey(keyCode))
            }
            is KeyboardConfiguration.Item.ContentRow -> {
                val rowIndex = keyCodeRows.size - item.rowId - 1
                val content = keyCodeRows[rowIndex].map {
                    if(it == KeyCode.KEYCODE_SPACE) {
                        Keyboard.KeyItem.SplitSpacer(params.splitWidth)
                    } else {
                        val keyCode = softKeyCodeMapper[params, it]
                        inflateKey(keyCode)
                    }
                }
                content
            }
            is KeyboardConfiguration.Item.Spacer -> {
                listOf(Keyboard.KeyItem.Spacer(item.width))
            }
            is KeyboardConfiguration.Item.TemplateKey -> {
                val keyCode = softKeyCodeMapper[params, item.keyCode]
                if(params.splitWidth != 0 && keyCode == KeyCode.KEYCODE_SPACE) {
                    listOf(
                        inflateKey(keyCode, item.copy(width = item.width / 2)),
                        Keyboard.KeyItem.SplitSpacer(params.splitWidth),
                        inflateKey(keyCode, item.copy(width = item.width / 2))
                    )
                } else {
                    listOf(inflateKey(keyCode, item))
                }
            }
            is KeyboardConfiguration.Item.IfEq -> {
                val value = params.extra[item.name].toString()
                if(value == item.value) inflateItem(item.content) else listOf()
            }
            is KeyboardConfiguration.Item.IfNEq -> {
                val value = params.extra[item.name].toString()
                if(value != item.value) inflateItem(item.content) else listOf()
            }
            is KeyboardConfiguration.Item.Multiple -> {
                return item.items.flatMap(::inflateItem)
            }
        }

        template.configuration.rows.forEach { row ->
            val resultRow = mutableListOf<Keyboard.KeyItem>()
            row.forEach { item ->
                resultRow += inflateItem(item)
            }
            result += resultRow
        }
        result.removeIf { it.isEmpty() }
        return Keyboard(result, params)
    }

    private fun inflateKey(keyCode: Int): Keyboard.KeyItem {
        return Keyboard.KeyItem.Key(
            keyCode,
            bkgRes = KeyBackground.forMerge(null)
        )
    }

    private fun inflateKey(keyCode: Int, item: KeyboardConfiguration.Item.TemplateKey): Keyboard.KeyItem {
        val bkgRes = KeyBackground.forMerge(item.merge)
        return if(item.special) {
            val type = SpecialKeyType.ofKeyCode(keyCode) ?: SpecialKeyType.Default
            val iconRes = SpecialKeyIcon.iconFor(type) ?: 0
            Keyboard.KeyItem.Key(
                keyCode,
                width = item.width,
                label = "",
                iconRes = iconRes,
                keyType = type.keyType,
                bkgRes = bkgRes
            )
        } else {
            Keyboard.KeyItem.Key(
                keyCode,
                width = item.width,
                bkgRes = bkgRes
            )
        }
    }
}