package ee.oyatl.ime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressTableTest {
    @Test
    fun latinTableProvidesConfiguredCandidates() {
        assertEquals("àáâäæãåā".codePointList(), candidatesFor('a'))
        assertEquals("çćč".codePointList(), candidatesFor('c'))
        assertEquals("èéêëēėę".codePointList(), candidatesFor('e'))
        assertEquals("îïíīįì".codePointList(), candidatesFor('i'))
        assertEquals("ł".codePointList(), candidatesFor('l'))
        assertEquals("ñń".codePointList(), candidatesFor('n'))
        assertEquals("ôöòóœøōõ".codePointList(), candidatesFor('o'))
        assertEquals("ßśš".codePointList(), candidatesFor('s'))
        assertEquals("ûüùúū".codePointList(), candidatesFor('u'))
        assertEquals("ÿ".codePointList(), candidatesFor('y'))
        assertEquals("žźż".codePointList(), candidatesFor('z'))
    }

    @Test
    fun uppercaseBaseProducesUppercaseCandidates() {
        assertEquals("ÀÁÂÄÆÃÅĀ".codePointList(), candidatesFor('A'))
        assertEquals("ẞŚŠ".codePointList(), candidatesFor('S'))
    }

    @Test
    fun numberAndSymbolKeysProvideConfiguredCandidates() {
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
            assertEquals(candidates.codePointList(), candidatesFor(base))
        }
    }

    @Test
    fun keyWithoutCandidatesIsUnchanged() {
        assertTrue(LatinLongPressTable.Default.candidatesFor('q'.code).isEmpty())
    }

    private fun candidatesFor(base: Char): List<Int> {
        return LatinLongPressTable.Default.candidatesFor(base.code)
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
