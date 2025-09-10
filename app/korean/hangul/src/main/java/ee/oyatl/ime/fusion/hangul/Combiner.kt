package ee.oyatl.ime.fusion.hangul

interface Combiner {

    fun combine(state: State, input: Int): Result

    interface State {
        val previous: State?
        val combined: CharSequence
    }

    data class Result(
        val textToCommit: List<CharSequence>,
        val newState: State
    )
}