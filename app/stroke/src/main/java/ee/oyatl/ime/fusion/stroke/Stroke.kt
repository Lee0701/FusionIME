package ee.oyatl.ime.fusion.stroke

/**
 * The five stroke types, as digits in stroke sequences and as key labels.
 */
object Stroke {
    /** Matches any single stroke. */
    const val WILDCARD = '*'

    // 橫 豎 撇 點 折
    private const val LABELS = "一丨丿丶乙"
    private const val WILDCARD_LABEL = '＊'

    /** The stroke digit or [WILDCARD] for a key label, or null for other characters. */
    fun ofLabel(codePoint: Int): Char? {
        if(codePoint == WILDCARD_LABEL.code) return WILDCARD
        val index = LABELS.indexOf(codePoint.toChar())
        return if(codePoint <= 0xffff && index >= 0) '1' + index else null
    }

    fun label(stroke: Char): Char =
        if(stroke == WILDCARD) WILDCARD_LABEL else LABELS[stroke - '1']

    val keyLabels: List<Char> = LABELS.toList() + WILDCARD_LABEL
}
