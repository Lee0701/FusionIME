package ee.oyatl.ime.viet.host

import ee.oyatl.ime.dictionary.Dictionary
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream

fun loadData(dictionary: Dictionary, source: InputStream) {
    val br = source.bufferedReader()
    br.forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 4) {
            val (key, word, freq, extra) = tokens
            dictionary.insert(key, Dictionary.Entry(word, extra, freq.toInt()))
        }
    }
}

fun generateDictionary(input: String, output: String) {
    val dictionary = Dictionary()
    loadData(dictionary, File(input).inputStream())
    dictionary.write(DataOutputStream(File(output).outputStream()))
}

fun main() {
    val inDir = "data"
    val outDir = "app/viet/src/main/res/raw"
    File(outDir).mkdirs()
    generateDictionary("$inDir/nom_quoc_ngu.tsv", "$outDir/nom_quoc_ngu.bin")
    generateDictionary("$inDir/nom_qwerty.tsv", "$outDir/nom_qwerty.bin")
    generateDictionary("$inDir/quoc_ngu_qwerty.tsv", "$outDir/quoc_ngu_qwerty.bin")
}