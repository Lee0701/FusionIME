package ee.oyatl.ime.fusion.mode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MozcTextConversionTest {
    @Test
    fun convertsHiraganaToKatakana() {
        assertEquals("カタカナヲヴヽヾ", hiraganaToKatakana("かたかなをゔゝゞ"))
        assertEquals("漢字・ABC", hiraganaToKatakana("漢字・ABC"))
    }

    @Test
    fun parsesIndependentModeSettings() {
        val enabled = MozcIMEMode.Params.parse(
            mapOf(
                "kyujitai_enabled" to "true",
                "katakana_enabled" to "true"
            )
        )
        assertTrue(enabled.kyujitaiEnabled)
        assertTrue(enabled.katakanaEnabled)

        val defaults = MozcIMEMode.Params.parse(emptyMap())
        assertFalse(defaults.kyujitaiEnabled)
        assertFalse(defaults.katakanaEnabled)
    }
}
