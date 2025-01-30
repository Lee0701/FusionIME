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

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.widget.Toast
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.OpenWnnEN
import jp.co.omronsoft.openwnn.OpenWnnEvent
import jp.co.omronsoft.openwnn.WnnWord

/**
 * The preference class to clear user dictionary for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class ClearUserDictionaryDialogPreferenceEN @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) :
    DialogPreference(context, attrs) {
    /** The context  */
    protected var mContext: Context? = null

    /**
     * Constructor
     *
     * @param context   The context
     * @param attrs     The set of attributes
     */
    /**
     * Constructor
     *
     * @param context   The context
     */
    init {
        mContext = context
    }

    /** @see android.preference.DialogPreference.onDialogClosed
     */
    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            /* clear the user dictionary */
            val ev = OpenWnnEvent(OpenWnnEvent.INITIALIZE_USER_DICTIONARY, WnnWord())
            OpenWnnEN.instance?.onEvent(ev)

            /* show the message */
            Toast.makeText(
                mContext!!.applicationContext, R.string.dialog_clear_user_dictionary_done,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
