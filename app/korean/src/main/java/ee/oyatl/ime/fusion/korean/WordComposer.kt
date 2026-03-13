package ee.oyatl.ime.fusion.korean

import kotlin.math.max
import kotlin.math.min

class WordComposer {
    private var composingWord: String = ""
    private var composingChar: String = ""
    private var cursor: Int = 0
    val composingText: String get() = composingWord.take(cursor) + composingChar + composingWord.drop(cursor)
    val textBeforeCursor: String get() = composingWord.take(cursor) + composingChar
    val cursorPosition: Int get() = cursor

    fun compose(text: String) {
        composingChar = text
    }

    fun commit(text: String) {
        if(text.isEmpty()) return
        composingWord = composingWord.take(cursor) + text + composingWord.drop(cursor)
        moveCursorRight(text.length)
        composingChar = ""
    }

    fun commit() = this.commit(composingChar)

    fun delete(length: Int): Boolean {
        val result = cursor >= length
        composingWord = composingWord.take(cursor).dropLast(length) + composingChar + composingWord.drop(cursor)
        moveCursorLeft(length)
        return result
    }

    fun consume(length: Int) {
        commit(composingChar)
        composingWord = composingText.drop(length)
        moveCursorLeft(length)
    }

    fun moveCursor(position: Int) {
        cursor = position
        cursor = max(cursor, 0)
        cursor = min(cursor, composingWord.length)
    }

    fun moveCursorLeft(amount: Int) {
        moveCursor(cursor - amount)
    }

    fun moveCursorRight(amount: Int) {
        moveCursor(cursor + amount)
    }

    fun reset() {
        composingWord = ""
        composingChar = ""
    }
}