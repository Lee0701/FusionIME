package ee.oyatl.ime.fusion.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StrokeDictionaryTest {
    private val dictionary: StrokeDictionary = run {
        val raw = File("src/main/res/raw")
        val ranking = File(raw, "stroke_ranking.bin").inputStream().use(StrokeRanking::read)
        File(raw, "stroke_sequences.bin").inputStream().use { StrokeDictionary(it, ranking) }
    }

    private fun search(strokes: String, traditional: Boolean = false, limit: Int = StrokeDictionary.DEFAULT_LIMIT): List<String> =
        dictionary.search(strokes, traditional, limit).map { String(Character.toChars(it)) }

    @Test
    fun exactMatchComesFirst() {
        assertEquals("一", search("1").first())
        assertEquals("二", search("11").first())
        assertEquals("是", search("251112134").first())
    }

    @Test
    fun charactersOfTheChosenScriptComeFirst() {
        val simplified = search("4413554", traditional = false)
        val traditional = search("4413554", traditional = true)
        assertTrue(simplified.indexOf("没") < simplified.indexOf("沒"))
        assertTrue(traditional.indexOf("沒") < traditional.indexOf("没"))
    }

    @Test
    fun continuationsAreRankedByTheChosenScript() {
        // 为 is 4354 and 為 is 435554444; both continue 435.
        val simplified = search("435", traditional = false)
        val traditional = search("435", traditional = true)
        assertTrue("为" in simplified)
        assertTrue(simplified.indexOf("为") < simplified.indexOf("為"))
        assertTrue(traditional.indexOf("為") < traditional.indexOf("为"))
    }

    @Test
    fun exactMatchesPrecedeContinuations() {
        // 二 is exactly 11; 三 (111) continues it.
        val result = search("11")
        assertEquals("二", result.first())
        assertTrue(result.indexOf("三") > 0)
    }

    @Test
    fun wildcardMatchesAnyStroke() {
        val withWildcard = search("44135" + Stroke.WILDCARD + "4")
        assertTrue("没" in withWildcard)
        assertTrue("沒" in withWildcard)
    }

    @Test
    fun returnsCharactersOutsideTheBasicPlane() {
        assertTrue("𧧝" in search("1111251131523"))
    }

    @Test
    fun emptyOrUnknownInputGivesNothing() {
        assertEquals(emptyList<String>(), search(""))
        assertEquals(emptyList<String>(), search("5555555555555555555555"))
    }

    @Test
    fun respectsTheLimit() {
        assertEquals(10, search("1", limit = 10).size)
    }

    @Test
    fun shortInputIsFastEnough() {
        val inputs = listOf("1", "2", "3", "4", "5", Stroke.WILDCARD.toString())
        inputs.forEach { search(it) }
        val start = System.nanoTime()
        inputs.forEach { search(it) }
        val millis = (System.nanoTime() - start) / 1_000_000
        println("six one-stroke searches took $millis ms after warm-up")
        assertTrue("took $millis ms", millis < 5_000)
    }
}
