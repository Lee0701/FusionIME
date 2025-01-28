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
package com.android.inputmethod.latin

import android.text.TextUtils
import com.android.inputmethod.event.Event
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.define.DecoderSpecificConstants

/**
 * This class encapsulates data about a word previously composed, but that has been
 * committed already. This is used for resuming suggestion, and cancel auto-correction.
 */
class LastComposedWord(
    events: ArrayList<Event?>,
    inputPointers: InputPointers?, typedWord: String,
    committedWord: CharSequence, separatorString: String?,
    ngramContext: NgramContext?, capitalizedMode: Int
) {
    val mEvents: ArrayList<Event?>
    val mTypedWord: String
    val mCommittedWord: CharSequence
    val mSeparatorString: String?
    val mNgramContext: NgramContext?
    val mCapitalizedMode: Int
    val mInputPointers: InputPointers =
        InputPointers(DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH)

    private var mActive: Boolean

    // Warning: this is using the passed objects as is and fully expects them to be
    // immutable. Do not fiddle with their contents after you passed them to this constructor.
    init {
        if (inputPointers != null) {
            mInputPointers.copy(inputPointers)
        }
        mTypedWord = typedWord
        mEvents = ArrayList(events)
        mCommittedWord = committedWord
        mSeparatorString = separatorString
        mActive = true
        mNgramContext = ngramContext
        mCapitalizedMode = capitalizedMode
    }

    fun deactivate() {
        mActive = false
    }

    fun canRevertCommit(): Boolean {
        return mActive && !TextUtils.isEmpty(mCommittedWord) && !didCommitTypedWord()
    }

    private fun didCommitTypedWord(): Boolean {
        return TextUtils.equals(mTypedWord, mCommittedWord)
    }

    companion object {
        // COMMIT_TYPE_USER_TYPED_WORD is used when the word committed is the exact typed word, with
        // no hinting from the IME. It happens when some external event happens (rotating the device,
        // for example) or when auto-correction is off by settings or editor attributes.
        const val COMMIT_TYPE_USER_TYPED_WORD: Int = 0

        // COMMIT_TYPE_MANUAL_PICK is used when the user pressed a field in the suggestion strip.
        const val COMMIT_TYPE_MANUAL_PICK: Int = 1

        // COMMIT_TYPE_DECIDED_WORD is used when the IME commits the word it decided was best
        // for the current user input. It may be different from what the user typed (true auto-correct)
        // or it may be exactly what the user typed if it's in the dictionary or the IME does not have
        // enough confidence in any suggestion to auto-correct (auto-correct to typed word).
        const val COMMIT_TYPE_DECIDED_WORD: Int = 2

        // COMMIT_TYPE_CANCEL_AUTO_CORRECT is used upon committing back the old word upon cancelling
        // an auto-correction.
        const val COMMIT_TYPE_CANCEL_AUTO_CORRECT: Int = 3

        const val NOT_A_SEPARATOR: String = ""

        val NOT_A_COMPOSED_WORD: LastComposedWord = LastComposedWord(
            ArrayList<Event?>(), null, "", "",
            NOT_A_SEPARATOR, null, WordComposer.CAPS_MODE_OFF
        )
    }
}
