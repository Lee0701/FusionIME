package ee.oyatl.ime.fusion.hangul

class CheonjiinComposer(
    private val hangulCombiner: HangulCombiner
): Combiner {
    fun canConfirmConsonantCycle(state: Combiner.State): Boolean {
        return (state as? State)?.activeConsonantKey != null
    }

    override fun combine(state: Combiner.State, input: Int): Combiner.Result {
        val previous = state as? State ?: State.Initial
        val consonants = CONSONANTS[input]
        if(consonants != null) {
            val nextIndex =
                if(previous.activeConsonantKey == input) {
                    (previous.activeConsonantIndex + 1) % consonants.size
                } else {
                    0
                }
            return selectConsonant(previous, input, nextIndex, fromTap = true)
        }

        val history = previous.keyHistory + input
        return result(
            history = history,
            previous = previous,
            lastTapKey = input,
            beforeLastTap = previous
        )
    }

    fun selectConsonant(
        state: Combiner.State,
        sourceKey: Int,
        targetIndex: Int
    ): Combiner.Result {
        val current = state as? State ?: State.Initial
        return selectConsonant(current, sourceKey, targetIndex, fromTap = false)
    }

    fun selectCompletedVowel(
        state: Combiner.State,
        sourceKey: Int,
        vowel: Int
    ): Combiner.Result {
        val current = state as? State ?: State.Initial
        val base = current.beforeFlick(sourceKey)

        // 完成母音を天地人のストローク列へ戻して履歴へ追加する。
        // これにより、フリック後も後続の母音と合成できる。
        val strokes = VOWELS.entries
            .firstOrNull { (_, completedVowel) -> completedVowel == vowel }
            ?.key
            ?.map { it.codePoint }

        val history =
            if(strokes != null) {
                base.keyHistory + strokes
            } else {
                base.keyHistory + vowel
            }

        // previous は追加前の状態を指すので、
        // Backspaceではフリックで入れた母音全体が一度に消える。
        return result(
            history = history,
            previous = base
        )
    }

    fun cancelFlick(state: Combiner.State, sourceKey: Int): Combiner.Result {
        val current = state as? State ?: State.Initial
        return Combiner.Result(
            textToCommit = emptyList(),
            newState = current.beforeFlick(sourceKey)
        )
    }

    private fun selectConsonant(
        current: State,
        sourceKey: Int,
        targetIndex: Int,
        fromTap: Boolean
    ): Combiner.Result {
        val candidates = CONSONANTS[sourceKey] ?: return Combiner.Result(emptyList(), current)
        if(targetIndex !in candidates.indices) return Combiner.Result(emptyList(), current)

        val base = if(fromTap) current else current.beforeFlick(sourceKey)
        val replacing = base.activeConsonantKey == sourceKey && base.keyHistory.isNotEmpty()
        val token = CONSONANT_TOKENS.getValue(sourceKey)[targetIndex]
        val history =
            if(replacing) base.keyHistory.dropLast(1) + token
            else base.keyHistory + token
        val undoState = if(replacing) base.previous else base

        return result(
            history = history,
            previous = undoState,
            activeConsonantKey = sourceKey,
            activeConsonantIndex = targetIndex,
            lastTapKey = if(fromTap) sourceKey else null,
            beforeLastTap = if(fromTap) current else null
        )
    }

    private fun result(
        history: List<Int>,
        previous: State?,
        activeConsonantKey: Int? = null,
        activeConsonantIndex: Int = 0,
        lastTapKey: Int? = null,
        beforeLastTap: State? = null
    ): Combiner.Result {
        return Combiner.Result(
            textToCommit = emptyList(),
            newState = State(
                keyHistory = history,
                activeConsonantKey = activeConsonantKey,
                activeConsonantIndex = activeConsonantIndex,
                lastTapKey = lastTapKey,
                beforeLastTap = beforeLastTap,
                combined = rebuild(history),
                previous = previous
            )
        )
    }

    private fun rebuild(history: List<Int>): String {
        val jamo = mutableListOf<Int>()
        val vowelStrokes = mutableListOf<VowelStroke>()
        fun flushVowel() {
            VOWELS[vowelStrokes.toList()]?.let(jamo::add)
            vowelStrokes.clear()
        }

        history.forEach { input ->
            val stroke = VowelStroke.from(input)
            val consonant = CONSONANT_TOKEN_VALUES[input]
            when {
                consonant != null -> {
                    flushVowel()
                    jamo += consonant
                }
                stroke != null -> {
                    val candidate = vowelStrokes + stroke
                    if(VOWELS.keys.any { it.startsWith(candidate) }) {
                        vowelStrokes += stroke
                    } else {
                        flushVowel()
                        vowelStrokes += stroke
                    }
                }
                input == CONFIRM_INPUT -> {
                    flushVowel()
                }
                else -> {
                    flushVowel()
                    jamo += input
                }
            }
        }
        flushVowel()

        var state: Combiner.State = HangulCombiner.State.Initial
        val combined = StringBuilder()
        jamo.forEach { input ->
            val result = hangulCombiner.combine(state, input)
            result.textToCommit.forEach(combined::append)
            state = result.newState
        }
        combined.append(state.combined)
        return combined.toString()
    }

    data class State(
        val keyHistory: List<Int> = emptyList(),
        val activeConsonantKey: Int? = null,
        val activeConsonantIndex: Int = 0,
        val lastTapKey: Int? = null,
        val beforeLastTap: State? = null,
        override val combined: CharSequence = "",
        override val previous: State? = null
    ): Combiner.State {
        fun beforeFlick(sourceKey: Int): State {
            return if(lastTapKey == sourceKey) beforeLastTap ?: this else this
        }

        companion object {
            val Initial = State()
        }
    }

    private enum class VowelStroke(val codePoint: Int) {
        I(0x3163),
        DOT(0x318d),
        EU(0x3161);

        companion object {
            fun from(codePoint: Int): VowelStroke? = entries.find { it.codePoint == codePoint }
        }
    }

    companion object {
        const val CONFIRM_INPUT = -1

        private val CONSONANTS = mapOf(
            0x3131 to listOf(0x3131, 0x314b, 0x3132),
            0x3134 to listOf(0x3134, 0x3139),
            0x3137 to listOf(0x3137, 0x314c, 0x3138),
            0x3142 to listOf(0x3142, 0x314d, 0x3143),
            0x3145 to listOf(0x3145, 0x314e, 0x3146),
            0x3148 to listOf(0x3148, 0x314a, 0x3149),
            0x3147 to listOf(0x3147, 0x3141)
        )

        private const val CONSONANT_TOKEN_BASE = 0x110000
        private val CONSONANT_TOKENS: Map<Int, List<Int>> = buildMap {
            var token = CONSONANT_TOKEN_BASE
            CONSONANTS.forEach { (key, candidates) ->
                put(key, candidates.map { token++ })
            }
        }
        private val CONSONANT_TOKEN_VALUES: Map<Int, Int> = buildMap {
            CONSONANTS.forEach { (key, candidates) ->
                CONSONANT_TOKENS.getValue(key).zip(candidates).forEach { (token, value) ->
                    put(token, value)
                }
            }
        }

        private val VOWELS = mapOf(
            listOf(VowelStroke.DOT) to 0x30fb,
            listOf(VowelStroke.DOT, VowelStroke.DOT) to 0xff1a,
            listOf(VowelStroke.I) to 0x3163,
            listOf(VowelStroke.I, VowelStroke.DOT) to 0x314f,
            listOf(VowelStroke.I, VowelStroke.DOT, VowelStroke.DOT) to 0x3151,
            listOf(VowelStroke.DOT, VowelStroke.I) to 0x3153,
            listOf(VowelStroke.DOT, VowelStroke.DOT, VowelStroke.I) to 0x3155,
            listOf(VowelStroke.DOT, VowelStroke.EU) to 0x3157,
            listOf(VowelStroke.DOT, VowelStroke.DOT, VowelStroke.EU) to 0x315b,
            listOf(VowelStroke.EU, VowelStroke.DOT) to 0x315c,
            listOf(VowelStroke.EU, VowelStroke.DOT, VowelStroke.DOT) to 0x3160,
            listOf(VowelStroke.EU) to 0x3161,
            listOf(VowelStroke.I, VowelStroke.DOT, VowelStroke.I) to 0x3150,
            listOf(VowelStroke.I, VowelStroke.DOT, VowelStroke.DOT, VowelStroke.I) to 0x3152,
            listOf(VowelStroke.DOT, VowelStroke.I, VowelStroke.I) to 0x3154,
            listOf(VowelStroke.DOT, VowelStroke.DOT, VowelStroke.I, VowelStroke.I) to 0x3156,
            listOf(VowelStroke.DOT, VowelStroke.EU, VowelStroke.I, VowelStroke.DOT) to 0x3158,
            listOf(VowelStroke.DOT, VowelStroke.EU, VowelStroke.I, VowelStroke.DOT, VowelStroke.I) to 0x3159,
            listOf(VowelStroke.DOT, VowelStroke.EU, VowelStroke.I) to 0x315a,
            listOf(VowelStroke.EU, VowelStroke.DOT, VowelStroke.DOT, VowelStroke.I) to 0x315d,
            listOf(VowelStroke.EU, VowelStroke.DOT, VowelStroke.DOT, VowelStroke.I, VowelStroke.I) to 0x315e,
            listOf(VowelStroke.EU, VowelStroke.DOT, VowelStroke.I) to 0x315f,
            listOf(VowelStroke.EU, VowelStroke.I) to 0x3162
        )

        private fun List<VowelStroke>.startsWith(prefix: List<VowelStroke>): Boolean {
            return size >= prefix.size && take(prefix.size) == prefix
        }
    }
}
