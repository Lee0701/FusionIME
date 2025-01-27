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
package com.android.inputmethod.latin.utils

import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.settings.SettingsValues

@Suppress("unused")
object StatsUtils {
    fun onCreate(
        settingsValues: SettingsValues?,
        richImm: RichInputMethodManager?
    ) {
    }

    fun onPickSuggestionManually(
        suggestedWords: SuggestedWords?,
        suggestionInfo: SuggestedWordInfo?,
        dictionaryFacilitator: DictionaryFacilitator?
    ) {
    }

    fun onBackspaceWordDelete(wordLength: Int) {
    }

    fun onBackspacePressed(lengthToDelete: Int) {
    }

    fun onBackspaceSelectedText(selectedTextLength: Int) {
    }

    fun onDeleteMultiCharInput(multiCharLength: Int) {
    }

    fun onRevertAutoCorrect() {
    }

    fun onRevertDoubleSpacePeriod() {
    }

    fun onRevertSwapPunctuation() {
    }

    fun onFinishInputView() {
    }

    fun onCreateInputView() {
    }

    fun onStartInputView(inputType: Int, displayOrientation: Int, restarting: Boolean) {
    }

    fun onAutoCorrection(
        typedWord: String?, autoCorrectionWord: String?,
        isBatchInput: Boolean, dictionaryFacilitator: DictionaryFacilitator?,
        prevWordsContext: String?
    ) {
    }

    fun onWordCommitUserTyped(commitWord: String?, isBatchMode: Boolean) {
    }

    fun onWordCommitAutoCorrect(commitWord: String?, isBatchMode: Boolean) {
    }

    fun onWordCommitSuggestionPickedManually(
        commitWord: String?, isBatchMode: Boolean
    ) {
    }

    fun onDoubleSpacePeriod() {
    }

    fun onLoadSettings(settingsValues: SettingsValues?) {
    }

    fun onInvalidWordIdentification(invalidWord: String?) {
    }

    fun onSubtypeChanged(
        oldSubtype: InputMethodSubtype?,
        newSubtype: InputMethodSubtype?
    ) {
    }

    fun onSettingsActivity(entryPoint: String?) {
    }

    fun onInputConnectionLaggy(operation: Int, duration: Long) {
    }

    fun onDecoderLaggy(operation: Int, duration: Long) {
    }
}
