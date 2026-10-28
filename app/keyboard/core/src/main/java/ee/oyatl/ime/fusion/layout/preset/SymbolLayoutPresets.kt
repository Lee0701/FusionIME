package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.LongPressTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.LayoutSymbol
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.NumberKeyboard
import ee.oyatl.ime.fusion.layout.SymbolMoreKeysTable
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows

object SymbolLayoutPresets {
    fun number(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = NumberKeyboard.mobile(),
                contentRows = emptyList(),
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = NumberKeyboard.tablet(),
                contentRows = emptyList(),
            )
        ),
        layoutTable = LayoutTable(mapOf())
    )

    fun symbolG(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom()
                ),
                contentRows = MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_G
        ),
        longPressTable = LongPressTable.from(
            SymbolMoreKeysTable.DEFAULT
        ),
        softKeyCodeMapper = softKeyCodeMapper()
    )

    fun symbolN1(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom()
                ),
                contentRows = MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_N1
        ),
        longPressTable = LongPressTable.from(
            SymbolMoreKeysTable.DEFAULT
        ),
        softKeyCodeMapper = softKeyCodeMapper()
    )

    fun symbolN2(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom()
                ),
                contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(),
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_N2
        ),
        longPressTable = LongPressTable.from(
            SymbolMoreKeysTable.DEFAULT
        ),
        softKeyCodeMapper = softKeyCodeMapper()
    )

    fun symbolO1(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(),
                    MobileKeyboard.bottom()
                ),
                contentRows = MobileKeyboardRows.DEFAULT,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.DEFAULT
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_O1
        ),
        longPressTable = LongPressTable.from(
            SymbolMoreKeysTable.DEFAULT
        ),
        softKeyCodeMapper = softKeyCodeMapper()
    )

    fun symbolO2(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(),
                    MobileKeyboard.bottom()
                ),
                contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.DEFAULT,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(),
                    TabletKeyboard.alphabetic(),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.DEFAULT
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_O2
        ),
        longPressTable = LongPressTable.from(
            SymbolMoreKeysTable.DEFAULT
        ),
        softKeyCodeMapper = softKeyCodeMapper()
    )

    fun softKeyCodeMapper(): SoftKeyCodeMapper {
        return SoftKeyCodeMapper.Basic(LayoutSymbol.KEYCODE_MAP_SYMBOLS)
    }

    enum class Type(
        val createPreset: () -> KeyboardLayoutPreset
    ) {
        G({ symbolG() }),
        N1({ symbolN1() }),
        N2({ symbolN2() }),
        O1({ symbolO1() }),
        O2({ symbolO2() }),
        ;
    }
}