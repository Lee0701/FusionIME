package ee.oyatl.ime.fusion.stroke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrokeTest {
    @Test
    fun mapsKeyLabelsToStrokeDigits() {
        assertEquals('1', Stroke.ofLabel('一'.code))
        assertEquals('2', Stroke.ofLabel('丨'.code))
        assertEquals('3', Stroke.ofLabel('丿'.code))
        assertEquals('4', Stroke.ofLabel('丶'.code))
        assertEquals('5', Stroke.ofLabel('乙'.code))
        assertEquals(Stroke.WILDCARD, Stroke.ofLabel('＊'.code))
    }

    @Test
    fun ignoresOtherCharacters() {
        assertNull(Stroke.ofLabel('a'.code))
        assertNull(Stroke.ofLabel('，'.code))
        assertNull(Stroke.ofLabel(0x20000))
    }

    @Test
    fun labelsStrokes() {
        assertEquals("一丨丿丶乙＊", "12345${Stroke.WILDCARD}".map(Stroke::label).joinToString(""))
    }
}
