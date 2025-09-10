package ee.oyatl.ime.fusion.host

import ee.oyatl.ime.dictionary.Dictionary
import java.io.DataOutputStream
import java.io.File

fun main(args: Array<String>) {
    val (input, output) = args
    val dict = Dictionary()
    File(input).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (word, freq) = tokens
            dict.insert(word, Dictionary.Entry(word, "", freq.toInt()))
        }
    }
    dict.write(DataOutputStream(File(output).outputStream()))
}