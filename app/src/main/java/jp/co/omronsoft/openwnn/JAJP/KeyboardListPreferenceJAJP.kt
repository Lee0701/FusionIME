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
package jp.co.omronsoft.openwnn.JAJP

import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import jp.co.omronsoft.openwnn.OpenWnnEvent
import jp.co.omronsoft.openwnn.OpenWnnJAJP

/**
 * The preference class of keyboard image list for Japanese IME.
 * This class notices to `OpenWnnJAJP` that the keyboard image is changed.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.
 */
class KeyboardListPreferenceJAJP @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) :
    ListPreference(context, attrs) {
    /** @see android.preference.DialogPreference.onDialogClosed
     */
    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (positiveResult) {
            val wnn: OpenWnnJAJP = OpenWnnJAJP.Companion.getInstance()
            val code: Int = OpenWnnEvent.Companion.CHANGE_INPUT_VIEW
            val ev = OpenWnnEvent(code)
            try {
                wnn.onEvent(ev)
            } catch (ex: Exception) {
            }
        }
    }
}
