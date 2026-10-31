package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.fusion.mode.CangjieIMEMode
import ee.oyatl.ime.fusion.stroke.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutStrokeTest {
    private fun output(keyCode: Int): Int = LayoutStroke.TABLE_12KEY.getValue(keyCode).single()

    @Test
    fun strokeKeysTypeTheFiveStrokesAndTheWildcard() {
        val strokes = listOf(
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6
        ).map { Stroke.ofLabel(output(it)) }
        assertEquals(listOf('1', '2', '3', '4', '5', Stroke.WILDCARD), strokes)
    }

    @Test
    fun punctuationKeysAreNotStrokes() {
        listOf(
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_PERIOD
        ).forEach { assertNull(Stroke.ofLabel(output(it))) }
    }

    @Test
    fun everyKeyInTheRowsHasAnOutput() {
        assertEquals(12, LayoutStroke.ROWS_12KEY.sumOf { it.length })
        assertEquals(12, LayoutStroke.TABLE_12KEY.size)
    }

    @Test
    fun parsesScriptSetting() {
        val stroke = CangjieIMEMode.Params.parse(mapOf("type" to "cangjie", "layout" to "Stroke"))
        assertEquals(CangjieIMEMode.Layout.Stroke, stroke.layout)
        assertFalse(stroke.chineseTraditional)
        assertTrue(CangjieIMEMode.Params.parse(mapOf("type" to "cangjie", "layout" to "Stroke", "chinese_traditional" to "true")).chineseTraditional)
    }
}
