package ee.oyatl.ime.fusion.pinyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoublePinyinComposerTest {
    private val composer = DoublePinyinComposer(DoublePinyinScheme.Ziranma)

    @Test
    fun replacesProvisionalInitialWithDecodedSyllable() {
        assertEquals(
            DoublePinyinComposer.Edit(append = "sh"),
            composer.type('u', prependSeparator = false)
        )
        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 2, append = "shuang"),
            composer.type('d', prependSeparator = false)
        )
    }

    @Test
    fun prefixesFollowingSyllablesWithSeparator() {
        composer.type('j', prependSeparator = false)
        composer.type('m', prependSeparator = false)

        assertEquals(
            DoublePinyinComposer.Edit(append = "'g"),
            composer.type('g', prependSeparator = true)
        )
        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 2, append = "'gong"),
            composer.type('s', prependSeparator = false)
        )
    }

    @Test
    fun backspaceRemovesPendingFirstKey() {
        composer.type('u', prependSeparator = false)

        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 2),
            composer.backspace()
        )
        assertNull(composer.backspace())
    }

    @Test
    fun backspaceRestoresFirstKeyOfCompletedSyllable() {
        composer.type('u', prependSeparator = false)
        composer.type('d', prependSeparator = false)

        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 6, append = "sh"),
            composer.backspace()
        )
        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 2),
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
            DoublePinyinComposer.Edit(removeBeforeCursor = 7, append = "'zh"),
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

    @Test
    fun acceptsSemicolonOnlyAsAConfiguredSecondKey() {
        val microsoft = DoublePinyinComposer(DoublePinyinScheme.Microsoft)
        assertFalse(microsoft.canType(';'))

        microsoft.type('j', prependSeparator = false)
        assertTrue(microsoft.canType(';'))
        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 1, append = "jing"),
            microsoft.type(';', prependSeparator = false)
        )
    }

    @Test
    fun usesSchemeSpecificProvisionalInitials() {
        val smartABC = DoublePinyinComposer(DoublePinyinScheme.SmartABC)
        assertEquals(
            DoublePinyinComposer.Edit(append = "zh"),
            smartABC.type('a', prependSeparator = false)
        )
        assertEquals(
            DoublePinyinComposer.Edit(removeBeforeCursor = 2, append = "zhuang"),
            smartABC.type('t', prependSeparator = false)
        )
    }
}
