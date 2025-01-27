/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.latin.touchinputconsumer

import android.view.inputmethod.EditorInfo
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.inputlogic.PrivateCommandPerformer
import java.util.Locale

/**
 * Stub for GestureConsumer.
 * <br></br>
 * The methods of this class should only be called from a single thread, e.g.,
 * the UI Thread.
 */
@Suppress("unused")
class GestureConsumer private constructor() {
    fun willConsume(): Boolean {
        return false
    }

    fun onInit(locale: Locale?, keyboard: Keyboard?) {
    }

    fun onGestureStarted(locale: Locale?, keyboard: Keyboard?) {
    }

    fun onGestureCanceled() {
    }

    fun onGestureCompleted(inputPointers: InputPointers?) {
    }

    fun onImeSuggestionsProcessed(
        suggestedWords: SuggestedWords?,
        composingStart: Int, composingLength: Int,
        dictionaryFacilitator: DictionaryFacilitator?
    ) {
    }

    companion object {
        val NULL_GESTURE_CONSUMER: GestureConsumer = GestureConsumer()

        fun newInstance(
            editorInfo: EditorInfo?, commandPerformer: PrivateCommandPerformer?,
            locale: Locale?, keyboard: Keyboard?
        ): GestureConsumer {
            return NULL_GESTURE_CONSUMER
        }
    }
}
