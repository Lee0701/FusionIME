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
package com.android.inputmethod.latin.personalization

import android.content.Context
import android.util.Log
import com.android.inputmethod.latin.common.FileUtils
import java.io.File
import java.io.FilenameFilter
import java.lang.ref.SoftReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Helps handle and manage personalized dictionaries such as [UserHistoryDictionary].
 */
object PersonalizationHelper {
    private val TAG: String = PersonalizationHelper::class.java.getSimpleName()
    private const val DEBUG: Boolean = false

    private val sLangUserHistoryDictCache: ConcurrentHashMap<String, SoftReference<UserHistoryDictionary>?> =
        ConcurrentHashMap()

    fun getUserHistoryDictionary(
        context: Context, locale: Locale, accountName: String?
    ): UserHistoryDictionary {
        var lookupStr: String = locale.toString()
        if (accountName != null) {
            lookupStr += "." + accountName
        }
        synchronized(sLangUserHistoryDictCache) {
            if (sLangUserHistoryDictCache.containsKey(lookupStr)) {
                val ref: SoftReference<UserHistoryDictionary>? =
                    sLangUserHistoryDictCache.get(lookupStr)
                val dict: UserHistoryDictionary? = if (ref == null) null else ref.get()
                if (dict != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Use cached UserHistoryDictionary with lookup: " + lookupStr)
                    }
                    dict.reloadDictionaryIfRequired()
                    return dict
                }
            }
            val dict: UserHistoryDictionary = UserHistoryDictionary(
                context, locale, accountName
            )
            sLangUserHistoryDictCache.put(lookupStr, SoftReference(dict))
            return dict
        }
    }

    fun removeAllUserHistoryDictionaries(context: Context) {
        synchronized(sLangUserHistoryDictCache) {
            for (entry: Map.Entry<String, SoftReference<UserHistoryDictionary>?>
            in sLangUserHistoryDictCache.entries) {
                if (entry.value != null) {
                    val dict: UserHistoryDictionary? = entry.value!!.get()
                    if (dict != null) {
                        dict.clear()
                    }
                }
            }
            sLangUserHistoryDictCache.clear()
            val filesDir: File? = context.getFilesDir()
            if (filesDir == null) {
                Log.e(TAG, "context.getFilesDir() returned null.")
                return
            }
            val filesDeleted: Boolean = FileUtils.deleteFilteredFiles(
                filesDir, DictFilter(UserHistoryDictionary.NAME)
            )
            if (!filesDeleted) {
                Log.e(
                    TAG, ("Cannot remove dictionary files. filesDir: " + filesDir.getAbsolutePath()
                            + ", dictNamePrefix: " + UserHistoryDictionary.NAME)
                )
            }
        }
    }

    private class DictFilter(name: String) : FilenameFilter {
        private val mName: String

        init {
            mName = name
        }

        override fun accept(dir: File, name: String): Boolean {
            return name.startsWith(mName)
        }
    }
}
