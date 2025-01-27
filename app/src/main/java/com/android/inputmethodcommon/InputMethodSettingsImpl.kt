/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethodcommon

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.preference.Preference
import android.preference.PreferenceScreen
import android.provider.Settings
import android.text.TextUtils
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype

/* package private */
internal class InputMethodSettingsImpl : InputMethodSettingsInterface {
    private var mSubtypeEnablerPreference: Preference? = null
    private var mInputMethodSettingsCategoryTitleRes: Int = 0
    private var mInputMethodSettingsCategoryTitle: CharSequence? = null
    private var mSubtypeEnablerTitleRes: Int = 0
    private var mSubtypeEnablerTitle: CharSequence? = null
    private var mSubtypeEnablerIconRes: Int = 0
    private var mSubtypeEnablerIcon: Drawable? = null
    private var mImm: InputMethodManager? = null
    private var mImi: InputMethodInfo? = null

    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    fun init(context: Context, prefScreen: PreferenceScreen): Boolean {
        mImm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        mImi = getMyImi(context, mImm!!)
        if (mImi == null || mImi!!.getSubtypeCount() <= 1) {
            return false
        }
        val intent: Intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, mImi!!.getId())
        intent.setFlags(
            (Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        mSubtypeEnablerPreference = Preference(context)
        mSubtypeEnablerPreference!!.setIntent(intent)
        prefScreen.addPreference(mSubtypeEnablerPreference)
        updateSubtypeEnabler()
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(resId: Int) {
        mInputMethodSettingsCategoryTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(title: CharSequence?) {
        mInputMethodSettingsCategoryTitleRes = 0
        mInputMethodSettingsCategoryTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(resId: Int) {
        mSubtypeEnablerTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(title: CharSequence?) {
        mSubtypeEnablerTitleRes = 0
        mSubtypeEnablerTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(resId: Int) {
        mSubtypeEnablerIconRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(drawable: Drawable?) {
        mSubtypeEnablerIconRes = 0
        mSubtypeEnablerIcon = drawable
        updateSubtypeEnabler()
    }

    fun updateSubtypeEnabler() {
        val pref: Preference? = mSubtypeEnablerPreference
        if (pref == null) {
            return
        }
        val context: Context = pref.getContext()
        val title: CharSequence?
        if (mSubtypeEnablerTitleRes != 0) {
            title = context.getString(mSubtypeEnablerTitleRes)
        } else {
            title = mSubtypeEnablerTitle
        }
        pref.setTitle(title)
        val intent: Intent? = pref.getIntent()
        if (intent != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title)
        }
        val summary: String? = getEnabledSubtypesLabel(context, mImm, mImi)
        if (!TextUtils.isEmpty(summary)) {
            pref.setSummary(summary)
        }
        if (mSubtypeEnablerIconRes != 0) {
            pref.setIcon(mSubtypeEnablerIconRes)
        } else {
            pref.setIcon(mSubtypeEnablerIcon)
        }
    }

    companion object {
        private fun getMyImi(context: Context, imm: InputMethodManager): InputMethodInfo? {
            val imis: List<InputMethodInfo> = imm.getInputMethodList()
            for (i in imis.indices) {
                val imi: InputMethodInfo = imis.get(i)
                if (imis.get(i).getPackageName() == context.getPackageName()) {
                    return imi
                }
            }
            return null
        }

        private fun getEnabledSubtypesLabel(
            context: Context?, imm: InputMethodManager?, imi: InputMethodInfo?
        ): String? {
            if (context == null || imm == null || imi == null) return null
            val subtypes: List<InputMethodSubtype> = imm.getEnabledInputMethodSubtypeList(imi, true)
            val sb: StringBuilder = StringBuilder()
            val N: Int = subtypes.size
            for (i in 0 until N) {
                val subtype: InputMethodSubtype = subtypes.get(i)
                if (sb.length > 0) {
                    sb.append(", ")
                }
                sb.append(
                    subtype.getDisplayName(
                        context, imi.getPackageName(),
                        imi.getServiceInfo().applicationInfo
                    )
                )
            }
            return sb.toString()
        }
    }
}
