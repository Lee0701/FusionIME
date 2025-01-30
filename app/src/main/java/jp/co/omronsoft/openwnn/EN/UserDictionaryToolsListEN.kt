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
package jp.co.omronsoft.openwnn.EN

import android.os.Bundle
import android.view.View
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.OpenWnnEN
import jp.co.omronsoft.openwnn.OpenWnnEvent
import jp.co.omronsoft.openwnn.UserDictionaryToolsEdit
import jp.co.omronsoft.openwnn.UserDictionaryToolsList
import jp.co.omronsoft.openwnn.WnnWord

/**
 * The user dictionary tool class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class UserDictionaryToolsListEN : UserDictionaryToolsList() {
    /**
     * Constructor
     */
    init {
        if (OpenWnnEN.Companion.getInstance() == null) {
            OpenWnnEN(this)
        }
        mListViewName = "jp.co.omronsoft.openwnn.EN.UserDictionaryToolsListEN"
        mEditViewName = "jp.co.omronsoft.openwnn.EN.UserDictionaryToolsEditEN"
        mPackageName = "ee.oyatl.ime.fusion"
    }

    /** @see jp.co.omronsoft.iwnnime.ml.UserDictionaryToolsList.onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.user_dictionary_list_words_en)
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList.createUserDictionaryToolsEdit
     */
    override fun createUserDictionaryToolsEdit(
        view1: View?,
        view2: View?
    ): UserDictionaryToolsEdit {
        return UserDictionaryToolsEditEN(view1, view2)
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList.sendEventToIME
     */
    override fun sendEventToIME(ev: OpenWnnEvent): Boolean {
        try {
            return OpenWnnEN.Companion.getInstance().onEvent(ev)
        } catch (ex: Exception) {
            /* do nothing if an error occurs */
        }
        return false
    }

    override val comparator: Comparator<WnnWord?>
        /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList.getComparator
         */
        get() = ListComparatorEN()

    /** Comparator class for sorting the list of English user dictionary  */
    protected inner class ListComparatorEN : Comparator<WnnWord> {
        override fun compare(word1: WnnWord, word2: WnnWord): Int {
            return word1.stroke!!.compareTo(word2.stroke!!)
        }
    }
}
