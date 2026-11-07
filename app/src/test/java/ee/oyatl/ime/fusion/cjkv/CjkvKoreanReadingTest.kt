package ee.oyatl.ime.fusion.cjkv

import ee.oyatl.ime.fusion.cjkv.CjkvKoreanReading.Segment
import org.junit.Assert.assertEquals
import org.junit.Test

class CjkvKoreanReadingTest {
    @Test
    fun hunTypesTheLabelAndEumTypesTheReading() {
        assertEquals(
            listOf(Segment("무거울", "무거울 중"), Segment(" "), Segment("중", "중")),
            CjkvKoreanReading.segments("무거울 중", listOf("중")),
        )
    }

    @Test
    fun readingsNoLabelCoversComeAfterTheLabels() {
        assertEquals(
            listOf(
                Segment("마칠", "마칠 료"), Segment(" "), Segment("료", "료"),
                Segment(", "),
                Segment("깨달을", "깨달을 료"), Segment(" "), Segment("료", "료"),
                Segment(" · "),
                Segment("요", "요"),
            ),
            CjkvKoreanReading.segments("마칠 료, 깨달을 료", listOf("료", "요")),
        )
    }

    @Test
    fun labelsWithoutAReadingAreTypedWhole() {
        assertEquals(
            listOf(Segment("땅이름", "땅이름"), Segment(" · "), Segment("볼", "볼")),
            CjkvKoreanReading.segments("땅이름", listOf("볼")),
        )
        assertEquals(
            listOf(Segment("竝과 同字", "竝과 同字"), Segment(" · "), Segment("병", "병")),
            CjkvKoreanReading.segments("竝과 同字", listOf("병")),
        )
    }

    @Test
    fun withoutLabelsOnlyTheReadingsAreShown() {
        assertEquals(
            listOf(Segment("부", "부"), Segment(" · "), Segment("불", "불")),
            CjkvKoreanReading.segments(null, listOf("부", "불")),
        )
    }
}
