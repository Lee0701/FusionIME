package org.jyutping.jyutping

import android.content.Context
import ee.oyatl.ime.dictionary.manager.DictionaryCache
import ee.oyatl.ime.newdict.DiskBSTDictionary
import ee.oyatl.ime.newdict.DiskJyutpingDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary

object BinaryDictionaries {
    var isInitialized: Boolean = false
        private set
    lateinit var spellDict: DiskBSTDictionary
        private set
    lateinit var anchorDict: DiskTrieDictionary
        private set
    lateinit var vocabDict: DiskJyutpingDictionary
        private set

    fun loadDictionaries(context: Context) {
        if(!isInitialized) {
            val spellDictId = R.raw.jyutping_spell
            val anchorDictId = R.raw.jyutping_anchor
            val vocabDictId = R.raw.jyutping_vocab
            spellDict = DictionaryCache.get(spellDictId) {
                context.resources.openRawResource(spellDictId).use {
                    DiskBSTDictionary(it)
                }
            }
            anchorDict = DictionaryCache.get(anchorDictId) {
                context.resources.openRawResource(anchorDictId).use {
                    DiskTrieDictionary(it)
                }
            }
            vocabDict = DictionaryCache.get(vocabDictId) {
                context.resources.openRawResource(vocabDictId).use {
                    DiskJyutpingDictionary(it)
                }
            }
            isInitialized = true
        }
    }
}
