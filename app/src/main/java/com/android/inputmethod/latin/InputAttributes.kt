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

import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import com.android.inputmethod.latin.common.Constants.ImeOption
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.utils.InputTypeUtils

/**
 * Class to hold attributes of the input field.
 */
class InputAttributes(
    editorInfo: EditorInfo?, isFullscreenMode: Boolean,
    packageNameForPrivateImeOptions: String
) {
    private val TAG: String = InputAttributes::class.java.getSimpleName()

    val mTargetApplicationPackageName: String?
    var mInputTypeNoAutoCorrect: Boolean
    val mIsPasswordField: Boolean
    var mShouldShowSuggestions: Boolean
    var mApplicationSpecifiedCompletionOn: Boolean
    var mShouldInsertSpacesAutomatically: Boolean
    var mShouldShowVoiceInputKey: Boolean

    /**
     * Whether the floating gesture preview should be disabled. If true, this should override the
     * corresponding keyboard settings preference, always suppressing the floating preview text.
     * [com.android.inputmethod.latin.settings.SettingsValues.mGestureFloatingPreviewTextEnabled]
     */
    var mDisableGestureFloatingPreviewText: Boolean
    var mIsGeneralTextInput: Boolean
    private val mInputType: Int
    private val mEditorInfo: EditorInfo?
    private val mPackageNameForPrivateImeOptions: String

    init {
        mEditorInfo = editorInfo
        mPackageNameForPrivateImeOptions = packageNameForPrivateImeOptions
        mTargetApplicationPackageName = if (null != editorInfo) editorInfo.packageName else null
        val inputType: Int = if (null != editorInfo) editorInfo.inputType else 0
        val inputClass: Int = inputType and InputType.TYPE_MASK_CLASS
        mInputType = inputType
        mIsPasswordField = InputTypeUtils.isPasswordInputType(inputType)
                || InputTypeUtils.isVisiblePasswordInputType(inputType)
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            // If we are not looking at a TYPE_CLASS_TEXT field, the following strange
            // cases may arise, so we do a couple validity checks for them. If it's a
            // TYPE_CLASS_TEXT field, these special cases cannot happen, by construction
            // of the flags.
            if (null == editorInfo) {
                Log.w(TAG, "No editor info for this field. Bug?")
            } else if (InputType.TYPE_NULL == inputType) {
                // TODO: We should honor TYPE_NULL specification.
                Log.i(TAG, "InputType.TYPE_NULL is specified")
            } else if (inputClass == 0) {
                // TODO: is this check still necessary?
                Log.w(
                    TAG, String.format(
                        "Unexpected input class: inputType=0x%08x"
                                + " imeOptions=0x%08x", inputType, editorInfo.imeOptions
                    )
                )
            }
            mShouldShowSuggestions = false
            mInputTypeNoAutoCorrect = false
            mApplicationSpecifiedCompletionOn = false
            mShouldInsertSpacesAutomatically = false
            mShouldShowVoiceInputKey = false
            mDisableGestureFloatingPreviewText = false
            mIsGeneralTextInput = false
        } else {
            // inputClass == InputType.TYPE_CLASS_TEXT
            val variation: Int = inputType and InputType.TYPE_MASK_VARIATION
            val flagNoSuggestions: Boolean =
                0 != (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
            val flagMultiLine: Boolean =
                0 != (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            val flagAutoCorrect: Boolean =
                0 != (inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
            val flagAutoComplete: Boolean =
                0 != (inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE)

            // TODO: Have a helper method in InputTypeUtils
            // Make sure that passwords are not displayed in {@link SuggestionStripView}.
            val shouldSuppressSuggestions: Boolean = mIsPasswordField
                    || InputTypeUtils.isEmailVariation(variation)
                    || InputType.TYPE_TEXT_VARIATION_URI == variation || InputType.TYPE_TEXT_VARIATION_FILTER == variation || flagNoSuggestions
                    || flagAutoComplete
            mShouldShowSuggestions = !shouldSuppressSuggestions

            mShouldInsertSpacesAutomatically = InputTypeUtils.isAutoSpaceFriendlyType(inputType)

            val noMicrophone: Boolean = mIsPasswordField
                    || InputTypeUtils.isEmailVariation(variation)
                    || InputType.TYPE_TEXT_VARIATION_URI == variation || hasNoMicrophoneKeyOption()
            mShouldShowVoiceInputKey = !noMicrophone

            mDisableGestureFloatingPreviewText = inPrivateImeOptions(
                mPackageNameForPrivateImeOptions, ImeOption.NO_FLOATING_GESTURE_PREVIEW, editorInfo
            )

            // If it's a browser edit field and auto correct is not ON explicitly, then
            // disable auto correction, but keep suggestions on.
            // If NO_SUGGESTIONS is set, don't do prediction.
            // If it's not multiline and the autoCorrect flag is not set, then don't correct
            mInputTypeNoAutoCorrect =
                (variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT && !flagAutoCorrect)
                        || flagNoSuggestions
                        || (!flagAutoCorrect && !flagMultiLine)

            mApplicationSpecifiedCompletionOn = flagAutoComplete && isFullscreenMode

            // If we come here, inputClass is always TYPE_CLASS_TEXT
            mIsGeneralTextInput =
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != variation && InputType.TYPE_TEXT_VARIATION_PASSWORD != variation && InputType.TYPE_TEXT_VARIATION_PHONETIC != variation && InputType.TYPE_TEXT_VARIATION_URI != variation && InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != variation && InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS != variation && InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != variation
        }
    }

    val isTypeNull: Boolean
        get() {
            return InputType.TYPE_NULL == mInputType
        }

    fun isSameInputType(editorInfo: EditorInfo): Boolean {
        return editorInfo.inputType == mInputType
    }

    private fun hasNoMicrophoneKeyOption(): Boolean {
        @Suppress("deprecation") val deprecatedNoMicrophone: Boolean = inPrivateImeOptions(
            null, ImeOption.NO_MICROPHONE_COMPAT, mEditorInfo
        )
        val noMicrophone: Boolean = inPrivateImeOptions(
            mPackageNameForPrivateImeOptions, ImeOption.NO_MICROPHONE, mEditorInfo
        )
        return noMicrophone || deprecatedNoMicrophone
    }

    @Suppress("unused")
    private fun dumpFlags(inputType: Int) {
        val inputClass: Int = inputType and InputType.TYPE_MASK_CLASS
        val inputClassString: String = toInputClassString(inputClass)
        val variationString: String = toVariationString(
            inputClass, inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        val flagsString: String = toFlagsString(inputType and InputType.TYPE_MASK_FLAGS)
        Log.i(TAG, "Input class: " + inputClassString)
        Log.i(TAG, "Variation: " + variationString)
        Log.i(TAG, "Flags: " + flagsString)
    }

    // Pretty print
    override fun toString(): String {
        return String.format(
            "%s: inputType=0x%08x%s%s%s%s%s targetApp=%s\n", javaClass.getSimpleName(),
            mInputType,
            (if (mInputTypeNoAutoCorrect) " noAutoCorrect" else ""),
            (if (mIsPasswordField) " password" else ""),
            (if (mShouldShowSuggestions) " shouldShowSuggestions" else ""),
            (if (mApplicationSpecifiedCompletionOn) " appSpecified" else ""),
            (if (mShouldInsertSpacesAutomatically) " insertSpaces" else ""),
            mTargetApplicationPackageName
        )
    }

    companion object {
        private fun toInputClassString(inputClass: Int): String {
            when (inputClass) {
                InputType.TYPE_CLASS_TEXT -> return "TYPE_CLASS_TEXT"
                InputType.TYPE_CLASS_PHONE -> return "TYPE_CLASS_PHONE"
                InputType.TYPE_CLASS_NUMBER -> return "TYPE_CLASS_NUMBER"
                InputType.TYPE_CLASS_DATETIME -> return "TYPE_CLASS_DATETIME"
                else -> return String.format("unknownInputClass<0x%08x>", inputClass)
            }
        }

        private fun toVariationString(inputClass: Int, variation: Int): String {
            when (inputClass) {
                InputType.TYPE_CLASS_TEXT -> return toTextVariationString(variation)
                InputType.TYPE_CLASS_NUMBER -> return toNumberVariationString(variation)
                InputType.TYPE_CLASS_DATETIME -> return toDatetimeVariationString(variation)
                else -> return ""
            }
        }

        private fun toTextVariationString(variation: Int): String {
            when (variation) {
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> return " TYPE_TEXT_VARIATION_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> return "TYPE_TEXT_VARIATION_EMAIL_SUBJECT"
                InputType.TYPE_TEXT_VARIATION_FILTER -> return "TYPE_TEXT_VARIATION_FILTER"
                InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> return "TYPE_TEXT_VARIATION_LONG_MESSAGE"
                InputType.TYPE_TEXT_VARIATION_NORMAL -> return "TYPE_TEXT_VARIATION_NORMAL"
                InputType.TYPE_TEXT_VARIATION_PASSWORD -> return "TYPE_TEXT_VARIATION_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> return "TYPE_TEXT_VARIATION_PERSON_NAME"
                InputType.TYPE_TEXT_VARIATION_PHONETIC -> return "TYPE_TEXT_VARIATION_PHONETIC"
                InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> return "TYPE_TEXT_VARIATION_POSTAL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> return "TYPE_TEXT_VARIATION_SHORT_MESSAGE"
                InputType.TYPE_TEXT_VARIATION_URI -> return "TYPE_TEXT_VARIATION_URI"
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> return "TYPE_TEXT_VARIATION_VISIBLE_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> return "TYPE_TEXT_VARIATION_WEB_EDIT_TEXT"
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> return "TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> return "TYPE_TEXT_VARIATION_WEB_PASSWORD"
                else -> return String.format("unknownVariation<0x%08x>", variation)
            }
        }

        private fun toNumberVariationString(variation: Int): String {
            when (variation) {
                InputType.TYPE_NUMBER_VARIATION_NORMAL -> return "TYPE_NUMBER_VARIATION_NORMAL"
                InputType.TYPE_NUMBER_VARIATION_PASSWORD -> return "TYPE_NUMBER_VARIATION_PASSWORD"
                else -> return String.format("unknownVariation<0x%08x>", variation)
            }
        }

        private fun toDatetimeVariationString(variation: Int): String {
            when (variation) {
                InputType.TYPE_DATETIME_VARIATION_NORMAL -> return "TYPE_DATETIME_VARIATION_NORMAL"
                InputType.TYPE_DATETIME_VARIATION_DATE -> return "TYPE_DATETIME_VARIATION_DATE"
                InputType.TYPE_DATETIME_VARIATION_TIME -> return "TYPE_DATETIME_VARIATION_TIME"
                else -> return String.format("unknownVariation<0x%08x>", variation)
            }
        }

        private fun toFlagsString(flags: Int): String {
            val flagsArray: ArrayList<String> = ArrayList()
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)) flagsArray.add("TYPE_TEXT_FLAG_NO_SUGGESTIONS")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE)) flagsArray.add("TYPE_TEXT_FLAG_MULTI_LINE")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE)) flagsArray.add("TYPE_TEXT_FLAG_IME_MULTI_LINE")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_CAP_WORDS)) flagsArray.add("TYPE_TEXT_FLAG_CAP_WORDS")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)) flagsArray.add("TYPE_TEXT_FLAG_CAP_SENTENCES")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) flagsArray.add("TYPE_TEXT_FLAG_CAP_CHARACTERS")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)) flagsArray.add("TYPE_TEXT_FLAG_AUTO_CORRECT")
            if (0 != (flags and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE)) flagsArray.add("TYPE_TEXT_FLAG_AUTO_COMPLETE")
            return if (flagsArray.isEmpty()) "" else flagsArray.toTypedArray().contentToString()
        }

        fun inPrivateImeOptions(
            packageName: String?, key: String,
            editorInfo: EditorInfo?
        ): Boolean {
            if (editorInfo == null) return false
            val findingKey: String = if ((packageName != null)) "$packageName.$key" else key
            return StringUtils.containsInCommaSplittableText(
                findingKey,
                editorInfo.privateImeOptions
            )
        }
    }
}
