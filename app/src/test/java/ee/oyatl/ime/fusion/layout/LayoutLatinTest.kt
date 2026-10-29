package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutLatinTest {
    @Test
    fun spanishQwertyAddsEnyeAfterL() {
        assertEquals(
            listOf('ñ'.code, 'Ñ'.code),
            LayoutLatin.TABLE_SPANISH_QWERTY[KeyEvent.KEYCODE_SEMICOLON]
        )
    }

    @Test
    fun azertyRemapsLettersAndKeepsApostropheOnBottomRow() {
        val table = LayoutTable.fromShiftStates(
            LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutLatin.TABLE_AZERTY
        )
        val mapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_AZERTY
                + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_NUMBERS
                + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_MOBILE
        )

        assertKey(table, mapper, KeyEvent.KEYCODE_Q, 'a', 'A')
        assertKey(table, mapper, KeyEvent.KEYCODE_W, 'z', 'Z')
        assertKey(table, mapper, KeyEvent.KEYCODE_A, 'q', 'Q')
        assertKey(table, mapper, KeyEvent.KEYCODE_SEMICOLON, 'm', 'M')
        assertKey(table, mapper, KeyEvent.KEYCODE_Z, 'w', 'W')
        assertKey(table, mapper, KeyEvent.KEYCODE_M, '\'', '?')
    }

    @Test
    fun frenchDefaultsToAzertyUnlessLayoutIsExplicit() {
        assertEquals(
            LatinIMEMode.Layout.Azerty,
            LatinIMEMode.Params.parse(mapOf("type" to "latin", "locale" to "fr")).layout
        )
        assertEquals(
            LatinIMEMode.Layout.Qwerty,
            LatinIMEMode.Params.parse(
                mapOf("type" to "latin", "locale" to "fr", "layout" to "Qwerty")
            ).layout
        )
    }

    private fun assertKey(
        table: LayoutTable,
        mapper: SoftKeyCodeMapper.Basic,
        keyCode: Int,
        normal: Char,
        shifted: Char
    ) {
        val mappedKeyCode = mapper.map[keyCode] ?: keyCode
        val item = table[mappedKeyCode] as LayoutTable.DefaultItem
        assertEquals(normal.code, item.normal)
        assertEquals(shifted.code, item.shifted)
    }
}
