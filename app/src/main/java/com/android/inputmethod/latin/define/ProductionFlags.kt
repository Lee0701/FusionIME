/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.inputmethod.latin.define

import com.android.inputmethod.latin.personalization.UserHistoryDictionary

object ProductionFlags {
    const val IS_HARDWARE_KEYBOARD_SUPPORTED: Boolean = false

    /**
     * Include all suggestions from all dictionaries in
     * [com.android.inputmethod.latin.SuggestedWords.mRawSuggestions].
     */
    const val INCLUDE_RAW_SUGGESTIONS: Boolean = false

    /**
     * When false, the metrics logging is not yet ready to be enabled.
     */
    const val IS_METRICS_LOGGING_SUPPORTED: Boolean = false

    /**
     * When `false`, the split keyboard is not yet ready to be enabled.
     */
    const val IS_SPLIT_KEYBOARD_SUPPORTED: Boolean = true

    /**
     * When `false`, account sign-in in keyboard is not yet ready to be enabled.
     */
    const val ENABLE_ACCOUNT_SIGN_IN: Boolean = false

    /**
     * When `true`, user history dictionary sync feature is ready to be enabled.
     */
    const val ENABLE_USER_HISTORY_DICTIONARY_SYNC: Boolean = ENABLE_ACCOUNT_SIGN_IN && false

    /**
     * When `true`, the IME maintains per account [UserHistoryDictionary].
     */
    const val ENABLE_PER_ACCOUNT_USER_HISTORY_DICTIONARY: Boolean = ENABLE_ACCOUNT_SIGN_IN && false
}
