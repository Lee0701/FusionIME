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
package com.android.inputmethod.latin.spellcheck

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import android.service.textservice.SpellCheckerService
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.view.textservice.SuggestionsInfo
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardId
import com.android.inputmethod.keyboard.KeyboardLayoutSet
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.DictionaryFacilitatorLruCache
import com.android.inputmethod.latin.NgramContext
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.RichInputMethodSubtype
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils
import com.android.inputmethod.latin.utils.ScriptUtils
import com.android.inputmethod.latin.utils.SuggestionResults
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore

/**
 * Service for spell checking, using LatinIME's dictionaries and mechanisms.
 */
class AndroidSpellCheckerService : SpellCheckerService(), OnSharedPreferenceChangeListener {
    private val MAX_NUM_OF_THREADS_READ_DICTIONARY: Int = 2
    private val mSemaphore: Semaphore = Semaphore(
        MAX_NUM_OF_THREADS_READ_DICTIONARY,
        true /* fair */
    )

    // TODO: Make each spell checker session has its own session id.
    private val mSessionIdPool: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue()

    private val mDictionaryFacilitatorCache: DictionaryFacilitatorLruCache =
        DictionaryFacilitatorLruCache(
            this,  /* context */DICTIONARY_NAME_PREFIX
        )
    private val mKeyboardCache: ConcurrentHashMap<Locale, Keyboard> = ConcurrentHashMap()

    // The threshold for a suggestion to be considered "recommended".
    var recommendedThreshold: Float = 0f
        private set

    // TODO: make a spell checker option to block offensive words or not
    private val mSettingsValuesForSuggestion: SettingsValuesForSuggestion =
        SettingsValuesForSuggestion(true /* blockPotentiallyOffensive */)

    init {
        for (i in 0 until MAX_NUM_OF_THREADS_READ_DICTIONARY) {
            mSessionIdPool.add(i)
        }
    }

    override fun onCreate() {
        super.onCreate()
        recommendedThreshold =
            getString(R.string.spellchecker_recommended_threshold_value).toFloat()
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(prefs, PREF_USE_CONTACTS_KEY)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (PREF_USE_CONTACTS_KEY != key) return
        val useContactsDictionary: Boolean = prefs.getBoolean(PREF_USE_CONTACTS_KEY, true)
        mDictionaryFacilitatorCache.setUseContactsDictionary(useContactsDictionary)
    }

    override fun createSession(): Session {
        // Should not refer to AndroidSpellCheckerSession directly considering
        // that AndroidSpellCheckerSession may be overlaid.
        return AndroidSpellCheckerSessionFactory.newInstance(this)
    }

    fun isValidWord(locale: Locale?, word: String): Boolean {
        mSemaphore.acquireUninterruptibly()
        try {
            val dictionaryFacilitatorForLocale: DictionaryFacilitator =
                mDictionaryFacilitatorCache.get(locale)
            return dictionaryFacilitatorForLocale.isValidSpellingWord(word)
        } finally {
            mSemaphore.release()
        }
    }

    fun getSuggestionResults(
        locale: Locale?,
        composedData: ComposedData, ngramContext: NgramContext,
        keyboard: Keyboard
    ): SuggestionResults {
        var sessionId: Int? = null
        mSemaphore.acquireUninterruptibly()
        try {
            sessionId = mSessionIdPool.poll()
            val dictionaryFacilitatorForLocale: DictionaryFacilitator =
                mDictionaryFacilitatorCache.get(locale)
            return dictionaryFacilitatorForLocale.getSuggestionResults(
                composedData, ngramContext,
                keyboard, mSettingsValuesForSuggestion,
                sessionId, SuggestedWords.INPUT_STYLE_TYPING
            )
        } finally {
            if (sessionId != null) {
                mSessionIdPool.add(sessionId)
            }
            mSemaphore.release()
        }
    }

