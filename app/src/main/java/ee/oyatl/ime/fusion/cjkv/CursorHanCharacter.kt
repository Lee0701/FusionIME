package ee.oyatl.ime.fusion.cjkv

import android.view.inputmethod.InputConnection

data class CursorHanCharacter(
    val codePoint: Int,
    val variationSelector: Int? = null,
) {
    val text: String = buildString {
        appendCodePoint(codePoint)
        variationSelector?.let(::appendCodePoint)
    }

    val codePointLabel: String = buildString {
        append(formatCodePoint(codePoint))
        variationSelector?.let {
            append(" + ")
            append(formatCodePoint(it))
        }
    }

    private fun formatCodePoint(value: Int): String = "U+%04X".format(value)
}

sealed interface CursorCharacterResult {
    data class Han(val character: CursorHanCharacter): CursorCharacterResult
    data class NotHan(val text: String, val codePoint: Int): CursorCharacterResult
    data object Empty: CursorCharacterResult
    data object Unavailable: CursorCharacterResult
}

internal fun readCharacterBeforeCursor(inputConnection: InputConnection): CursorCharacterResult {
    val text = try {
        inputConnection.getTextBeforeCursor(MAX_CURSOR_TEXT_UNITS, 0)
    } catch(_: RuntimeException) {
        null
    } ?: return CursorCharacterResult.Unavailable
    return extractCharacterBeforeCursor(text)
}

internal fun extractCharacterBeforeCursor(text: CharSequence): CursorCharacterResult {
    if(text.isEmpty()) return CursorCharacterResult.Empty
    var index = text.length
    var codePoint = Character.codePointBefore(text, index)
    var variationSelector: Int? = null
    if(isVariationSelector(codePoint)) {
        variationSelector = codePoint
        index -= Character.charCount(codePoint)
        if(index == 0) return CursorCharacterResult.NotHan(String(Character.toChars(codePoint)), codePoint)
        codePoint = Character.codePointBefore(text, index)
    }
    if(Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.HAN) {
        return CursorCharacterResult.NotHan(String(Character.toChars(codePoint)), codePoint)
    }
    return CursorCharacterResult.Han(CursorHanCharacter(codePoint, variationSelector))
}

private fun isVariationSelector(codePoint: Int): Boolean {
    return codePoint in 0xFE00..0xFE0F || codePoint in 0xE0100..0xE01EF
}

private const val MAX_CURSOR_TEXT_UNITS = 8
