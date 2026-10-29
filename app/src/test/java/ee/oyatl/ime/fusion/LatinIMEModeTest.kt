package ee.oyatl.ime.fusion

import ee.oyatl.ime.fusion.mode.LatinIMEMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LatinIMEModeTest {
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
}