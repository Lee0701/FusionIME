package ee.oyatl.ime.fusion.util

import android.content.Context
import org.json.JSONObject

object KyujitaiConverter {
    private val charMap = HashMap<Char, String>()
    private var douonReplacements: List<Pair<String, String>> = emptyList()
    private var excludedWords: List<String> = emptyList()

    fun initialize(context: Context) {
        val json = context.assets
            .open("kyujitai.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        charMap.clear()
        val kyuji = root.getJSONArray("kyuji")
        for (i in 0 until kyuji.length()) {
            val entry = kyuji.getJSONArray(i)
            val newChar = entry.getString(0)[0]
            val oldChar = entry.getString(1)
            charMap[newChar] = oldChar
        }
        val replacements = LinkedHashMap<String, String>()
        val douon = root.getJSONArray("douon")
        for (i in 0 until douon.length()) {
            val entry = douon.getJSONObject(i)
            val oldForms = entry.getJSONArray("old")
            val newForms = entry.getJSONArray("new")
            val words = entry.getJSONArray("words")
            val pairCount = minOf(oldForms.length(), newForms.length())
            for (j in 0 until words.length()) {
                val modernWord = words.getString(j)
                var oldWord = modernWord
                for (k in 0 until pairCount) {
                    oldWord = oldWord.replace(
                        newForms.getString(k),
                        oldForms.getString(k)
                    )
                }
                if (oldWord != modernWord) {
                    replacements[modernWord] = oldWord
                }
            }
        }
        douonReplacements = replacements.entries
            .sortedByDescending { it.key.length }
            .map { it.key to it.value }
        val exclude = root.getJSONArray("exclude")
        excludedWords = (0 until exclude.length())
            .map { exclude.getString(it) }
            .sortedByDescending { it.length }
    }

    fun encode(text: String, enabled: Boolean): String {
        if (!enabled) {
            return text
        }
        return buildString {
            var index = 0
            var segmentStart = 0
            while (index < text.length) {
                val excludedWord = excludedWords.firstOrNull {
                    text.startsWith(it, index)
                }
                if (excludedWord == null) {
                    index++
                    continue
                }
                append(encodeSegment(text.substring(segmentStart, index)))
                append(excludedWord)
                index += excludedWord.length
                segmentStart = index
            }
            append(encodeSegment(text.substring(segmentStart)))
        }
    }

    private fun encodeSegment(text: String): String {
        var result = text
        for ((modernWord, oldWord) in douonReplacements) {
            result = result.replace(modernWord, oldWord)
        }
        return buildString {
            for (character in result) {
                append(charMap[character] ?: character)
            }
        }
    }
}
