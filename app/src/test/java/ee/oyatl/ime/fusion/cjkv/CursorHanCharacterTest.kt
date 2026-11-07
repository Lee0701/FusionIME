package ee.oyatl.ime.fusion.cjkv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CursorHanCharacterTest {
    @Test
    fun extractsBmpHanCharacter() {
        val result = extractCharacterBeforeCursor("文字") as CursorCharacterResult.Han
        assertEquals(0x5B57, result.character.codePoint)
        assertEquals("字", result.character.text)
        assertEquals("U+5B57", result.character.codePointLabel)
    }

    @Test
    fun extractsSupplementaryHanCharacter() {
        val result = extractCharacterBeforeCursor("𠮷") as CursorCharacterResult.Han
        assertEquals(0x20BB7, result.character.codePoint)
        assertEquals("U+20BB7", result.character.codePointLabel)
    }

    @Test
    fun keepsIdeographicVariationSelectorWithBaseCharacter() {
        val text = "葛" + String(Character.toChars(0xE0100))
        val result = extractCharacterBeforeCursor(text) as CursorCharacterResult.Han
        assertEquals(0x845B, result.character.codePoint)
        assertEquals(0xE0100, result.character.variationSelector)
        assertEquals("U+845B + U+E0100", result.character.codePointLabel)
        assertEquals(text, result.character.text)
    }

    @Test
    fun reportsNonHanCharacter() {
        val result = extractCharacterBeforeCursor("漢A") as CursorCharacterResult.NotHan
        assertEquals("A", result.text)
        assertEquals(0x41, result.codePoint)
    }

    @Test
    fun reportsEmptyText() {
        assertSame(CursorCharacterResult.Empty, extractCharacterBeforeCursor(""))
    }
}
