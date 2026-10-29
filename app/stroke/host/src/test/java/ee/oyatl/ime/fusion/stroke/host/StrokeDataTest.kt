package ee.oyatl.ime.fusion.stroke.host

import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeDataTest {
    @Test
    fun parsesSequencesSkippingComments() {
        val sequences = StrokeData.parseSequences(listOf(
            "# comment",
            "",
            "1\t一",
            "1111251131523\t𧧝"
        ))
        assertEquals(listOf('一'.code), sequences["1"])
        assertEquals(listOf(0x279dd), sequences["1111251131523"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidStrokes() {
        StrokeData.parseSequences(listOf("16\t一"))
    }

    @Test
    fun ranksByFirstAppearance() {
        val ranks = StrokeData.parseRanking(listOf("# header", "是不一", "了是 的"))
        assertEquals(mapOf('是'.code to 0, '不'.code to 1, '一'.code to 2, '了'.code to 3, '的'.code to 4), ranks)
    }

    @Test
    fun combinesFlags() {
        val data = StrokeData(
            sequences = emptyMap(),
            simplifiedRanks = emptyMap(),
            traditionalRanks = emptyMap(),
            traditionalOnly = setOf('沒'.code),
            simplifiedOnly = setOf('没'.code),
            unlikelyFontSupport = setOf('沒'.code)
        )
        assertEquals(StrokeData.FLAG_TRADITIONAL_ONLY or StrokeData.FLAG_UNLIKELY_FONT_SUPPORT, data.flagsOf('沒'.code))
        assertEquals(StrokeData.FLAG_SIMPLIFIED_ONLY, data.flagsOf('没'.code))
        assertEquals(0, data.flagsOf('一'.code))
    }
}
