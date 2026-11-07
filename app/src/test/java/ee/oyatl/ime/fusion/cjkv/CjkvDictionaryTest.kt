package ee.oyatl.ime.fusion.cjkv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class CjkvDictionaryTest {

    @Test
    fun cangjieCodesUseCangjieRootsForDisplay() {
        assertEquals("水廿中人", CjkvCangjieFormatter.format("ETLO"))
        assertEquals("山 · 難卜重", CjkvCangjieFormatter.format("U · XYZ"))
    }
    @Test
    fun lookupDecodesReadingsAndVariants() {
        val dictionary = CjkvDictionary.fromBytes(
            encode(
                0x6F22 to mapOf(
                    0 to "カン",
                    1 to "から",
                    2 to "hàn",
                    3 to "hon3",
                    4 to "한",
                    5 to "hán",
                    6 to "汉",
                    11 to "G H J K T V",
                    12 to "ETLO",
                    13 to "3413.4",
                    14 to "한수 한",
                    15 to "85.11",
                    16 to "14",
                    17 to "*xɑ̀n",
                    18 to "呼旰",
                    19 to "𣶔",
                    20 to "𤁉",
                    21 to "漢",
                    22 to "Chinese people; Chinese language",
                ),
            )
        )

        assertEquals(
            CjkvDictionaryEntry(
                codePoint = 0x6F22,
                japaneseOn = "カン",
                japaneseKun = "から",
                mandarin = "hàn",
                cantonese = "hon3",
                korean = "한",
                vietnamese = "hán",
                simplifiedVariants = "汉",
                sourceRegions = "G H J K T V",
                cangjie = "ETLO",
                fourCorner = "3413.4",
                koreanHunEum = "한수 한",
                radicalStrokes = "85.11",
                totalStrokes = "14",
                middleChinese = "*xɑ̀n",
                fanqie = "呼旰",
                semanticVariants = "𣶔",
                specializedSemanticVariants = "𤁉",
                zVariants = "漢",
                definition = "Chinese people; Chinese language",
            ),
            dictionary.lookup(0x6F22),
        )
    }

    @Test
    fun lookupSupportsSupplementaryCodePoints() {
        val dictionary = CjkvDictionary.fromBytes(encode(0x20BB7 to mapOf(0 to "キチ キツ")))
        assertEquals("キチ キツ", dictionary.lookup(0x20BB7)?.japaneseOn)
        assertNull(dictionary.lookup(0x20BB8))
    }

    @Test
    fun lookupDecodesCompatibilityVariant() {
        val dictionary = CjkvDictionary.fromBytes(
            encode(0xF900 to mapOf(10 to "豈", 11 to "K"))
        )
        assertEquals("豈", dictionary.lookup(0xF900)?.compatibilityVariant)
        assertEquals("K", dictionary.lookup(0xF900)?.sourceRegions)
    }

    @Test
    fun lookupResolvedFillsMissingReadingsFromExplicitVariant() {
        val dictionary = CjkvDictionary.fromBytes(
            encode(
                0x6C49 to mapOf(2 to "hàn", 7 to "漢"),
                0x6F22 to mapOf(0 to "カン", 2 to "hàn", 6 to "汉"),
            )
        )

        val resolved = requireNotNull(dictionary.lookupResolved(0x6C49))
        assertEquals("hàn", resolved.reading(CjkvReadingField.Mandarin).value)
        assertNull(resolved.reading(CjkvReadingField.Mandarin).sourceCodePoint)
        assertEquals("カン", resolved.reading(CjkvReadingField.JapaneseOn).value)
        assertEquals(0x6F22, resolved.reading(CjkvReadingField.JapaneseOn).sourceCodePoint)
    }

    @Test
    fun formsAreFollowedAcrossJapaneseAndChinese() {
        // Unihan: 譲 → 讓 (旧字体), 讓 → 让 (簡体字) and 譲 (新字体), 让 → 讓 (繁体字)
        val dictionary = CjkvDictionary.fromBytes(
            encode(
                0x8B72 to mapOf(9 to "讓"),
                0x8B93 to mapOf(6 to "让", 8 to "譲"),
                0x8BA9 to mapOf(7 to "讓"),
            )
        )

        val jo = requireNotNull(dictionary.lookupResolved(0x8B72))
        assertEquals(listOf(CjkvFormVariant("让", 0x8B93)), jo.forms(CjkvFormField.Simplified))
        assertEquals(listOf(CjkvFormVariant("讓")), jo.forms(CjkvFormField.Traditional))
        assertEquals(listOf(CjkvFormVariant("讓")), jo.forms(CjkvFormField.JapaneseOld))
        assertEquals(emptyList<CjkvFormVariant>(), jo.forms(CjkvFormField.JapaneseNew))

        val rang = requireNotNull(dictionary.lookupResolved(0x8BA9))
        assertEquals(listOf(CjkvFormVariant("讓")), rang.forms(CjkvFormField.Traditional))
        assertEquals(listOf(CjkvFormVariant("譲", 0x8B93)), rang.forms(CjkvFormField.JapaneseNew))
        assertEquals(listOf(CjkvFormVariant("讓")), rang.forms(CjkvFormField.JapaneseOld))
    }

    @Test
    fun aCharacterListedAsItsOwnFormIsNotARole() {
        // 广 lists itself as simplified and 廣 as traditional; 廣 → 広 (新字体)
        val dictionary = CjkvDictionary.fromBytes(
            encode(
                0x5E7F to mapOf(6 to "广", 7 to "广 廣"),
                0x5EE3 to mapOf(6 to "广", 8 to "広"),
            )
        )
        val guang = requireNotNull(dictionary.lookupResolved(0x5E7F))
        assertEquals(listOf(CjkvFormVariant("广")), guang.forms(CjkvFormField.Simplified))
        assertEquals(listOf(CjkvFormVariant("広", 0x5EE3)), guang.forms(CjkvFormField.JapaneseNew))
        assertEquals(listOf(CjkvFormVariant("廣")), guang.forms(CjkvFormField.JapaneseOld))
    }

    @Test
    fun invalidHeaderIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CjkvDictionary.fromBytes(ByteArray(12))
        }
    }

    private fun encode(vararg entries: Pair<Int, Map<Int, String>>): ByteArray {
        val records = entries.sortedBy { it.first }.map { (_, fields) -> encodeRecord(fields) }
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(0x434A4B56)
            data.writeInt(4)
            data.writeInt(entries.size)
            var offset = 0
            entries.sortedBy { it.first }.zip(records).forEach { (entry, record) ->
                data.writeInt(entry.first)
                data.writeInt(offset)
                offset += record.size
            }
            records.forEach(data::write)
        }
        return output.toByteArray()
    }

    private fun encodeRecord(fields: Map<Int, String>): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(fields.keys.fold(0) { mask, index -> mask or (1 shl index) })
            fields.toSortedMap().values.forEach { value ->
                val bytes = value.toByteArray(Charsets.UTF_8)
                data.writeShort(bytes.size)
                data.write(bytes)
            }
        }
        return output.toByteArray()
    }
}
