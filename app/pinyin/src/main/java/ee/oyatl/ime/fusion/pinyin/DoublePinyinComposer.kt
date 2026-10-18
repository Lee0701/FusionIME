package ee.oyatl.ime.fusion.pinyin

class DoublePinyinComposer(private val scheme: DoublePinyinScheme) {
    data class Edit(
        val removeBeforeCursor: Int = 0,
        val append: String = ""
    )

    private data class Syllable(
        val first: Char,
        val emittedLength: Int,
        val prefix: String
    )

    private val syllables = ArrayDeque<Syllable>()
    private var pendingFirst: Char? = null
    private var pendingPrefix = ""
    private var pendingEmissionLength = 0

    fun reset() {
        syllables.clear()
        clearPending()
    }

    fun canType(ch: Char): Boolean = if(pendingFirst == null) {
        ch in 'a'..'z'
    } else {
        scheme.acceptsFinal(ch)
    }

    fun type(ch: Char, prependSeparator: Boolean): Edit {
        require(canType(ch))

        val first = pendingFirst
        if(first != null) {
            val replacement = pendingPrefix + scheme.decode(first, ch)
            syllables.addLast(Syllable(first, replacement.length, pendingPrefix))
            return Edit(pendingEmissionLength, replacement).also { clearPending() }
        }

        val prefix = if(prependSeparator) "'" else ""
        val provisional = prefix + scheme.expandInitial(ch)
        pendingFirst = ch
        pendingPrefix = prefix
        pendingEmissionLength = provisional.length
        return Edit(append = provisional)
    }

    fun backspace(): Edit? {
        if(pendingFirst != null) {
            return Edit(removeBeforeCursor = pendingEmissionLength).also { clearPending() }
        }

        val syllable = syllables.removeLastOrNull() ?: return null
        val provisional = syllable.prefix + scheme.expandInitial(syllable.first)
        pendingFirst = syllable.first
        pendingPrefix = syllable.prefix
        pendingEmissionLength = provisional.length
        return Edit(syllable.emittedLength, provisional)
    }

    private fun clearPending() {
        pendingFirst = null
        pendingPrefix = ""
        pendingEmissionLength = 0
    }
}
