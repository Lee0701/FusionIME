package org.jyutping.jyutping.host

import ee.oyatl.ime.newdict.BSTDictionary
import ee.oyatl.ime.newdict.JyutpingDictionary
import ee.oyatl.ime.newdict.TrieDictionary
import java.io.DataOutputStream
import java.io.File

fun genDict(
    inFile: String,
    outDir: String
) {
    val spellDict = BSTDictionary()
    val anchorDict = TrieDictionary()
    val vocabDict = JyutpingDictionary()

    val br = File(inFile).bufferedReader()

    var index = 0
    br.forEachLine { line ->
        val parts = line.split('\t')
        if(parts.size != 2) return@forEachLine
        val (word, romanization) = parts
        val syllableText = romanization.filter { it in 'a'..'z' }
        val spell = syllableText.hashCode()
        val anchors = romanization.split(' ').mapNotNull { it.firstOrNull() }.joinToString("")

        if(syllableText.isEmpty()) return@forEachLine
        if(anchors.isEmpty()) return@forEachLine

        vocabDict.insert(index, JyutpingDictionary.Entry(word, romanization))
        spellDict.insert(spell, index)
        anchorDict.insert(anchors, listOf(index))
        index += 1
    }

    spellDict.write(DataOutputStream(File(outDir, "jyutping_spell.bin").outputStream()))
    anchorDict.sorted().write(DataOutputStream(File(outDir, "jyutping_anchor.bin").outputStream()))
    vocabDict.write(DataOutputStream(File(outDir, "jyutping_vocab.bin").outputStream()))
}

fun main(args: Array<String>) {
    if(args.size == 2) {
        val (inFile, outDir) = args
        genDict(inFile, outDir)
    } else {
        val inFile = "data/jyutping.txt"
        val outDir = "app/jyutping/src/main/res/raw"
        genDict(inFile, outDir)
    }
}
