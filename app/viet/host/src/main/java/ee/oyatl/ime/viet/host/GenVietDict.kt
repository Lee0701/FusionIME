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

fun main(args: Array<String>) {
    val dictionary = Dictionary()
    val names = args.map { "nom_$it.tsv" }
    names.forEach { loadData(dictionary, File("data/$it").inputStream()) }
    dictionary.write(DataOutputStream(File("app/viet/src/main/res/raw/viet.bin").outputStream()))
}