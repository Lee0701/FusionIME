package ee.oyatl.ime.fusion.pinyin

private fun splitZeroInitials(
    ai: Char,
    an: Char,
    ang: Char,
    ao: Char,
    ei: Char,
    en: Char,
    eng: Char,
    er: Char,
    ou: Char
) = mapOf(
    "aa" to "a",
    "a$ai" to "ai",
    "a$an" to "an",
    "a$ang" to "ang",
    "a$ao" to "ao",
    "ee" to "e",
    "e$ei" to "ei",
    "e$en" to "en",
    "e$eng" to "eng",
    "e$er" to "er",
    "oo" to "o",
    "o$ou" to "ou"
)

private fun prefixedZeroInitials(
    ai: Char,
    an: Char,
    ang: Char,
    ao: Char,
    ei: Char,
    en: Char,
    eng: Char,
    er: Char,
    ou: Char
) = mapOf(
    "oa" to "a",
    "o$ai" to "ai",
    "o$an" to "an",
    "o$ang" to "ang",
    "o$ao" to "ao",
    "oe" to "e",
    "o$ei" to "ei",
    "o$en" to "en",
    "o$eng" to "eng",
    "o$er" to "er",
    "oo" to "o",
    "o$ou" to "ou"
)

private fun splitAndPrefixedZeroInitials(
    ai: Char,
    an: Char,
    ang: Char,
    ao: Char,
    ei: Char,
    en: Char,
    eng: Char,
    er: Char,
    ou: Char
) = splitZeroInitials(ai, an, ang, ao, ei, en, eng, er, ou) +
    prefixedZeroInitials(ai, an, ang, ao, ei, en, eng, er, ou)

