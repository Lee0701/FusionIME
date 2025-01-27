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
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.UserDictionary.Words
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import com.android.inputmethod.compat.UserDictionaryCompatUtils
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.LocaleUtils
import java.util.Locale
import java.util.TreeSet

// Caveat: This class is basically taken from
// packages/apps/Settings/src/com/android/settings/inputmethod/UserDictionaryAddWordContents.java
// in order to deal with some devices that have issues with the user dictionary handling
/**
 * A container class to factor common code to UserDictionaryAddWordFragment
 * and UserDictionaryAddWordActivity.
 */
class UserDictionaryAddWordContents {
    private val mMode: Int // Either MODE_EDIT or MODE_INSERT
    private val mWordEditText: EditText
    private val mShortcutEditText: EditText?
    var currentUserDictionaryLocale: String? = null
        private set
    private val mOldWord: String?
    private val mOldShortcut: String?
    private var mSavedWord: String? = null
    private var mSavedShortcut: String? = null

    /* package */
    internal constructor(view: View, args: Bundle) {
        mWordEditText = view.findViewById<View>(R.id.user_dictionary_add_word_text) as EditText
        mShortcutEditText = view.findViewById<View>(R.id.user_dictionary_add_shortcut) as EditText?
        if (!UserDictionarySettings.Companion.IS_SHORTCUT_API_SUPPORTED) {
            mShortcutEditText!!.setVisibility(View.GONE)
            view.findViewById<View>(R.id.user_dictionary_add_shortcut_label)
                .setVisibility(View.GONE)
        }
        val word: String? = args.getString(EXTRA_WORD)
        if (null != word) {
            mWordEditText.setText(word)
            // Use getText in case the edit text modified the text we set. This happens when
            // it's too long to be edited.
            mWordEditText.setSelection(mWordEditText.getText().length)
        }
        val shortcut: String?
        if (UserDictionarySettings.Companion.IS_SHORTCUT_API_SUPPORTED) {
            shortcut = args.getString(EXTRA_SHORTCUT)
            if (null != shortcut && null != mShortcutEditText) {
                mShortcutEditText.setText(shortcut)
            }
            mOldShortcut = args.getString(EXTRA_SHORTCUT)
        } else {
            shortcut = null
            mOldShortcut = null
        }
        mMode = args.getInt(EXTRA_MODE) // default return value for #getInt() is 0 = MODE_EDIT
        mOldWord = args.getString(EXTRA_WORD)
        updateLocale(args.getString(EXTRA_LOCALE))
    }

    /* package */
    internal constructor(
        view: View,
        oldInstanceToBeEdited: UserDictionaryAddWordContents
    ) {
        mWordEditText = view.findViewById<View>(R.id.user_dictionary_add_word_text) as EditText
        mShortcutEditText = view.findViewById<View>(R.id.user_dictionary_add_shortcut) as EditText?
        mMode = MODE_EDIT
        mOldWord = oldInstanceToBeEdited.mSavedWord
        mOldShortcut = oldInstanceToBeEdited.mSavedShortcut
        updateLocale(currentUserDictionaryLocale)
    }

    // locale may be null, this means default locale
    // It may also be the empty string, which means "all locales"
    /* package */
    fun updateLocale(locale: String?) {
        currentUserDictionaryLocale = if (null == locale) Locale.getDefault().toString() else locale
    }

    /* package */
    fun saveStateIntoBundle(outState: Bundle) {
        outState.putString(EXTRA_WORD, mWordEditText.getText().toString())
        outState.putString(EXTRA_ORIGINAL_WORD, mOldWord)
        if (null != mShortcutEditText) {
            outState.putString(EXTRA_SHORTCUT, mShortcutEditText.getText().toString())
        }
        if (null != mOldShortcut) {
            outState.putString(EXTRA_ORIGINAL_SHORTCUT, mOldShortcut)
        }
        outState.putString(
            EXTRA_LOCALE,
            currentUserDictionaryLocale
        )
    }

    /* package */
    fun delete(context: Context) {
        if (MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            // Mode edit: remove the old entry.
            val resolver: ContentResolver = context.getContentResolver()
            UserDictionarySettings.Companion.deleteWord(mOldWord, mOldShortcut, resolver)
        }
        // If we are in add mode, nothing was added, so we don't need to do anything.
    }

    /* package */
    fun apply(context: Context, outParameters: Bundle?): Int {
        if (null != outParameters) saveStateIntoBundle(outParameters)
        val resolver: ContentResolver = context.getContentResolver()
        if (MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            // Mode edit: remove the old entry.
            UserDictionarySettings.Companion.deleteWord(mOldWord, mOldShortcut, resolver)
        }
        val newWord: String = mWordEditText.getText().toString()
        val newShortcut: String?
        if (!UserDictionarySettings.Companion.IS_SHORTCUT_API_SUPPORTED) {
            newShortcut = null
        } else if (null == mShortcutEditText) {
            newShortcut = null
        } else {
            val tmpShortcut: String = mShortcutEditText.getText().toString()
            if (TextUtils.isEmpty(tmpShortcut)) {
                newShortcut = null
            } else {
                newShortcut = tmpShortcut
            }
        }
        if (TextUtils.isEmpty(newWord)) {
            // If the word is somehow empty, don't insert it.
            return CODE_CANCEL
        }
        mSavedWord = newWord
        mSavedShortcut = newShortcut
        // If there is no shortcut, and the word already exists in the database, then we
        // should not insert, because either A. the word exists with no shortcut, in which
        // case the exact same thing we want to insert is already there, or B. the word
        // exists with at least one shortcut, in which case it has priority on our word.
        if (TextUtils.isEmpty(newShortcut) && hasWord(newWord, context)) {
            return CODE_ALREADY_PRESENT
        }

        // Disallow duplicates. If the same word with no shortcut is defined, remove it; if
        // the same word with the same shortcut is defined, remove it; but we don't mind if
        // there is the same word with a different, non-empty shortcut.
        UserDictionarySettings.Companion.deleteWord(newWord, null, resolver)
        if (!TextUtils.isEmpty(newShortcut)) {
            // If newShortcut is empty we just deleted this, no need to do it again
            UserDictionarySettings.Companion.deleteWord(newWord, newShortcut, resolver)
        }

        // In this class we use the empty string to represent 'all locales' and mLocale cannot
        // be null. However the addWord method takes null to mean 'all locales'.
        UserDictionaryCompatUtils.addWord(
            context, newWord.toString(),
            FREQUENCY_FOR_USER_DICTIONARY_ADDS, newShortcut, if (TextUtils.isEmpty(
                    currentUserDictionaryLocale
                )
            ) null else LocaleUtils.constructLocaleFromString(
                currentUserDictionaryLocale
            )
        )

        return CODE_WORD_ADDED
    }

