package ee.oyatl.ime.dictionary

import java.io.DataOutputStream
import java.io.File

fun main(args: Array<String>) {
    val (inDict, inUnigrams, inBigrams) = args.take(3)
    val (outVocab, outUnigrams, outBigrams) = args.drop(3)

    println("load vocab...")
    val vocab = mutableListOf<DiskVocabDictionary.Entry>()
    File(inUnigrams).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (word, freq) = tokens
            vocab += DiskVocabDictionary.Entry(word, freq.toInt())
        }
    }
    val revVocab = vocab.mapIndexed { i, entry -> entry.result to i }.toMap()

    println("load dict...")
    val dict = mutableMapOf<String, List<String>>()
    File(inDict).forEachLine { line ->
        if(line.startsWith('#')) return@forEachLine
        val tokens = line.split(':')
        if(tokens.size == 3) {
            val (hangul, hanja, _) = tokens
            dict += hanja to (dict[hanja] ?: listOf()) + hangul
        }
    }

    println("generate unigrams...")
    val unigrams = IndexDictionary()
    revVocab.forEach { (key, index) ->
        val hanguls = dict[key]
        if(hanguls != null) hanguls.forEach { unigrams.insert(it, index) }
        else unigrams.insert(key, index)
    }

    println("generate bigrams...")
    val bigrams = IndexDictionary()
    File(inBigrams).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (grams, freq) = tokens
            val key = grams.split(' ').map { revVocab[it]!!.toChar() }.joinToString("")
            bigrams.insert(key, freq.toInt())
        }
    }
    DiskVocabDictionary.write(File(outVocab).outputStream(), vocab)
    unigrams.write(DataOutputStream(File(outUnigrams).outputStream()))
    bigrams.write(DataOutputStream(File(outBigrams).outputStream()))
}