package ee.oyatl.ime.fusion.pinyin

import com.android.inputmethod.pinyin.PinyinIME.ImeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodingInfoLengthTest {
    private fun decodingInfo() = DecodingInfo(object : DecodingInfo.IMEStateHolder {
        override val imeState = ImeState.STATE_INPUT
    })

    @Test
    fun fullPinyinStopsAtTheLimitWithoutChangingComposition() {
        val decodingInfo = decodingInfo()

        repeat(PinyinInputLimits.MAX_INPUT_LENGTH) {
            assertTrue(decodingInfo.addSplChar('a', reset = false))
        }

        val compositionAtLimit = decodingInfo.origianlSplStr.toString()
        assertTrue(decodingInfo.isSplStrFull)
        assertFalse(decodingInfo.addSplChar('a', reset = false))
        assertEquals(compositionAtLimit, decodingInfo.origianlSplStr.toString())
        assertEquals(PinyinInputLimits.MAX_INPUT_LENGTH, decodingInfo.cursorPos)
    }

    @Test
    fun apostrophesUseTheSameInputBudget() {
        val decodingInfo = decodingInfo()

        repeat(PinyinInputLimits.MAX_INPUT_LENGTH / 2) {
            assertTrue(decodingInfo.addSplChar('a', reset = false))
            assertTrue(decodingInfo.addSplChar('\'', reset = false))
        }

        assertTrue(decodingInfo.isSplStrFull)
        assertFalse(decodingInfo.addSplChar('a', reset = false))
        assertEquals(PinyinInputLimits.MAX_INPUT_LENGTH, decodingInfo.length())
    }
}