    private fun hasWord(word: String, context: Context): Boolean {
        val cursor: Cursor?
        // mLocale == "" indicates this is an entry for all languages. Here, mLocale can't
        // be null at all (it's ensured by the updateLocale method).
        if ("" == currentUserDictionaryLocale) {
            cursor = context.getContentResolver().query(
                Words.CONTENT_URI,
                HAS_WORD_PROJECTION, HAS_WORD_SELECTION_ALL_LOCALES,
                arrayOf(word), null /* sort order */
            )
        } else {
            cursor = context.getContentResolver().query(
                Words.CONTENT_URI,
                HAS_WORD_PROJECTION, HAS_WORD_SELECTION_ONE_LOCALE,
                arrayOf(word, currentUserDictionaryLocale), null /* sort order */
            )
        }
        try {
            if (null == cursor) return false
            return cursor.getCount() > 0
        } finally {
            if (null != cursor) cursor.close()
        }
    }

    class LocaleRenderer(context: Context, localeString: String?) {
        val localeString: String?
        private var mDescription: String? = null

        init {
            this.localeString = localeString
            if (null == localeString) {
                mDescription = context.getString(R.string.user_dict_settings_more_languages)
            } else if ("" == localeString) {
                mDescription = context.getString(R.string.user_dict_settings_all_languages)
            } else {
                mDescription = LocaleUtils.constructLocaleFromString(
                    localeString
                )!!
                    .getDisplayName()
            }
        }

        override fun toString(): String {
            return mDescription!!
        }

        val isMoreLanguages: Boolean
            // "More languages..." is null ; "All languages" is the empty string.
            get() {
                return null == localeString
            }
    }

    // Helper method to get the list of locales to display for this word
    fun getLocalesList(activity: Activity): ArrayList<LocaleRenderer> {
        val locales: TreeSet<String?>? =
            UserDictionaryList.Companion.getUserDictionaryLocalesSet(activity)
        // Remove our locale if it's in, because we're always gonna put it at the top
        locales!!.remove(currentUserDictionaryLocale) // mLocale may not be null
        val systemLocale: String = Locale.getDefault().toString()
        // The system locale should be inside. We want it at the 2nd spot.
        locales.remove(systemLocale) // system locale may not be null
        locales.remove("") // Remove the empty string if it's there
        val localesList: ArrayList<LocaleRenderer> = ArrayList()
        // Add the passed locale, then the system locale at the top of the list. Add an
        // "all languages" entry at the bottom of the list.
        addLocaleDisplayNameToList(
            activity, localesList,
            currentUserDictionaryLocale
        )
        if (systemLocale != currentUserDictionaryLocale) {
            addLocaleDisplayNameToList(activity, localesList, systemLocale)
        }
        for (l: String? in locales) {
            // TODO: sort in unicode order
            addLocaleDisplayNameToList(activity, localesList, l)
        }
        if ("" != currentUserDictionaryLocale) {
            // If mLocale is "", then we already inserted the "all languages" item, so don't do it
            addLocaleDisplayNameToList(activity, localesList, "") // meaning: all languages
        }
        localesList.add(LocaleRenderer(activity, null)) // meaning: select another locale
        return localesList
    }

    companion object {
        const val EXTRA_MODE: String = "mode"
        const val EXTRA_WORD: String = "word"
        const val EXTRA_SHORTCUT: String = "shortcut"
        const val EXTRA_LOCALE: String = "locale"
        const val EXTRA_ORIGINAL_WORD: String = "originalWord"
        const val EXTRA_ORIGINAL_SHORTCUT: String = "originalShortcut"

        const val MODE_EDIT: Int = 0
        const val MODE_INSERT: Int = 1

        /* package */
        const val CODE_WORD_ADDED: Int = 0

        /* package */
        const val CODE_CANCEL: Int = 1

        /* package */
        const val CODE_ALREADY_PRESENT: Int = 2

        private const val FREQUENCY_FOR_USER_DICTIONARY_ADDS: Int = 250

        private val HAS_WORD_PROJECTION: Array<String> = arrayOf(Words.WORD)
        private val HAS_WORD_SELECTION_ONE_LOCALE: String = (Words.WORD
                + "=? AND " + Words.LOCALE + "=?")
        private val HAS_WORD_SELECTION_ALL_LOCALES: String = (Words.WORD
                + "=? AND " + Words.LOCALE + " is null")

        private fun addLocaleDisplayNameToList(
            context: Context,
            list: ArrayList<LocaleRenderer>, locale: String?
        ) {
            if (null != locale) {
                list.add(LocaleRenderer(context, locale))
            }
        }
    }
}

