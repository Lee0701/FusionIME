package ee.oyatl.ime.fusion.korean

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.UnderlineSpan
import kotlin.math.max
import kotlin.math.min

class WordComposer {
    private var composingWord: String = ""
    private var composingChar: String = ""
    private var cursor: Int = 0
    val composingText: String get() = composingWord.take(cursor) + composingChar + composingWord.drop(cursor)
    val textBeforeCursor: String get() = composingWord.take(cursor) + composingChar
    val textAfterCursor: String get() = composingWord.drop(cursor)

    fun compose(text: String) {
        composingChar = text
    }

    fun commit(text: String) {
        if(text.isEmpty()) return
        composingWord = composingWord.take(cursor) + text + composingWord.drop(cursor)
        moveCursorRelative(text.length)
        composingChar = ""
    }

    fun commit() = this.commit(composingChar)

    fun delete(length: Int): Boolean {
        val result = cursor >= length
        composingWord = composingWord.take(cursor).dropLast(length) + composingChar + composingWord.drop(cursor)
        moveCursorRelative(-length)
        return result
    }

    fun consume(length: Int) {
        commit(composingChar)
        composingWord = composingText.drop(length)
        moveCursorRelative(-length)
    }

    fun replace(length: Int, text: String) {
        consume(length)
        composingWord = text + composingWord
        moveCursorRelative(text.length)
    }

    fun moveCursor(position: Int): Boolean {
        cursor = position
        cursor = max(cursor, 0)
        cursor = min(cursor, composingWord.length)
        return cursor == position
    }

    fun moveCursorRelative(amount: Int): Boolean {
        return moveCursor(cursor + amount)
    }

    fun getSpannableSurfaceString(): CharSequence {
        return buildSpannableSurfaceString(textBeforeCursor, textAfterCursor)
    }

    fun reset() {
        composingWord = ""
        composingChar = ""
        cursor = 0
    }

    companion object {
        const val COMPOSING_BACKGROUND: Int = 0x664DB6AC

        fun buildSpannableSurfaceString(
            textBeforeCursor: String,
            textAfterCursor: String
        ): CharSequence {
            val builder = SpannableStringBuilder()
            builder.append(textBeforeCursor, BackgroundColorSpan(COMPOSING_BACKGROUND), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append(textAfterCursor)
            builder.setSpan(UnderlineSpan(), 0, textBeforeCursor.length + textAfterCursor.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
    }
}