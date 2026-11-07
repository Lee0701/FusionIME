package ee.oyatl.ime.fusion.cjkv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinyinZhuyinTest {
    @Test
    fun convertsTonesIncludingNeutral() {
        assertEquals("ㄓㄨㄥˋ", PinyinZhuyin.convert("zhòng"))
        assertEquals("ㄔㄨㄥˊ", PinyinZhuyin.convert("chóng"))
        assertEquals("ㄕㄢ", PinyinZhuyin.convert("shān"))
        assertEquals("ㄏㄠˇ", PinyinZhuyin.convert("hǎo"))
        assertEquals("˙ㄌㄜ", PinyinZhuyin.convert("le"))
    }

    @Test
    fun handlesSpellingRules() {
        assertEquals("ㄓ", PinyinZhuyin.convert("zhī"))
        assertEquals("ㄙˋ", PinyinZhuyin.convert("sì"))
        assertEquals("ㄐㄩˋ", PinyinZhuyin.convert("jù"))
        assertEquals("ㄒㄩㄝˊ", PinyinZhuyin.convert("xué"))
        assertEquals("ㄌㄩˋ", PinyinZhuyin.convert("lǜ"))
        assertEquals("ㄋㄩㄝˋ", PinyinZhuyin.convert("nüè"))
        assertEquals("ㄧ", PinyinZhuyin.convert("yī"))
        assertEquals("ㄩㄢˊ", PinyinZhuyin.convert("yuán"))
        assertEquals("ㄨㄟˋ", PinyinZhuyin.convert("wèi"))
        assertEquals("ㄧㄡˇ", PinyinZhuyin.convert("yǒu"))
        assertEquals("ㄍㄨㄟˋ", PinyinZhuyin.convert("guì"))
        assertEquals("ㄌㄧㄡˊ", PinyinZhuyin.convert("liú"))
        assertEquals("ㄏㄨㄣˋ", PinyinZhuyin.convert("hùn"))
        assertEquals("ㄦˊ", PinyinZhuyin.convert("ér"))
        assertEquals("ㄒㄧㄥˊ", PinyinZhuyin.convert("xíng"))
        assertEquals("ㄐㄩㄥˇ", PinyinZhuyin.convert("jiǒng"))
    }

    @Test
    fun rejectsNonStandardSyllables() {
        assertNull(PinyinZhuyin.convert("m̄"))
        assertNull(PinyinZhuyin.convert("hm"))
        assertNull(PinyinZhuyin.convert("ňg"))
    }
}
