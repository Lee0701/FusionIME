package ee.oyatl.ime.fusion.hangul

import ee.oyatl.ime.fusion.layout.Hangul2Set
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CheonjiinComposerTest {
    private val composer = CheonjiinComposer(
        HangulCombiner(Hangul2Set.COMB_KS, correctOrders = false)
    )

    @Test
    fun composesAllModernVowelsFromCanonicalStrokeSequences() {
        val cases = mapOf(
            "ㅣ" to intArrayOf(I),
            "ㅏ" to intArrayOf(I, DOT),
            "ㅑ" to intArrayOf(I, DOT, DOT),
            "ㅓ" to intArrayOf(DOT, I),
            "ㅕ" to intArrayOf(DOT, DOT, I),
            "ㅗ" to intArrayOf(DOT, EU),
            "ㅛ" to intArrayOf(DOT, DOT, EU),
            "ㅜ" to intArrayOf(EU, DOT),
            "ㅠ" to intArrayOf(EU, DOT, DOT),
            "ㅡ" to intArrayOf(EU),
            "ㅐ" to intArrayOf(I, DOT, I),
            "ㅒ" to intArrayOf(I, DOT, DOT, I),
            "ㅔ" to intArrayOf(DOT, I, I),
            "ㅖ" to intArrayOf(DOT, DOT, I, I),
            "ㅘ" to intArrayOf(DOT, EU, I, DOT),
            "ㅙ" to intArrayOf(DOT, EU, I, DOT, I),
            "ㅚ" to intArrayOf(DOT, EU, I),
            "ㅝ" to intArrayOf(EU, DOT, DOT, I),
            "ㅞ" to intArrayOf(EU, DOT, DOT, I, I),
            "ㅟ" to intArrayOf(EU, DOT, I),
            "ㅢ" to intArrayOf(EU, I)
        )

        cases.forEach { (expected, keys) ->
            assertEquals(expected, compose(*keys).combined.toString())
        }
    }

    @Test
    fun cyclesConsonantsAssignedToTheSameKey() {
        val cases = mapOf(
            "ㄱ" to intArrayOf(GIYEOK),
            "ㅋ" to intArrayOf(GIYEOK, GIYEOK),
            "ㄲ" to intArrayOf(GIYEOK, GIYEOK, GIYEOK),
            "ㄴ" to intArrayOf(NIEUN),
            "ㄹ" to intArrayOf(NIEUN, NIEUN),
            "ㄷ" to intArrayOf(DIGEUT),
            "ㅌ" to intArrayOf(DIGEUT, DIGEUT),
            "ㄸ" to intArrayOf(DIGEUT, DIGEUT, DIGEUT),
            "ㅂ" to intArrayOf(BIEUP),
            "ㅍ" to intArrayOf(BIEUP, BIEUP),
            "ㅃ" to intArrayOf(BIEUP, BIEUP, BIEUP),
            "ㅅ" to intArrayOf(SIOT),
            "ㅎ" to intArrayOf(SIOT, SIOT),
            "ㅆ" to intArrayOf(SIOT, SIOT, SIOT),
            "ㅈ" to intArrayOf(JIEUT),
            "ㅊ" to intArrayOf(JIEUT, JIEUT),
            "ㅉ" to intArrayOf(JIEUT, JIEUT, JIEUT),
            "ㅇ" to intArrayOf(IEUNG),
            "ㅁ" to intArrayOf(IEUNG, IEUNG)
        )

        cases.forEach { (expected, keys) ->
            assertEquals(expected, compose(*keys).combined.toString())
        }
    }

    @Test
    fun flickSelectsConsonantsWithoutAddingAnotherJamo() {
        val rightFromInitial = flickConsonant(CheonjiinComposer.State.Initial, GIYEOK, 1)
        val tapped = press(CheonjiinComposer.State.Initial, GIYEOK)
        val rightAfterTap = flickConsonant(tapped, GIYEOK, 1)
        val downFromInitial = flickConsonant(CheonjiinComposer.State.Initial, GIYEOK, 2)

        assertEquals("ㅋ", rightFromInitial.combined.toString())
        assertEquals("ㅋ", rightAfterTap.combined.toString())
        assertEquals("ㄲ", downFromInitial.combined.toString())
        assertEquals(1, rightAfterTap.keyHistory.size)
    }

    @Test
    fun flickSelectsCompletedVowelsAndReplacesTheProvisionalTap() {
        val a = flickVowel(CheonjiinComposer.State.Initial, I, 0x314f)
        val ga = flickVowel(press(CheonjiinComposer.State.Initial, GIYEOK), I, 0x314f)

        assertEquals("ㅏ", a.combined.toString())
        assertEquals("가", ga.combined.toString())
        assertEquals(1, a.keyHistory.size)
    }

    @Test
    fun anUnassignedFlickRestoresTheStateBeforeTouchDown() {
        val before = press(CheonjiinComposer.State.Initial, NIEUN)
        val provisional = press(before, GIYEOK)
        val cancelled = composer.cancelFlick(provisional, GIYEOK).newState as CheonjiinComposer.State

        assertEquals(before.combined.toString(), cancelled.combined.toString())
        assertEquals(before.keyHistory, cancelled.keyHistory)
    }

    @Test
    fun composesSyllablesThroughTheExistingHangulCombiner() {
        assertEquals(
            "한글",
            compose(SIOT, SIOT, I, DOT, NIEUN, GIYEOK, EU, NIEUN, NIEUN).combined.toString()
        )
        assertEquals(
            "가나",
            compose(GIYEOK, I, DOT, NIEUN, I, DOT).combined.toString()
        )
    }

    @Test
    fun confirmSeparatesConsecutiveConsonantsOnTheSameKey() {
        assertEquals(
            "앗사",
            compose(IEUNG, I, DOT, SIOT, CheonjiinComposer.CONFIRM_INPUT, SIOT, I, DOT)
                .combined.toString()
        )
    }

    @Test
    fun onlyAConsonantTailNeedsRightArrowConfirmation() {
        val consonant = compose(SIOT)
        val vowel = compose(I, DOT)
        val confirmed = press(consonant, CheonjiinComposer.CONFIRM_INPUT)

        assertEquals(true, composer.canConfirmConsonantCycle(consonant))
        assertEquals(false, composer.canConfirmConsonantCycle(vowel))
        assertEquals(false, composer.canConfirmConsonantCycle(confirmed))
        assertEquals(false, composer.canConfirmConsonantCycle(HangulCombiner.State.Initial))
    }

    @Test
    fun backspaceReturnsOnePhysicalVowelStrokeAtATime() {
        val iState = press(CheonjiinComposer.State.Initial, I)
        val aState = press(iState, DOT)

        assertEquals("ㅏ", aState.combined.toString())
        assertEquals("ㅣ", aState.previous?.combined.toString())
    }

    @Test
    fun backspaceRemovesAConsonantCandidateWithoutReplayingItsCycle() {
        val cycled = compose(GIYEOK, GIYEOK, GIYEOK)
        val flicked = flickConsonant(CheonjiinComposer.State.Initial, GIYEOK, 1)

        assertEquals("ㄲ", cycled.combined.toString())
        assertEquals("", cycled.previous?.combined.toString())
        assertEquals("ㅋ", flicked.combined.toString())
        assertEquals("", flicked.previous?.combined.toString())
    }

    @Test
    fun araeaRemainsTransientAndIsNeverEmitted() {
        val state = compose(DOT, DOT)

        assertEquals("", state.combined.toString())
        assertFalse(state.combined.contains('\u318d'))
    }

    @Test
    fun aFreshCombinerStateStartsANewPhysicalKeyHistory() {
        val cycled = compose(GIYEOK, GIYEOK)
        val fresh = composer.combine(HangulCombiner.State.Initial, GIYEOK)
            .newState as CheonjiinComposer.State

        assertEquals("ㅋ", cycled.combined.toString())
        assertEquals("ㄱ", fresh.combined.toString())
        assertEquals(GIYEOK, fresh.activeConsonantKey)
        assertEquals(0, fresh.activeConsonantIndex)
        assertEquals(1, fresh.keyHistory.size)
    }

    private fun compose(vararg keys: Int): CheonjiinComposer.State {
        return keys.fold(CheonjiinComposer.State.Initial, ::press)
    }

    private fun press(state: CheonjiinComposer.State, key: Int): CheonjiinComposer.State {
        return composer.combine(state, key).newState as CheonjiinComposer.State
    }

    private fun flickConsonant(
        state: CheonjiinComposer.State,
        key: Int,
        targetIndex: Int
    ): CheonjiinComposer.State {
        val provisional = press(state, key)
        return composer.selectConsonant(provisional, key, targetIndex)
            .newState as CheonjiinComposer.State
    }

    private fun flickVowel(
        state: CheonjiinComposer.State,
        key: Int,
        target: Int
    ): CheonjiinComposer.State {
        val provisional = press(state, key)
        return composer.selectCompletedVowel(provisional, key, target)
            .newState as CheonjiinComposer.State
    }

    companion object {
        const val I = 0x3163
        const val DOT = 0x318d
        const val EU = 0x3161
        const val GIYEOK = 0x3131
        const val NIEUN = 0x3134
        const val DIGEUT = 0x3137
        const val BIEUP = 0x3142
        const val SIOT = 0x3145
        const val JIEUT = 0x3148
        const val IEUNG = 0x3147
    }
}
