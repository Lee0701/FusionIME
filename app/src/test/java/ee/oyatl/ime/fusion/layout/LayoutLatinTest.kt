package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.keyboard.LayoutTable
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
            LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + mapOf(
                KeyEvent.KEYCODE_APOSTROPHE to listOf('\''.code, '?'.code)
            )
        ).mapKeyCodes(LayoutLatin.KEYCODE_MAP_AZERTY)

        assertKey(table, KeyEvent.KEYCODE_Q, 'a', 'A')
        assertKey(table, KeyEvent.KEYCODE_W, 'z', 'Z')
        assertKey(table, KeyEvent.KEYCODE_A, 'q', 'Q')
        assertKey(table, KeyEvent.KEYCODE_SEMICOLON, 'm', 'M')
        assertKey(table, KeyEvent.KEYCODE_Z, 'w', 'W')
        assertKey(table, KeyEvent.KEYCODE_M, '\'', '?')
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
        keyCode: Int,
        normal: Char,
        shifted: Char
    ) {
        val item = table[keyCode] as LayoutTable.DefaultItem
        assertEquals(normal.code, item.normal)
        assertEquals(shifted.code, item.shifted)
    }
}
