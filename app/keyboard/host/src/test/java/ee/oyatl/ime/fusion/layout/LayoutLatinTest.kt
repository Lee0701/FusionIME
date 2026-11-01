package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent
import ee.oyatl.ime.fusion.keyboard.core.table.KeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.KeyEventSource
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState
import ee.oyatl.ime.fusion.keyboard.core.table.LayoutTable
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutLatinTest {
    @Test
    fun spanishQwertyAddsEnyeAfterL() {
        assertEquals(
            listOf('ñ'.code, 'Ñ'.code),
            LayoutLatin.TABLE_SPANISH_QWERTY[KeyCode.KEYCODE_SEMICOLON]
        )
    }

    @Test
    fun azertyRemapsLettersAndKeepsApostropheOnBottomRow() {
        val table = LayoutTable.fromShiftStates(
            LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + LayoutLatin.TABLE_AZERTY
        )
        val mapper = KeyCodeMapper(LayoutLatin.KEYCODE_MAP_AZERTY
                + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_NUMBERS
                + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_MOBILE
        )

        assertKey(table, mapper, KeyCode.KEYCODE_Q, 'a', 'A')
        assertKey(table, mapper, KeyCode.KEYCODE_W, 'z', 'Z')
        assertKey(table, mapper, KeyCode.KEYCODE_A, 'q', 'Q')
        assertKey(table, mapper, KeyCode.KEYCODE_SEMICOLON, 'm', 'M')
        assertKey(table, mapper, KeyCode.KEYCODE_Z, 'w', 'W')
        assertKey(table, mapper, KeyCode.KEYCODE_M, '\'', '?')
    }

    private fun assertKey(
        table: LayoutTable,
        mapper: KeyCodeMapper,
        keyCode: Int,
        expectedNormal: Char,
        expectedShifted: Char
    ) {
        val mappedKeyCode = mapper[keyCode]
        val actualNormal = table[FusionKeyEvent.Press(mappedKeyCode, KeyboardState.Shift.Released, false, KeyEventSource.Unknown)]
        val actualShifted = table[FusionKeyEvent.Press(mappedKeyCode, KeyboardState.Shift.Pressed, false, KeyEventSource.Unknown)]
        assertEquals(expectedNormal.code, actualNormal)
        assertEquals(expectedShifted.code, actualShifted)
    }
}