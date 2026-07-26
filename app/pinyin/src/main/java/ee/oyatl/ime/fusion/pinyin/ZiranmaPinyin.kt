package ee.oyatl.ime.fusion.pinyin

object ZiranmaPinyin {
    private val zeroInitialFinals = mapOf(
        'b' to "ou",
        'f' to "en",
        'g' to "eng",
        'h' to "ang",
        'j' to "an",
        'k' to "ao",
        'l' to "ai",
        'r' to "er",
        'z' to "ei"
    )

    fun expandInitial(initial: Char): String = when(initial) {
        'i' -> "ch"
        'u' -> "sh"
        'v' -> "zh"
        else -> initial.toString()
    }

    fun decode(first: Char, second: Char): String {
        require(first in 'a'..'z' && second in 'a'..'z')

        if(first in "aoe") {
            if(first == second) return first.toString()
            return zeroInitialFinals[second] ?: "$first$second"
        }

        val final = when(second) {
            'b' -> "ou"
            'c' -> "iao"
            'd' -> if(first in "jqxnl") "iang" else "uang"
            'f' -> "en"
            'g' -> "eng"
            'h' -> "ang"
            'j' -> "an"
            'k' -> "ao"
            'l' -> "ai"
            'm' -> "ian"
            'n' -> if(first in "bpmnljqxy") "in" else "n"
            'o' -> if(first in "dtnlgkhvuirzcs") "uo" else "o"
            'p' -> "un"
            'q' -> "iu"
            'r' -> "uan"
            's' -> if(first in "jqx") "iong" else "ong"
            't' -> "ve"
            'v' -> if(first in "dtgkhvuirzcs") "ui" else "v"
            'w' -> if(first in "gkhvuirzcs") "ua" else "ia"
            'x' -> "ie"
            'y' -> if(first in "gkhvuirzcs") "uai" else "ing"
            'z' -> "ei"
            else -> second.toString()
        }
        val normalizedFinal = if(first in "jqxy" && final.startsWith('v')) {
            "u" + final.drop(1)
        } else final
        return expandInitial(first) + normalizedFinal
    }
}
