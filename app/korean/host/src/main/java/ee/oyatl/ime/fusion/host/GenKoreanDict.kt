package ee.oyatl.ime.fusion.host

import ee.oyatl.ime.newdict.HanjaDictionary
import ee.oyatl.ime.newdict.NGramDictionary
import ee.oyatl.ime.newdict.StringDictionary
import ee.oyatl.ime.newdict.TrieDictionary
import java.io.DataOutputStream
import java.io.File

fun main(args: Array<String>) {
    val (input, bigramInput, outDir) = args
    val indexDict = TrieDictionary()
    val revIndexDict = TrieDictionary()
    val contentDict = HanjaDictionary()
    val definitionDict = StringDictionary()
    var i = 0
    File(input).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 5) {
            val (hangul, hanja, freq, extra, definition) = tokens
            if(revIndexDict.get(hanja).isEmpty()) {
                revIndexDict.insert(hanja, listOf(i))
                contentDict.insert(i, HanjaDictionary.Entry(hangul, hanja, freq.toInt(), extra))
                definitionDict.insert(i, definition)
                i += 1
            }
            val id = revIndexDict.get(hanja).first()
            val content = contentDict.get(id)
            if(content != null) {
                if(freq.toInt() > content.frequency) {
                    contentDict.insert(id, HanjaDictionary.Entry(hangul, hanja, freq.toInt(), extra))
                    definitionDict.insert(id, definition)
                }
            }
            indexDict.insert(hangul, listOf(id))
        }
    }

    val bigramDict = NGramDictionary()
    File(bigramInput).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 2) {
            val (s, f) = tokens
            val sequence = s.split(' ')
            val freq = f.toInt()
            if(sequence.any { revIndexDict.get(it).isEmpty() }) return@forEachLine
            val ids = sequence.map { revIndexDict.get(it).first() }
            val key = ids.dropLast(1)
            val value = mapOf(ids.last() to freq)
            bigramDict.insert(key, value)
        }
    }

    val outDirFile = File(outDir)
    indexDict.write(DataOutputStream(File(outDirFile, "hanja_index.bin").outputStream()))
//    revIndexDict.write(DataOutputStream(File(outDirFile, "hanja_rev_index.bin").outputStream()))
    contentDict.write(DataOutputStream(File(outDirFile, "hanja_content.bin").outputStream()))
//    definitionDict.write(DataOutputStream(File(outDirFile, "hanja_definition.bin").outputStream()))
    bigramDict.write(DataOutputStream(File(outDirFile, "hanja_bigram.bin").outputStream()))
}