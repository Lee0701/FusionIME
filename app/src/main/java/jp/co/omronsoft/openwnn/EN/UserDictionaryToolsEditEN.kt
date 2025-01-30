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

import android.view.View
import jp.co.omronsoft.openwnn.OpenWnnEN
import jp.co.omronsoft.openwnn.OpenWnnEvent
import jp.co.omronsoft.openwnn.UserDictionaryToolsEdit
import jp.co.omronsoft.openwnn.UserDictionaryToolsList

/**
 * The user dictionary's word editor class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class UserDictionaryToolsEditEN : UserDictionaryToolsEdit {
    /**
     * Constructor
     */
    constructor() : super() {
        initialize()
    }

    /**
     * Constructor
     *
     * @param focusView         The view
     * @param focusPairView     The pair view
     */
    constructor(focusView: View?, focusPairView: View?) : super(focusView, focusPairView) {
        initialize()
    }

    /**
     * Initialize the parameters
     */
    fun initialize() {
        mListViewName = "jp.co.omronsoft.openwnn.EN.UserDictionaryToolsListEN"
        mPackageName = "ee.oyatl.ime.fusion"
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsEdit.sendEventToIME
     */
    override fun sendEventToIME(ev: OpenWnnEvent): Boolean {
        try {
            return OpenWnnEN.Companion.getInstance().onEvent(ev)
        } catch (ex: Exception) {
            /* do nothing if an error occurs */
        }
        return false
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsEdit.createUserDictionaryToolsList
     */
    override fun createUserDictionaryToolsList(): UserDictionaryToolsList {
        return UserDictionaryToolsListEN()
    }
}
