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
package com.android.inputmethod.latin

import android.content.Context
import android.util.LruCache
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.spellcheck.AndroidSpellCheckerService
import com.android.inputmethod.latin.utils.SuggestionResults
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Interface that facilitates interaction with different kinds of dictionaries. Provides APIs to
 * instantiate and select the correct dictionaries (based on language or account), update entries
 * and fetch suggestions. Currently AndroidSpellCheckerService and LatinIME both use
 * DictionaryFacilitator as a client for interacting with dictionaries.
 */
interface DictionaryFacilitator {
    /**
     * The facilitator will put words into the cache whenever it decodes them.
     * @param cache
     */
    fun setValidSpellingWordReadCache(cache: LruCache<String?, Boolean>?)

    /**
     * The facilitator will get words from the cache whenever it needs to check their spelling.
     * @param cache
     */
    fun setValidSpellingWordWriteCache(cache: LruCache<String, Boolean>?)

    /**
     * Returns whether this facilitator is exactly for this locale.
     *
     * @param locale the locale to test against
     */
    fun isForLocale(locale: Locale?): Boolean

    /**
     * Returns whether this facilitator is exactly for this account.
     *
     * @param account the account to test against.
     */
    fun isForAccount(account: String?): Boolean

    interface DictionaryInitializationListener {
        fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean)
    }

    /**
     * Called every time [LatinIME] starts on a new text field.
     * Dot not affect [AndroidSpellCheckerService].
     *
     * WARNING: The service methods that call start/finish are very spammy.
     */
    fun onStartInput()

    /**
     * Called every time the [LatinIME] finishes with the current text field.
     * May be followed by [.onStartInput] again in another text field,
     * or it may be done for a while.
     * Dot not affect [AndroidSpellCheckerService].
     *
     * WARNING: The service methods that call start/finish are very spammy.
     */
    fun onFinishInput(context: Context?)

    val isActive: Boolean

    val locale: Locale

    fun usesContacts(): Boolean

    val account: String?

    fun resetDictionaries(
        context: Context,
        newLocale: Locale,
        useContactsDict: Boolean,
        usePersonalizedDicts: Boolean,
        forceReloadMainDictionary: Boolean,
        account: String?,
        dictNamePrefix: String,
        listener: DictionaryInitializationListener?
    )

    @UsedForTesting
    fun resetDictionariesForTesting(
        context: Context,
        locale: Locale,
        dictionaryTypes: ArrayList<String>,
        dictionaryFiles: HashMap<String?, File?>,
        additionalDictAttributes: Map<String?, Map<String?, String?>?>,
        account: String?
    )

    fun closeDictionaries()

    @UsedForTesting
    fun getSubDictForTesting(dictName: String): ExpandableBinaryDictionary?

    // The main dictionaries are loaded asynchronously. Don't cache the return value
    // of these methods.
    fun hasAtLeastOneInitializedMainDictionary(): Boolean

    fun hasAtLeastOneUninitializedMainDictionary(): Boolean

    @Throws(InterruptedException::class)
    fun waitForLoadingMainDictionaries(timeout: Long, unit: TimeUnit?)

    @UsedForTesting
    @Throws(InterruptedException::class)
    fun waitForLoadingDictionariesForTesting(timeout: Long, unit: TimeUnit?)

    fun addToUserHistory(
        suggestion: String, wasAutoCapitalized: Boolean,
        ngramContext: NgramContext, timeStampInSeconds: Long,
        blockPotentiallyOffensive: Boolean
    )

    fun unlearnFromUserHistory(
        word: String,
        ngramContext: NgramContext, timeStampInSeconds: Long,
        eventType: Int
    )

    // TODO: Revise the way to fusion suggestion results.
    fun getSuggestionResults(
        composedData: ComposedData,
        ngramContext: NgramContext, keyboard: Keyboard,
        settingsValuesForSuggestion: SettingsValuesForSuggestion, sessionId: Int,
        inputStyle: Int
    ): SuggestionResults

    fun isValidSpellingWord(word: String): Boolean

    fun isValidSuggestionWord(word: String): Boolean

    fun clearUserHistoryDictionary(context: Context?): Boolean

    fun dump(context: Context?): String

    fun dumpDictionaryForDebug(dictName: String)

    fun getDictionaryStats(context: Context?): List<DictionaryStats?>

    companion object {
        val ALL_DICTIONARY_TYPES: Array<String> = arrayOf<String>(
            Dictionary.TYPE_MAIN,
            Dictionary.TYPE_CONTACTS,
            Dictionary.TYPE_USER_HISTORY,
            Dictionary.TYPE_USER
        )

        val DYNAMIC_DICTIONARY_TYPES: Array<String> = arrayOf<String>(
            Dictionary.TYPE_CONTACTS,
            Dictionary.TYPE_USER_HISTORY,
            Dictionary.TYPE_USER
        )
    }
}
