package ee.oyatl.ime.fusion.cjkv

import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R

/**
 * Stands in for [candidate] after a long press, next to the delete button, and
 * opens it in the dictionary when tapped. Modes that cannot delete candidates
 * open the dictionary straight from the long press instead.
 */
data class CjkvLookupCandidate(
    val candidate: CandidateView.Candidate,
): CandidateView.IconCandidate {
    override val text: CharSequence = candidate.text
    override val icon: Int = R.drawable.info_24
}

/** True when [text] has a Han character the dictionary can show. */
internal fun containsHan(text: CharSequence): Boolean {
    return text.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN }
}
