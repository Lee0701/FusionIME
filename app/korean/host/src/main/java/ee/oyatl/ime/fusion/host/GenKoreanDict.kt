package ee.oyatl.ime.fusion.host

import ee.oyatl.ime.newdict.HanjaDictionary
import ee.oyatl.ime.newdict.StringDictionary
import ee.oyatl.ime.newdict.TrieDictionary
import java.io.DataOutputStream
import java.io.File

fun main(args: Array<String>) {
    val (input, outDir) = args
    val indexDict = TrieDictionary()
    val revIndexDict = TrieDictionary()
    val contentDict = HanjaDictionary()
    val definitionDict = StringDictionary()
    var i = 0
    File(input).forEachLine { line ->
        val tokens = line.split('\t')
        if(tokens.size == 5) {
            val (hangul, hanja, freq, extra, definition) = tokens
            indexDict.insert(hangul, listOf(i))
            revIndexDict.insert(hanja, listOf(i))
            contentDict.insert(i, HanjaDictionary.Entry(hangul, hanja, freq.toInt(), extra))
            definitionDict.insert(i, definition)
            i += 1
        }
    }
    val outDirFile = File(outDir)
    indexDict.write(DataOutputStream(File(outDirFile, "hanja_index.bin").outputStream()))
//    revIndexDict.write(DataOutputStream(File(outDirFile, "hanja_rev_index.bin").outputStream()))
    contentDict.write(DataOutputStream(File(outDirFile, "hanja_content.bin").outputStream()))
//    definitionDict.write(DataOutputStream(File(outDirFile, "hanja_definition.bin").outputStream()))
}