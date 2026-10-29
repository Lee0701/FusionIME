package ee.oyatl.ime.fusion.pinyin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinInputLimitsTest {
    @Test
    fun acceptsFullPinyinAtLimitAndRejectsOverflow() {
        val fullPinyin = "zhongguo".repeat(15)

        assertTrue(PinyinInputLimits.canApplyEdit(0, 0, fullPinyin.length))
        assertFalse(PinyinInputLimits.canApplyEdit(0, 0, fullPinyin.length + 1))
    }

    @Test
    fun apostrophesCountTowardTheLimit() {
        val separatedPinyin = "a'".repeat(PinyinInputLimits.MAX_INPUT_LENGTH / 2)

        assertTrue(PinyinInputLimits.canApplyEdit(0, 0, separatedPinyin.length))
        assertFalse(PinyinInputLimits.canApplyEdit(separatedPinyin.length, 0, 1))
    }

    @Test
    fun expandedDoublePinyinReplacementIsCheckedAtomically() {
        // A pending "sh" can be replaced by the decoded syllable "shuang".
        assertTrue(PinyinInputLimits.canApplyEdit(116, 2, 6))
        assertFalse(PinyinInputLimits.canApplyEdit(117, 2, 6))
    }

    @Test
    fun invalidEditBoundsAreRejected() {
        assertFalse(PinyinInputLimits.canApplyEdit(10, 11, 1))
        assertFalse(PinyinInputLimits.canApplyEdit(-1, 0, 1))
        assertFalse(PinyinInputLimits.canApplyEdit(10, 0, -1))
    }
}
