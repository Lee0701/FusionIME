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
package com.android.inputmethod.latin

import android.util.Log
import com.android.inputmethod.latin.DictionaryCollection
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Class for a collection of dictionaries that behave like one dictionary.
 */
class DictionaryCollection : Dictionary {
    private val TAG: String = DictionaryCollection::class.java.getSimpleName()
    protected val mDictionaries: CopyOnWriteArrayList<Dictionary>

    constructor(dictType: String, locale: Locale?) : super(dictType, locale) {
        mDictionaries = CopyOnWriteArrayList()
    }

    constructor(
        dictType: String, locale: Locale?,
        vararg dictionaries: Dictionary
    ) : super(dictType, locale) {
        if (null == dictionaries) {
            mDictionaries = CopyOnWriteArrayList()
        } else {
            mDictionaries = CopyOnWriteArrayList(dictionaries)
            mDictionaries.removeAll(setOf<Any?>(null))
        }
    }

    constructor(
        dictType: String, locale: Locale?,
        dictionaries: Collection<Dictionary>
    ) : super(dictType, locale) {
        mDictionaries = CopyOnWriteArrayList(dictionaries)
        mDictionaries.removeAll(setOf<Any?>(null))
    }

    override fun getSuggestions(
        composedData: ComposedData,
        ngramContext: NgramContext, proximityInfoHandle: Long,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        sessionId: Int, weightForLocale: Float,
        inOutWeightOfLangModelVsSpatialModel: FloatArray?
    ): ArrayList<SuggestedWordInfo?>? {
        val dictionaries: CopyOnWriteArrayList<Dictionary> = mDictionaries
        if (dictionaries.isEmpty()) return null
        // To avoid creating unnecessary objects, we get the list out of the first
        // dictionary and add the rest to it if not null, hence the get(0)
        var suggestions: ArrayList<SuggestedWordInfo?>? = dictionaries.get(0).getSuggestions(
            composedData,
            ngramContext, proximityInfoHandle, settingsValuesForSuggestion, sessionId,
            weightForLocale, inOutWeightOfLangModelVsSpatialModel
        )
        if (null == suggestions) suggestions = ArrayList()
        val length: Int = dictionaries.size
        for (i in 1 until length) {
            val sugg: ArrayList<SuggestedWordInfo?>? = dictionaries.get(i).getSuggestions(
                composedData, ngramContext, proximityInfoHandle, settingsValuesForSuggestion,
                sessionId, weightForLocale, inOutWeightOfLangModelVsSpatialModel
            )
            if (null != sugg) suggestions.addAll(sugg)
        }
        return suggestions
    }

    override fun isInDictionary(word: String?): Boolean {
        for (i in mDictionaries.indices.reversed()) if (mDictionaries.get(i)
                .isInDictionary(word)
        ) return true
        return false
    }

    override fun getFrequency(word: String?): Int {
        var maxFreq: Int = -1
        for (i in mDictionaries.indices.reversed()) {
            val tempFreq: Int = mDictionaries.get(i).getFrequency(word)
            maxFreq = max(tempFreq.toDouble(), maxFreq.toDouble()).toInt()
        }
        return maxFreq
    }

    override fun getMaxFrequencyOfExactMatches(word: String?): Int {
        var maxFreq: Int = -1
        for (i in mDictionaries.indices.reversed()) {
            val tempFreq: Int = mDictionaries.get(i).getMaxFrequencyOfExactMatches(word)
            maxFreq = max(tempFreq.toDouble(), maxFreq.toDouble()).toInt()
        }
        return maxFreq
    }

    override val isInitialized: Boolean
        get() {
            return !mDictionaries.isEmpty()
        }

    override fun close() {
        for (dict: Dictionary in mDictionaries) dict.close()
    }

    // Warning: this is not thread-safe. Take necessary precaution when calling.
    fun addDictionary(newDict: Dictionary?) {
        if (null == newDict) return
        if (mDictionaries.contains(newDict)) {
            Log.w(TAG, "This collection already contains this dictionary: " + newDict)
        }
        mDictionaries.add(newDict)
    }

    // Warning: this is not thread-safe. Take necessary precaution when calling.
    fun removeDictionary(dict: Dictionary) {
        if (mDictionaries.contains(dict)) {
            mDictionaries.remove(dict)
        } else {
            Log.w(TAG, "This collection does not contain this dictionary: " + dict)
        }
    }
}
