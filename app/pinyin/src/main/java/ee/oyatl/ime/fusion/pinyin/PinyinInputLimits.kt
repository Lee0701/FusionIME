package ee.oyatl.ime.fusion.pinyin

object PinyinInputLimits {
    // Native DictMatchInfo stores spelling lengths in 7-bit fields.
    const val MAX_INPUT_LENGTH = 120
    const val BUFFER_SIZE = MAX_INPUT_LENGTH + 1

    fun canApplyEdit(
        currentLength: Int,
        removeBeforeCursor: Int,
        appendLength: Int
    ): Boolean {
        if (currentLength < 0 || removeBeforeCursor !in 0..currentLength || appendLength < 0) {
            return false
        }

        val retainedLength = currentLength - removeBeforeCursor
        return appendLength <= MAX_INPUT_LENGTH - retainedLength
    }
}
