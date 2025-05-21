package ee.oyatl.ime.dictionary

import java.io.DataOutputStream
import java.io.File

fun main(args: Array<String>) {
    val (inDict, inUnigrams, inBigrams) = args.take(3)
    val (outVocab, outDict, outBigrams) = args.drop(3)

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
    val dict = mutableMapOf<String, String>()
    File(inDict).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (hanja, hangul) = tokens
            dict += hanja to hangul
        }
    }

    println("generate unigrams...")
    val unigrams = IndexDictionary()
    revVocab.forEach { (key, index) ->
        val hangul = dict[key]
        if(hangul != null) unigrams.insert(hangul, index)
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
    unigrams.write(DataOutputStream(File(outDict).outputStream()))
    bigrams.write(DataOutputStream(File(outBigrams).outputStream()))
}