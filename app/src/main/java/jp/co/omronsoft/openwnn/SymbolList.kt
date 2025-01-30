/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.omronsoft.openwnn

import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.util.Log
import ee.oyatl.ime.fusion.R
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * The generator class of symbol list.
 * <br></br>
 * This class is used for generating lists of symbols.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class SymbolList(
    /** OpenWnn which has this instance  */
    private val mWnn: OpenWnn, lang: Int
) : WnnEngine {
    /*
     * DEFINITION OF VARIABLES
     */
    /** Symbols data  */
    protected var mSymbols: HashMap<String, ArrayList<String>> =
        HashMap()

    /** current list of symbols  */
    private var mCurrentList: ArrayList<String>?

    /** Iterator for getting symbols from the list  */
    private var mCurrentListIterator: Iterator<String>? = null

    /*
     * DEFINITION OF METHODS
     */
    /**
     * Constructor
     *
     * @param parent  OpenWnn instance which uses this.
     * @param lang    Language (`LANG_EN`, `LANG_JA` or `LANG_ZHCN`)
     */
    init {
        when (lang) {
            LANG_EN -> {
                /* symbols for English IME */
                mSymbols[SYMBOL_ENGLISH] = getXmlfile(R.xml.symbols_latin12_list)
                mCurrentList = mSymbols[SYMBOL_ENGLISH]
            }

            LANG_JA -> {
                /* symbols for Japanese IME */
                mSymbols[SYMBOL_JAPANESE] = getXmlfile(R.xml.symbols_japan_list)
                mSymbols[SYMBOL_JAPANESE_FACE] = getXmlfile(R.xml.symbols_japan_face_list)
                mSymbols[SYMBOL_ENGLISH] = getXmlfile(R.xml.symbols_latin1_list)
                mCurrentList = mSymbols[SYMBOL_JAPANESE]
            }

            LANG_ZHCN -> {
                /* symbols for Chinese IME */
                mSymbols[SYMBOL_CHINESE] = getXmlfile(R.xml.symbols_china_list)
                mSymbols[SYMBOL_ENGLISH] = getXmlfile(R.xml.symbols_latin1_list)
                mCurrentList = mSymbols[SYMBOL_CHINESE]
            }
        }

        mCurrentList = null
    }

    /**
     * Get a attribute value from a XML resource.
     *
     * @param xrp   XML resource
     * @param name  The attribute name
     *
     * @return  The value of the attribute
     */
    private fun getXmlAttribute(xrp: XmlResourceParser, name: String): String {
        val resId = xrp.getAttributeResourceValue(null, name, 0)
        return if (resId == 0) {
            xrp.getAttributeValue(null, name)
        } else {
            mWnn.getString(resId)
        }
    }

    /**
     * Load a symbols list from XML resource.
     *
     * @param id    XML resource ID
     * @return      The symbols list
     */
    private fun getXmlfile(id: Int): ArrayList<String> {
        val list = ArrayList<String>()

        val xrp = mWnn.resources.getXml(id)
        try {
            var xmlEventType: Int
            while ((xrp.next().also { xmlEventType = it }) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG) {
                    val attribute = xrp.name
                    if (XMLTAG_KEY == attribute) {
                        val value = getXmlAttribute(xrp, "value")
                        if (value != null) {
                            list.add(value)
                        }
                    }
                }
            }
            xrp.close()
        } catch (e: XmlPullParserException) {
            Log.e("OpenWnn", "Ill-formatted keybaord resource file")
        } catch (e: IOException) {
            Log.e("OpenWnn", "Unable to read keyboard resource file")
        }

        return list
    }

    /**
     * Set the dictionary
     *
     * @param listType  The list of symbol
     * @return          `true` if valid type is specified; `false` if not;
     */
    fun setDictionary(listType: String): Boolean {
        mCurrentList = mSymbols[listType]
        return (mCurrentList != null)
    }

    /***********************************************************************
     * WnnEngine's interface
     */
    /** @see jp.co.omronsoft.openwnn.WnnEngine.init
     */
    override fun init() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.close
     */
    override fun close() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.predict
     */
    override fun predict(text: ComposingText?, minLen: Int, maxLen: Int): Int {
        /* ignore if there is no list for the type */
        if (mCurrentList == null) {
            mCurrentListIterator = null
            return 0
        }

        /* return the iterator of the list */
        mCurrentListIterator = mCurrentList!!.iterator()
        return 1
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.convert
     */
    override fun convert(text: ComposingText?): Int {
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.searchWords
     */
    override fun searchWords(key: String?): Int {
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.searchWords
     */
    override fun searchWords(word: WnnWord?): Int {
        return 0
    }

    override val nextCandidate: WnnWord?
        /** @see jp.co.omronsoft.openwnn.WnnEngine.getNextCandidate
         */
        get() {
            if (mCurrentListIterator == null || !mCurrentListIterator!!.hasNext()) {
                return null
            }
            val str = mCurrentListIterator!!.next()
            val word = WnnWord(str, str)
            return word
        }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.learn
     */
    override fun learn(word: WnnWord): Boolean {
        return false
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.addWord
     */
    override fun addWord(word: WnnWord): Int {
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.deleteWord
     */
    override fun deleteWord(word: WnnWord?): Boolean {
        return false
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences?) {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.breakSequence
     */
    override fun breakSequence() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.makeCandidateListOf
     */
    override fun makeCandidateListOf(clausePosition: Int): Int {
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.initializeDictionary
     */
    override fun initializeDictionary(dictionary: Int): Boolean {
        return true
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.initializeDictionary
     */
    override fun initializeDictionary(dictionary: Int, type: Int): Boolean {
        return true
    }

    override val userDictionaryWords: Array<WnnWord?>?
        /** @see jp.co.omronsoft.openwnn.WnnEngine.getUserDictionaryWords
         */
        get() = null

    companion object {
        /*
     * DEFINITION OF CONSTANTS
     */
        /** Language definition (English)  */
        const val LANG_EN: Int = 0

        /** Language definition (Japanese)  */
        const val LANG_JA: Int = 1

        /** Language definition (Chinese)  */
        const val LANG_ZHCN: Int = 2


        /** Key string to get normal symbol list for Japanese  */
        const val SYMBOL_JAPANESE: String = "j"

        /** Key string to get normal symbol list for English  */
        const val SYMBOL_ENGLISH: String = "e"

        /** Key string to get normal symbol list for Chinese  */
        const val SYMBOL_CHINESE: String = "c1"

        /** Key string to get face mark list for Japanese  */
        const val SYMBOL_JAPANESE_FACE: String = "j_face"

        /** The name of XML tag key  */
        private const val XMLTAG_KEY = "string"
    }
}
