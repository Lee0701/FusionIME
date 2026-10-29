package ee.oyatl.ime.candidate

import ee.oyatl.ime.theme.KeyboardTheme

interface CandidateView {

    var keyboardTheme: KeyboardTheme?
    var listener: Listener?

    fun submitList(list: List<Candidate>)

    interface Candidate {
        val text: CharSequence
        override fun equals(other: Any?): Boolean
    }

    interface FocusableCandidate: Candidate {
        val focused: Boolean
    }

    interface VarLengthCandidate: Candidate {
        val inputLength: Int
    }

    interface ExtraCandidate: Candidate {
        val extra: CharSequence
    }

    interface Listener {
        fun onCandidateSelected(candidate: Candidate)

        /** Held down rather than tapped; modes that have nothing to offer ignore it. */
        fun onCandidateLongPressed(candidate: Candidate) = Unit
    }
}