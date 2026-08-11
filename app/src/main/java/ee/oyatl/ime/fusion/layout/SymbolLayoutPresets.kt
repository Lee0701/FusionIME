package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.fusion.KeyboardLayoutPreset
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable

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
                    MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_G
        )
    )

    fun symbolN1(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_NA
        )
    )

    fun symbolN2(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(delete = true),
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_NA
        )
    )

    fun symbolO1(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(),
                    MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = MobileKeyboardRows.DEFAULT,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(),
                    TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = TabletKeyboardRows.DEFAULT
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_OA
        )
    )

    fun symbolO2(): KeyboardLayoutPreset = KeyboardLayoutPreset(
        keyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(),
                    MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.DEFAULT,
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(delete = true),
                    TabletKeyboard.alphabetic(),
                    TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
                ),
                contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.DEFAULT
            )
        ),
        layoutTable = LayoutTable.fromShiftStates(
            map = LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_OB
        )
    )

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