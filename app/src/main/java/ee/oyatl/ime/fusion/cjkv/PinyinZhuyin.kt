package ee.oyatl.ime.fusion.cjkv

import java.text.Normalizer

/**
 * Converts one tone-marked pinyin syllable (zhòng, lǜ, le) to zhuyin (ㄓㄨㄥˋ, ㄌㄩˋ, ˙ㄌㄜ).
 *
 * Unihan writes the neutral tone without a mark, so an unmarked syllable is
 * neutral, not first tone; the first tone has a macron and, as usual in zhuyin,
 * no tone mark. Returns null for anything that is not a standard syllable
 * (interjections such as m̄ or hm), so a row never shows a wrong spelling.
 */
internal object PinyinZhuyin {
    private val INITIALS = linkedMapOf(
        "zh" to "ㄓ", "ch" to "ㄔ", "sh" to "ㄕ",
        "b" to "ㄅ", "p" to "ㄆ", "m" to "ㄇ", "f" to "ㄈ",
        "d" to "ㄉ", "t" to "ㄊ", "n" to "ㄋ", "l" to "ㄌ",
        "g" to "ㄍ", "k" to "ㄎ", "h" to "ㄏ",
        "j" to "ㄐ", "q" to "ㄑ", "x" to "ㄒ",
        "r" to "ㄖ", "z" to "ㄗ", "c" to "ㄘ", "s" to "ㄙ",
    )

    private val FINALS = mapOf(
        "a" to "ㄚ", "o" to "ㄛ", "e" to "ㄜ", "ê" to "ㄝ",
        "ai" to "ㄞ", "ei" to "ㄟ", "ao" to "ㄠ", "ou" to "ㄡ",
        "an" to "ㄢ", "en" to "ㄣ", "ang" to "ㄤ", "eng" to "ㄥ", "er" to "ㄦ",
        "ong" to "ㄨㄥ",
        "i" to "ㄧ", "ia" to "ㄧㄚ", "io" to "ㄧㄛ", "ie" to "ㄧㄝ", "iai" to "ㄧㄞ",
        "iao" to "ㄧㄠ", "iu" to "ㄧㄡ", "ian" to "ㄧㄢ", "in" to "ㄧㄣ",
        "iang" to "ㄧㄤ", "ing" to "ㄧㄥ", "iong" to "ㄩㄥ",
        "u" to "ㄨ", "ua" to "ㄨㄚ", "uo" to "ㄨㄛ", "uai" to "ㄨㄞ", "ui" to "ㄨㄟ",
        "uan" to "ㄨㄢ", "un" to "ㄨㄣ", "uang" to "ㄨㄤ", "ueng" to "ㄨㄥ",
        "ü" to "ㄩ", "üe" to "ㄩㄝ", "üan" to "ㄩㄢ", "ün" to "ㄩㄣ",
    )

    /** y- and w- spell the medial when there is no initial (yan = ian, wei = uei). */
    private val ZERO_INITIAL = mapOf(
        "yi" to "i", "ya" to "ia", "yo" to "io", "ye" to "ie", "yai" to "iai", "yao" to "iao",
        "you" to "iu", "yan" to "ian", "yin" to "in", "yang" to "iang", "ying" to "ing",
        "yong" to "iong", "yu" to "ü", "yue" to "üe", "yuan" to "üan", "yun" to "ün",
        "wu" to "u", "wa" to "ua", "wo" to "uo", "wai" to "uai", "wei" to "ui",
        "wan" to "uan", "wen" to "un", "wang" to "uang", "weng" to "ueng",
    )

    private val TONES = mapOf('̄' to 1, '́' to 2, '̌' to 3, '̀' to 4)
    private val TONE_MARKS = mapOf(2 to "ˊ", 3 to "ˇ", 4 to "ˋ")

    fun convert(pinyin: String): String? {
        val (syllable, tone) = splitTone(pinyin.lowercase()) ?: return null
        val initial = INITIALS.keys.firstOrNull { syllable.startsWith(it) && syllable.length > it.length }
        var final = if(initial == null) ZERO_INITIAL[syllable] ?: syllable else syllable.removePrefix(initial)
        if(initial != null) {
            // zhi, chi, shi, ri, zi, ci, si have no vowel in zhuyin.
            if(final == "i" && initial in setOf("zh", "ch", "sh", "r", "z", "c", "s")) final = ""
            // After j, q and x, u is ü.
            if(initial in setOf("j", "q", "x") && final.startsWith("u")) final = "ü" + final.drop(1)
        }
        val finalZhuyin = if(final.isEmpty()) "" else FINALS[final] ?: return null
        val body = (initial?.let { INITIALS.getValue(it) } ?: "") + finalZhuyin
        if(body.isEmpty()) return null
        return when(tone) {
            0 -> "˙$body"
            else -> body + (TONE_MARKS[tone] ?: "")
        }
    }

    /** Strips the tone mark: returns the bare syllable (ü kept) and the tone, 0 for neutral. */
    private fun splitTone(pinyin: String): Pair<String, Int>? {
        var tone = 0
        val bare = StringBuilder()
        Normalizer.normalize(pinyin, Normalizer.Form.NFD).forEach { char ->
            when {
                char in TONES -> tone = TONES.getValue(char)
                char == '̈' -> {
                    // u + combining diaeresis is ü
                    if(bare.lastOrNull() != 'u') return null
                    bare.setCharAt(bare.length - 1, 'ü')
                }
                char == '̂' -> {
                    if(bare.lastOrNull() != 'e') return null
                    bare.setCharAt(bare.length - 1, 'ê')
                }
                char in 'a'..'z' -> bare.append(if(char == 'v') 'ü' else char)
                else -> return null
            }
        }
        if(bare.isEmpty()) return null
        return bare.toString() to tone
    }
}
