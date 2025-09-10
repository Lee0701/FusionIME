package ee.oyatl.ime.fusion.host

import ee.oyatl.ime.dictionary.Dictionary
import ee.oyatl.ime.fusion.hangul.Hangul
import java.io.DataOutputStream
import java.io.File
import java.text.Normalizer

fun main(args: Array<String>) {
    val (input, output) = args
    val dict = Dictionary()
    File(input).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (value, keys) = tokens
            val keyList = splitKeys(keys)
            keyList.forEach { key ->
                dict.insert(key, Dictionary.Entry(value, "", 0))
            }
        }
    }
    dict.write(DataOutputStream(File(output).outputStream()))
}

fun splitKeys(keys: String): List<String> {
    val normalized = Normalizer.normalize(keys, Normalizer.Form.NFD)
    val result = mutableListOf<String>()
    var current = ""
    fun append() {
        result += Normalizer.normalize(current, Normalizer.Form.NFC)
        current = ""
    }
    normalized.forEach { c ->
        when {
            Hangul.isCho(c.code) -> {
                if(current.isNotEmpty()) append()
                current += c
            }
            Hangul.isJung(c.code) -> {
                current += c
            }
            Hangul.isJong(c.code) -> {
                current += c
            }
            c.code == 0x302e || c.code == 0x302f -> {
                current += c
            }
        }
    }
    if(current.isNotEmpty()) append()
    return result
}