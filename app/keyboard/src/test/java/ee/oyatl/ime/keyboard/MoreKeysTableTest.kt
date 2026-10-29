package ee.oyatl.ime.keyboard

import ee.oyatl.ime.fusion.keyboard.core.MoreKeysTable
import ee.oyatl.ime.fusion.layout.LatinMoreKeysTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreKeysTableTest {
    @Test
    fun latinTableProvidesConfiguredCandidates() {
        val table = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        assertEquals("àáâäæãåā".codePointList(), table.candidatesFor('a'.code))
        assertEquals("çćč".codePointList(), table.candidatesFor('c'.code))
        assertEquals("èéêëēėę".codePointList(), table.candidatesFor('e'.code))
        assertEquals("îïíīįì".codePointList(), table.candidatesFor('i'.code))
        assertEquals("ł".codePointList(), table.candidatesFor('l'.code))
        assertEquals("ñń".codePointList(), table.candidatesFor('n'.code))
        assertEquals("ôöòóœøōõ".codePointList(), table.candidatesFor('o'.code))
        assertEquals("ßśš".codePointList(), table.candidatesFor('s'.code))
        assertEquals("ûüùúū".codePointList(), table.candidatesFor('u'.code))
        assertEquals("ÿ".codePointList(), table.candidatesFor('y'.code))
        assertEquals("žźż".codePointList(), table.candidatesFor('z'.code))
    }

    @Test
    fun uppercaseBaseProducesUppercaseCandidates() {
        val table = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        assertEquals("ÀÁÂÄÆÃÅĀ".codePointList(), table.candidatesFor('A'.code))
        assertEquals("ẞŚŠ".codePointList(), table.candidatesFor('S'.code))
    }

    @Test
    fun numberAndSymbolKeysProvideConfiguredCandidates() {
        val table = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        val expected = mapOf(
            '1' to "¹½⅓¼⅕⅙⅐⅛⅑⅒",
            '2' to "²⅔⅖",
            '3' to "³¾⅗⅜",
            '4' to "⁴⅘",
            '5' to "⁵⅚⅝",
            '6' to "⁶",
            '7' to "⁷",
            '8' to "⁸",
            '9' to "⁹",
            '0' to "⁰ⁿ∅",
            '#' to "№",
            '-' to "_–—·†‡★±",
            '(' to "<{[",
            ')' to ">}]",
            '"' to "“”„«»",
            '\'' to "‘’‚‹›",
            '!' to "¡",
            '?' to "¿‽",
            '.' to "…!?'/;:\"%@",
            '=' to "≠≈",
            '^' to "←↑↓→",
            '<' to "≤≪⟨",
            '>' to "≥≫⟩",
            '/' to "\\÷",
            '%' to "‰℅",
            '&' to "§",
            '$' to "€£¥₩¢"
        )

        expected.forEach { (base, candidates) ->
            assertEquals(candidates.codePointList(), table.candidatesFor(base.code))
        }
    }

    @Test
    fun keyWithoutCandidatesIsUnchanged() {
        val table = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        assertTrue(table.candidatesFor('q'.code).isEmpty())
    }

    private fun String.codePointList(): List<Int> {
        val result = mutableListOf<Int>()
        var index = 0
        while(index < length) {
            val codePoint = codePointAt(index)
            result += codePoint
            index += Character.charCount(codePoint)
        }
        return result
    }
}
