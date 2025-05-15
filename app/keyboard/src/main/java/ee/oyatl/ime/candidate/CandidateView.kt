package ee.oyatl.ime.candidate

interface CandidateView {

    var listener: CandidateView.Listener?

    fun submitList(list: List<Candidate>)

    interface Candidate {
        val text: CharSequence
        override fun equals(other: Any?): Boolean
    }

    interface Listener {
        fun onCandidateSelected(candidate: Candidate)
    }
}