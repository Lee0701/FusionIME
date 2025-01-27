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
package com.android.inputmethod.latin.utils

import android.text.InputType
import android.view.inputmethod.EditorInfo

object InputTypeUtils : InputType {
    private const val WEB_TEXT_PASSWORD_INPUT_TYPE =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    private const val WEB_TEXT_EMAIL_ADDRESS_INPUT_TYPE =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    private const val NUMBER_PASSWORD_INPUT_TYPE =
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    private const val TEXT_PASSWORD_INPUT_TYPE =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    private const val TEXT_VISIBLE_PASSWORD_INPUT_TYPE =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    private val SUPPRESSING_AUTO_SPACES_FIELD_VARIATION = intArrayOf(
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    )
    const val IME_ACTION_CUSTOM_LABEL: Int = EditorInfo.IME_MASK_ACTION + 1

    private fun isWebEditTextInputType(inputType: Int): Boolean {
        return inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)
    }

    private fun isWebPasswordInputType(inputType: Int): Boolean {
        return WEB_TEXT_PASSWORD_INPUT_TYPE != 0
                && inputType == WEB_TEXT_PASSWORD_INPUT_TYPE
    }

    private fun isWebEmailAddressInputType(inputType: Int): Boolean {
        return WEB_TEXT_EMAIL_ADDRESS_INPUT_TYPE != 0
                && inputType == WEB_TEXT_EMAIL_ADDRESS_INPUT_TYPE
    }

    private fun isNumberPasswordInputType(inputType: Int): Boolean {
        return NUMBER_PASSWORD_INPUT_TYPE != 0
                && inputType == NUMBER_PASSWORD_INPUT_TYPE
    }

    private fun isTextPasswordInputType(inputType: Int): Boolean {
        return inputType == TEXT_PASSWORD_INPUT_TYPE
    }

    private fun isWebEmailAddressVariation(variation: Int): Boolean {
        return variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    fun isEmailVariation(variation: Int): Boolean {
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || isWebEmailAddressVariation(variation)
    }

    fun isWebInputType(inputType: Int): Boolean {
        val maskedInputType =
            inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
        return isWebEditTextInputType(maskedInputType) || isWebPasswordInputType(maskedInputType)
                || isWebEmailAddressInputType(maskedInputType)
    }

    // Please refer to TextView.isPasswordInputType
    fun isPasswordInputType(inputType: Int): Boolean {
        val maskedInputType =
            inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
        return isTextPasswordInputType(maskedInputType) || isWebPasswordInputType(maskedInputType)
                || isNumberPasswordInputType(maskedInputType)
    }

    // Please refer to TextView.isVisiblePasswordInputType
    fun isVisiblePasswordInputType(inputType: Int): Boolean {
        val maskedInputType =
            inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
        return maskedInputType == TEXT_VISIBLE_PASSWORD_INPUT_TYPE
    }

    fun isAutoSpaceFriendlyType(inputType: Int): Boolean {
        if (InputType.TYPE_CLASS_TEXT != (InputType.TYPE_MASK_CLASS and inputType)) return false
        val variation = InputType.TYPE_MASK_VARIATION and inputType
        for (fieldVariation in SUPPRESSING_AUTO_SPACES_FIELD_VARIATION) {
            if (variation == fieldVariation) return false
        }
        return true
    }

    fun getImeOptionsActionIdFromEditorInfo(editorInfo: EditorInfo): Int {
        return if ((editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            EditorInfo.IME_ACTION_NONE
        } else if (editorInfo.actionLabel != null) {
            IME_ACTION_CUSTOM_LABEL
        } else {
            // Note: this is different from editorInfo.actionId, hence "ImeOptionsActionId"
            editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
        }
    }
}