    fun hasMainDictionaryForLocale(locale: Locale?): Boolean {
        mSemaphore.acquireUninterruptibly()
        try {
            val dictionaryFacilitator: DictionaryFacilitator =
                mDictionaryFacilitatorCache.get(locale)
            return dictionaryFacilitator.hasAtLeastOneInitializedMainDictionary()
        } finally {
            mSemaphore.release()
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        mSemaphore.acquireUninterruptibly(MAX_NUM_OF_THREADS_READ_DICTIONARY)
        try {
            mDictionaryFacilitatorCache.closeDictionaries()
        } finally {
            mSemaphore.release(MAX_NUM_OF_THREADS_READ_DICTIONARY)
        }
        mKeyboardCache.clear()
        return false
    }

    fun getKeyboardForLocale(locale: Locale): Keyboard {
        var keyboard: Keyboard? = mKeyboardCache.get(locale)
        if (keyboard == null) {
            keyboard = createKeyboardForLocale(locale)
            if (keyboard != null) {
                mKeyboardCache[locale] = keyboard
            }
        }
        return keyboard
    }

    private fun createKeyboardForLocale(locale: Locale): Keyboard {
        val keyboardLayoutName: String = getKeyboardLayoutNameForLocale(locale)
        val subtype: InputMethodSubtype = AdditionalSubtypeUtils.createDummyAdditionalSubtype(
            locale.toString(), keyboardLayoutName
        )
        val keyboardLayoutSet: KeyboardLayoutSet = createKeyboardSetForSpellChecker(subtype)
        return keyboardLayoutSet.getKeyboard(KeyboardId.ELEMENT_ALPHABET)
    }

    private fun createKeyboardSetForSpellChecker(subtype: InputMethodSubtype): KeyboardLayoutSet {
        val editorInfo: EditorInfo = EditorInfo()
        editorInfo.inputType = InputType.TYPE_CLASS_TEXT
        val builder: KeyboardLayoutSet.Builder = KeyboardLayoutSet.Builder(
            this, editorInfo
        )
        builder.setKeyboardGeometry(
            SPELLCHECKER_DUMMY_KEYBOARD_WIDTH, SPELLCHECKER_DUMMY_KEYBOARD_HEIGHT
        )
        builder.setSubtype(RichInputMethodSubtype.getRichInputMethodSubtype(subtype))
        builder.setIsSpellChecker(true /* isSpellChecker */)
        builder.disableTouchPositionCorrectionData()
        return builder.build()
    }

    companion object {
        private val TAG: String = AndroidSpellCheckerService::class.java.simpleName
        private const val DEBUG: Boolean = false

        const val PREF_USE_CONTACTS_KEY: String = "pref_spellcheck_use_contacts"

        private const val SPELLCHECKER_DUMMY_KEYBOARD_WIDTH: Int = 480
        private const val SPELLCHECKER_DUMMY_KEYBOARD_HEIGHT: Int = 301

        private const val DICTIONARY_NAME_PREFIX: String = "spellcheck_"

        private val EMPTY_STRING_ARRAY: Array<String?> = arrayOfNulls(0)

        const val SINGLE_QUOTE: String = "\u0027"
        const val APOSTROPHE: String = "\u2019"

        private fun getKeyboardLayoutNameForLocale(locale: Locale): String {
            // See b/19963288.
            if (locale.language == "sr") {
                return "south_slavic"
            }
            val script: Int = ScriptUtils.getScriptFromSpellCheckerLocale(locale)
            when (script) {
                ScriptUtils.SCRIPT_LATIN -> return "qwerty"
                ScriptUtils.SCRIPT_CYRILLIC -> return "east_slavic"
                ScriptUtils.SCRIPT_GREEK -> return "greek"
                ScriptUtils.SCRIPT_HEBREW -> return "hebrew"
                else -> throw RuntimeException("Wrong script supplied: $script")
            }
        }

        /**
         * Returns an empty SuggestionsInfo with flags signaling the word is not in the dictionary.
         * @param reportAsTypo whether this should include the flag LOOKS_LIKE_TYPO, for red underline.
         * @return the empty SuggestionsInfo with the appropriate flags set.
         */
        fun getNotInDictEmptySuggestions(reportAsTypo: Boolean): SuggestionsInfo {
            return SuggestionsInfo(
                if (reportAsTypo) SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO else 0,
                EMPTY_STRING_ARRAY
            )
        }

        val inDictEmptySuggestions: SuggestionsInfo
            /**
             * Returns an empty suggestionInfo with flags signaling the word is in the dictionary.
             * @return the empty SuggestionsInfo with the appropriate flags set.
             */
            get() = SuggestionsInfo(
                SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY,
                EMPTY_STRING_ARRAY
            )
    }
}
