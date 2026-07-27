package ee.oyatl.ime.fusion.pinyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DoublePinyinSchemeTest {
    @Test
    fun expandsSchemeSpecificInitials() {
        assertEquals("ch", DoublePinyinScheme.Ziranma.expandInitial('i'))
        assertEquals("sh", DoublePinyinScheme.Microsoft.expandInitial('u'))
        assertEquals("zh", DoublePinyinScheme.SmartABC.expandInitial('a'))
        assertEquals("ch", DoublePinyinScheme.Jiajia.expandInitial('u'))
        assertEquals("zh", DoublePinyinScheme.Xiaohe.expandInitial('v'))
        assertEquals("sh", DoublePinyinScheme.Sogou.expandInitial('u'))
        assertEquals("ch", DoublePinyinScheme.Ziguang.expandInitial('a'))
    }

    @Test
    fun decodesZiranma() {
        assertCases(DoublePinyinScheme.Ziranma, mapOf(
            "bc" to "biao",
            "jm" to "jian",
            "jd" to "jiang",
            "qs" to "qiong",
            "jt" to "jue",
            "ud" to "shuang",
            "vd" to "zhuang"
        ))
    }

    @Test
    fun decodesMicrosoft() {
        assertCases(DoublePinyinScheme.Microsoft, mapOf(
            "bc" to "biao",
            "jm" to "jian",
            "j;" to "jing",
            "qs" to "qiong",
            "jt" to "jue",
            "ud" to "shuang"
        ))
    }

    @Test
    fun decodesSmartABC() {
        assertCases(DoublePinyinScheme.SmartABC, mapOf(
            "at" to "zhuang",
            "vh" to "shang",
            "jw" to "jian",
            "jy" to "jing",
            "jm" to "jue",
            "qs" to "qiong"
        ))
    }

    @Test
    fun decodesJiajia() {
        assertCases(DoublePinyinScheme.Jiajia, mapOf(
            "ih" to "shuang",
            "vg" to "zhang",
            "bj" to "bian",
            "yq" to "ying",
            "jy" to "jiong",
            "jx" to "jue"
        ))
    }

    @Test
    fun decodesXiaohe() {
        assertCases(DoublePinyinScheme.Xiaohe, mapOf(
            "ul" to "shuang",
            "jm" to "jian",
            "jk" to "jing",
            "qs" to "qiong",
            "jt" to "jue",
            "bn" to "biao"
        ))
    }

    @Test
    fun decodesSogou() {
        assertCases(DoublePinyinScheme.Sogou, mapOf(
            "ud" to "shuang",
            "jm" to "jian",
            "j;" to "jing",
            "bd" to "biang",
            "jt" to "jue"
        ))
    }

    @Test
    fun decodesZiguang() {
        assertCases(DoublePinyinScheme.Ziguang, mapOf(
            "ug" to "zhuang",
            "ag" to "chuang",
            "is" to "shang",
            "jf" to "jian",
            "j;" to "jing",
            "rn" to "rui",
            "bb" to "biao"
        ))
    }

    @Test
    fun decodesZeroInitialSyllables() {
        val cases = mapOf(
            DoublePinyinScheme.Ziranma to mapOf("aa" to "a", "al" to "ai", "er" to "er", "ob" to "ou"),
            DoublePinyinScheme.Microsoft to mapOf(
                "aa" to "a", "al" to "ai", "oa" to "a", "ol" to "ai", "eg" to "eng", "ob" to "ou"
            ),
            DoublePinyinScheme.SmartABC to mapOf("oa" to "a", "ol" to "ai", "or" to "er", "ob" to "ou"),
            DoublePinyinScheme.Jiajia to mapOf(
                "aa" to "a", "as" to "ai", "oa" to "a", "os" to "ai", "eq" to "er", "op" to "ou"
            ),
            DoublePinyinScheme.Xiaohe to mapOf("aa" to "a", "ad" to "ai", "er" to "er", "oz" to "ou"),
            DoublePinyinScheme.Sogou to mapOf(
                "aa" to "a", "al" to "ai", "oa" to "a", "ol" to "ai", "eg" to "eng", "ob" to "ou"
            ),
            DoublePinyinScheme.Ziguang to mapOf("oa" to "a", "op" to "ai", "oj" to "er", "oz" to "ou")
        )

        cases.forEach { (scheme, schemeCases) -> assertCases(scheme, schemeCases) }
    }

    @Test
    fun onlyAcceptsSchemeSpecificPunctuationFinals() {
        assertThrows(IllegalArgumentException::class.java) {
            DoublePinyinScheme.Ziranma.decode('j', ';')
        }
        assertEquals("jing", DoublePinyinScheme.Microsoft.decode('j', ';'))
    }

    private fun assertCases(scheme: DoublePinyinScheme, cases: Map<String, String>) {
        cases.forEach { (keys, expected) ->
            assertEquals("${scheme.name}: $keys", expected, scheme.decode(keys[0], keys[1]))
        }
    }
}
