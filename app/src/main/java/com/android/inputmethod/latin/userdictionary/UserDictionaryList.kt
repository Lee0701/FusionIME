/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.latin.userdictionary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.provider.UserDictionary.Words
import android.text.TextUtils
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.LocaleUtils
import java.util.Locale
import java.util.TreeSet

// Caveat: This class is basically taken from
// packages/apps/Settings/src/com/android/settings/inputmethod/UserDictionaryList.java
// in order to deal with some devices that have issues with the user dictionary handling
class UserDictionaryList : PreferenceFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()))
    }

    /**
     * Creates the entries that allow the user to go into the user dictionary for each locale.
     * @param userDictGroup The group to put the settings in.
     */
    protected fun createUserDictSettings(userDictGroup: PreferenceGroup) {
        val activity: Activity = getActivity()
        userDictGroup.removeAll()
        val localeSet: TreeSet<String?>? =
            getUserDictionaryLocalesSet(activity)

        if (localeSet!!.size > 1) {
            // Have an "All languages" entry in the languages list if there are two or more active
            // languages
            localeSet.add("")
        }

        if (localeSet.isEmpty()) {
            userDictGroup.addPreference(createUserDictionaryPreference(null))
        } else {
            for (locale: String? in localeSet) {
                userDictGroup.addPreference(createUserDictionaryPreference(locale))
            }
        }
    }

    /**
     * Create a single User Dictionary Preference object, with its parameters set.
     * @param localeString The locale for which this user dictionary is for.
     * @return The corresponding preference.
     */
    protected fun createUserDictionaryPreference(localeString: String?): Preference {
        val newPref: Preference = Preference(getActivity())
        val intent: Intent = Intent(
            USER_DICTIONARY_SETTINGS_INTENT_ACTION
        )
        if (null == localeString) {
            newPref.setTitle(Locale.getDefault().getDisplayName())
        } else {
            if (localeString.isEmpty()) {
                newPref.setTitle(getString(R.string.user_dict_settings_all_languages))
            } else {
                newPref.setTitle(
                    LocaleUtils.constructLocaleFromString(
                        localeString
                    )!!
                        .getDisplayName()
                )
            }
            intent.putExtra("locale", localeString)
            newPref.getExtras().putString("locale", localeString)
        }
        newPref.setIntent(intent)
        newPref.setFragment(UserDictionarySettings::class.java.getName())
        return newPref
    }

    override fun onResume() {
        super.onResume()
        createUserDictSettings(getPreferenceScreen())
    }

    companion object {
        const val USER_DICTIONARY_SETTINGS_INTENT_ACTION: String =
            "android.settings.USER_DICTIONARY_SETTINGS"

        fun getUserDictionaryLocalesSet(activity: Activity): TreeSet<String?>? {
            val cursor: Cursor? = activity.getContentResolver().query(
                Words.CONTENT_URI,
                arrayOf(Words.LOCALE),
                null, null, null
            )
            val localeSet: TreeSet<String?> = TreeSet()
            if (null == cursor) {
                // The user dictionary service is not present or disabled. Return null.
                return null
            }
            try {
                if (cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndex(Words.LOCALE)
                    do {
                        val locale: String? = cursor.getString(columnIndex)
                        localeSet.add(if (null != locale) locale else "")
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor.close()
            }
            if (!UserDictionarySettings.Companion.IS_SHORTCUT_API_SUPPORTED) {
                // For ICS, we need to show "For all languages" in case that the keyboard locale
                // is different from the system locale
                localeSet.add("")
            }

            val imm: InputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val imis: List<InputMethodInfo> = imm.getEnabledInputMethodList()
            for (imi: InputMethodInfo? in imis) {
                val subtypes: List<InputMethodSubtype> =
                    imm.getEnabledInputMethodSubtypeList(
                        imi, true /* allowsImplicitlySelectedSubtypes */
                    )
                for (subtype: InputMethodSubtype in subtypes) {
                    val locale: String = subtype.getLocale()
                    if (!TextUtils.isEmpty(locale)) {
                        localeSet.add(locale)
                    }
                }
            }

            // We come here after we have collected locales from existing user dictionary entries and
            // enabled subtypes. If we already have the locale-without-country version of the system
            // locale, we don't add the system locale to avoid confusion even though it's technically
            // correct to add it.
            if (!localeSet.contains(Locale.getDefault().getLanguage().toString())) {
                localeSet.add(Locale.getDefault().toString())
            }

            return localeSet
        }
    }
}

