package org.jyutping.jyutping

import android.content.Context
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
            spellDict = DiskBSTDictionary(context.resources.openRawResource(R.raw.jyutping_spell))
            anchorDict = DiskTrieDictionary(context.resources.openRawResource(R.raw.jyutping_anchor))
            vocabDict = DiskJyutpingDictionary(context.resources.openRawResource(R.raw.jyutping_vocab))
        }
    }
}