enum class DoublePinyinScheme(
    private val initials: Map<Char, String>,
    private val zeroInitials: Map<String, String>,
    private val extraFinalKeys: Set<Char> = emptySet()
) {
    Ziranma(
        initials = mapOf('i' to "ch", 'u' to "sh", 'v' to "zh"),
        zeroInitials = splitZeroInitials('l', 'j', 'h', 'k', 'z', 'f', 'g', 'r', 'b')
    ),
    Microsoft(
        initials = mapOf('i' to "ch", 'u' to "sh", 'v' to "zh"),
        zeroInitials = splitAndPrefixedZeroInitials('l', 'j', 'h', 'k', 'z', 'f', 'g', 'r', 'b'),
        extraFinalKeys = setOf(';')
    ),
    SmartABC(
        initials = mapOf('a' to "zh", 'e' to "ch", 'v' to "sh"),
        zeroInitials = prefixedZeroInitials('l', 'j', 'h', 'k', 'q', 'f', 'g', 'r', 'b')
    ),
    Jiajia(
        initials = mapOf('u' to "ch", 'i' to "sh", 'v' to "zh"),
        zeroInitials = splitAndPrefixedZeroInitials('s', 'f', 'g', 'd', 'w', 'r', 't', 'q', 'p')
    ),
    Xiaohe(
        initials = mapOf('i' to "ch", 'u' to "sh", 'v' to "zh"),
        zeroInitials = splitZeroInitials('d', 'j', 'h', 'c', 'w', 'f', 'g', 'r', 'z')
    ),
    Sogou(
        initials = mapOf('i' to "ch", 'u' to "sh", 'v' to "zh"),
        zeroInitials = splitAndPrefixedZeroInitials('l', 'j', 'h', 'k', 'z', 'f', 'g', 'r', 'b'),
        extraFinalKeys = setOf(';')
    ),
    Ziguang(
        initials = mapOf('u' to "zh", 'a' to "ch", 'i' to "sh"),
        zeroInitials = prefixedZeroInitials('p', 'r', 's', 'q', 'k', 'w', 't', 'j', 'z'),
        extraFinalKeys = setOf(';')
    );

    fun expandInitial(initial: Char): String {
        require(initial in 'a'..'z')
        return initials[initial] ?: initial.toString()
    }

    fun acceptsFinal(key: Char): Boolean = key in 'a'..'z' || key in extraFinalKeys

    fun decode(first: Char, second: Char): String {
        require(first in 'a'..'z' && acceptsFinal(second))

        zeroInitials["$first$second"]?.let { return it }

        val initial = expandInitial(first)
        val final = decodeFinal(first, second)
        val normalizedFinal = if(initial.length == 1 && initial[0] in "jqxy" && final.startsWith('v')) {
            "u" + final.drop(1)
        } else final
        return initial + normalizedFinal
    }

    private fun decodeFinal(first: Char, second: Char): String = when(this) {
        Ziranma -> when(second) {
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

        Microsoft -> decodeMicrosoftFinal(first, second, includeBiang = false)
        Sogou -> decodeMicrosoftFinal(first, second, includeBiang = true)

        SmartABC -> when(second) {
            'q' -> "ei"
            'n' -> "un"
            'g' -> "eng"
            'w' -> "ian"
            'r' -> if(first in "dtnljqx") "iu" else "r"
            't' -> if(first in "nljqx") "iang" else "uang"
            'y' -> "ing"
            'o' -> if(first in "dtnlgkhaevrzcs") "uo" else "o"
            'p' -> "uan"
            's' -> if(first in "jqx") "iong" else "ong"
            'd' -> if(first in "gkhaevrzcs") "ua" else "ia"
            'f' -> "en"
            'h' -> "ang"
            'j' -> "an"
            'k' -> "ao"
            'l' -> "ai"
            'z' -> "iao"
            'x' -> "ie"
            'c' -> if(first in "gkhaev") "uai" else "in"
            'b' -> "ou"
            'm' -> when {
                first in "nl" -> "ve"
                first in "jqxy" -> "ue"
                else -> "ui"
            }
            else -> second.toString()
        }

        Jiajia -> when(second) {
            'l' -> if(first in "bpmnljqxy") "in" else "l"
            't' -> "eng"
            'n' -> "iu"
            'b' -> if(first in "gkhvuirzcs") "ua" else "ia"
            'c' -> "uan"
            'v' -> if(first in "dtgkhvuirzcs") "ui" else "v"
            'x' -> if(first in "gkhvuirzcs") "uai" else "ve"
            'o' -> if(first in "dtnlgkhvuirzcs") "uo" else "o"
            'z' -> "un"
            'y' -> if(first in "jqx") "iong" else "ong"
            'h' -> if(first in "jqxnlb") "iang" else "uang"
            'r' -> "en"
            'g' -> "ang"
            'f' -> "an"
            'd' -> "ao"
            's' -> "ai"
            'w' -> "ei"
            'm' -> "ie"
            'k' -> "iao"
            'p' -> "ou"
            'j' -> "ian"
            'q' -> "ing"
            else -> second.toString()
        }

        Xiaohe -> when(second) {
            'n' -> if(first in "bpmfdtnljqx") "iao" else "n"
            'g' -> "eng"
            'q' -> "iu"
            'w' -> "ei"
            'r' -> "uan"
            't' -> "ve"
            'y' -> "un"
            'o' -> if(first in "dtnlgkhvuirzcs") "uo" else "o"
            'p' -> "ie"
            's' -> if(first in "jqx") "iong" else "ong"
            'd' -> "ai"
            'f' -> "en"
            'h' -> "ang"
            'j' -> "an"
            'k' -> if(first in "gkhvuirzcs") "uai" else "ing"
            'l' -> if(first in "jqxnl") "iang" else "uang"
            'z' -> "ou"
            'x' -> if(first in "gkhvuirzcs") "ua" else "ia"
            'c' -> "ao"
            'v' -> if(first in "dtgkhvuirzcs") "ui" else "v"
            'b' -> "in"
            'm' -> "ian"
            else -> second.toString()
        }

        Ziguang -> when(second) {
            'n' -> when {
                first in "jqxy" -> "ue"
                first in "nl" -> "ve"
                first in "dtgkhrzcsuai" -> "ui"
                else -> "n"
            }
            'g' -> when {
                first in "nljqxb" -> "iang"
                first in "gkhuai" -> "uang"
                else -> "g"
            }
            'w' -> "en"
            'r' -> "an"
            't' -> "eng"
            'y' -> if(first in "jqxylmnbp") "in" else "uai"
            'o' -> if(first in "dtnlgkhrzcsuai") "uo" else "o"
            'q' -> "ao"
            'p' -> "ai"
            's' -> "ang"
            'd' -> "ie"
            'f' -> "ian"
            'h' -> if(first in "jqx") "iong" else "ong"
            'j' -> if(first in "dnlmjqx") "iu" else "j"
            'k' -> "ei"
            'l' -> "uan"
            'z' -> "ou"
            'x' -> when {
                first in "jqxl" -> "ia"
                first in "gkhuai" -> "ua"
                else -> "x"
            }
            ';' -> "ing"
            'b' -> "iao"
            'm' -> "un"
            else -> second.toString()
        }
    }

    private fun decodeMicrosoftFinal(first: Char, second: Char, includeBiang: Boolean): String = when(second) {
        'n' -> if(first in "bpmnljqxy") "in" else "n"
        'g' -> "eng"
        'q' -> "iu"
        'w' -> if(first in "gkhvuirzcs") "ua" else "ia"
        'r' -> "uan"
        'v' -> if(first in "dtgkhvuirzcs") "ui" else "ve"
        't' -> "ve"
        'y' -> if(first in "gkhvuirzcs") "uai" else "v"
        'o' -> if(first in "dtnlgkhvuirzcs") "uo" else "o"
        'p' -> "un"
        's' -> if(first in "jqx") "iong" else "ong"
        'd' -> if(first in if(includeBiang) "jqxnlb" else "jqxnl") "iang" else "uang"
        'f' -> "en"
        'h' -> "ang"
        'j' -> "an"
        'k' -> "ao"
        'l' -> "ai"
        'z' -> "ei"
        'x' -> "ie"
        'c' -> "iao"
        'b' -> "ou"
        'm' -> "ian"
        ';' -> "ing"
        else -> second.toString()
    }
}
