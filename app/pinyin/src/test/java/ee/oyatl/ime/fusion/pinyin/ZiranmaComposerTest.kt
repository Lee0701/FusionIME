package ee.oyatl.ime.fusion.pinyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZiranmaComposerTest {
    private val composer = ZiranmaComposer()

    @Test
    fun replacesProvisionalInitialWithDecodedSyllable() {
        assertEquals(
            ZiranmaComposer.Edit(append = "sh"),
            composer.type('u', prependSeparator = false)
        )
        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 2, append = "shuang"),
            composer.type('d', prependSeparator = false)
        )
    }

    @Test
    fun prefixesFollowingSyllablesWithSeparator() {
        composer.type('j', prependSeparator = false)
        composer.type('m', prependSeparator = false)

        assertEquals(
            ZiranmaComposer.Edit(append = "'g"),
            composer.type('g', prependSeparator = true)
        )
        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 2, append = "'gong"),
            composer.type('s', prependSeparator = false)
        )
    }

    @Test
    fun backspaceRemovesPendingFirstKey() {
        composer.type('u', prependSeparator = false)

        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 2),
            composer.backspace()
        )
        assertNull(composer.backspace())
    }

    @Test
    fun backspaceRestoresFirstKeyOfCompletedSyllable() {
        composer.type('u', prependSeparator = false)
        composer.type('d', prependSeparator = false)

        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 6, append = "sh"),
            composer.backspace()
        )
        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 2),
            composer.backspace()
        )
    }

    @Test
    fun backspacePreservesSyllableSeparator() {
        composer.type('j', prependSeparator = false)
        composer.type('m', prependSeparator = false)
        composer.type('v', prependSeparator = true)
        composer.type('d', prependSeparator = false)

        assertEquals(
            ZiranmaComposer.Edit(removeBeforeCursor = 7, append = "'zh"),
            composer.backspace()
        )
    }

    @Test
    fun resetClearsEditHistory() {
        composer.type('j', prependSeparator = false)
        composer.type('m', prependSeparator = false)
        composer.reset()

        assertNull(composer.backspace())
    }
}
