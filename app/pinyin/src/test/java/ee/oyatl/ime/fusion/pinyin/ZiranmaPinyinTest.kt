package ee.oyatl.ime.fusion.pinyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ZiranmaPinyinTest {
    @Test
    fun expandsMultiletterInitials() {
        assertEquals("ch", ZiranmaPinyin.expandInitial('i'))
        assertEquals("sh", ZiranmaPinyin.expandInitial('u'))
        assertEquals("zh", ZiranmaPinyin.expandInitial('v'))
        assertEquals("b", ZiranmaPinyin.expandInitial('b'))
    }

    @Test
    fun decodesRepresentativeSyllables() {
        val cases = mapOf(
            "bo" to "bo",
            "bc" to "biao",
            "pn" to "pin",
            "jm" to "jian",
            "jd" to "jiang",
            "qs" to "qiong",
            "jt" to "jue",
            "lt" to "lve",
            "dv" to "dui",
            "gw" to "gua",
            "gy" to "guai",
            "ib" to "chou",
            "is" to "chong",
            "ud" to "shuang",
            "vd" to "zhuang"
        )

        cases.forEach { (keys, expected) ->
            assertEquals(keys, expected, ZiranmaPinyin.decode(keys[0], keys[1]))
        }
    }

    @Test
    fun decodesZeroInitialSyllables() {
        val cases = mapOf(
            "aa" to "a",
            "ee" to "e",
            "oo" to "o",
            "al" to "ai",
            "ak" to "ao",
            "aj" to "an",
            "ah" to "ang",
            "ef" to "en",
            "eg" to "eng",
            "er" to "er",
            "ez" to "ei",
            "ob" to "ou"
        )

        cases.forEach { (keys, expected) ->
            assertEquals(keys, expected, ZiranmaPinyin.decode(keys[0], keys[1]))
        }
    }

    @Test
    fun rejectsNonAlphabeticKeys() {
        assertThrows(IllegalArgumentException::class.java) {
            ZiranmaPinyin.decode('a', '1')
        }
    }
}
