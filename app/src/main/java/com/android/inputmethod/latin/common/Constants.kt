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
package com.android.inputmethod.latin.common

import com.android.inputmethod.annotations.UsedForTesting

object Constants {
    const val NOT_A_CODE: Int = -1
    const val NOT_A_CURSOR_POSITION: Int = -1

    // TODO: replace the following constants with state in InputTransaction?
    const val NOT_A_COORDINATE: Int = -1
    const val SUGGESTION_STRIP_COORDINATE: Int = -2
    const val EXTERNAL_KEYBOARD_COORDINATE: Int = -4

    // A hint on how many characters to cache from the TextView. A good value of this is given by
    // how many characters we need to be able to almost always find the caps mode.
    const val EDITOR_CONTENTS_CACHE_SIZE: Int = 1024

    // How many characters we accept for the recapitalization functionality. This needs to be
    // large enough for all reasonable purposes, but avoid purposeful attacks. 100k sounds about
    // right for this.
    const val MAX_CHARACTERS_FOR_RECAPITALIZATION: Int = 1024 * 100

    // Key events coming any faster than this are long-presses.
    const val LONG_PRESS_MILLISECONDS: Int = 200

    // TODO: Set this value appropriately.
    const val GET_SUGGESTED_WORDS_TIMEOUT: Int = 200

    // How many continuous deletes at which to start deleting at a higher speed.
    const val DELETE_ACCELERATE_AT: Int = 20

    const val WORD_SEPARATOR: String = " "

    fun isValidCoordinate(coordinate: Int): Boolean {
        // Detect {@link NOT_A_COORDINATE}, {@link SUGGESTION_STRIP_COORDINATE},
        // and {@link SPELL_CHECKER_COORDINATE}.
        return coordinate >= 0
    }

    /**
     * Custom request code used in
     * [com.android.inputmethod.keyboard.KeyboardActionListener.onCustomRequest].
     */
    // The code to show input method picker.
    const val CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER: Int = 1

    /**
     * Some common keys code. Must be positive.
     */
    const val CODE_ENTER: Int = '\n'.code
    const val CODE_TAB: Int = '\t'.code
    const val CODE_SPACE: Int = ' '.code
    const val CODE_PERIOD: Int = '.'.code
    const val CODE_COMMA: Int = ','.code
    const val CODE_DASH: Int = '-'.code
    const val CODE_SINGLE_QUOTE: Int = '\''.code
    const val CODE_DOUBLE_QUOTE: Int = '"'.code
    const val CODE_SLASH: Int = '/'.code
    const val CODE_BACKSLASH: Int = '\\'.code
    const val CODE_VERTICAL_BAR: Int = '|'.code
    const val CODE_COMMERCIAL_AT: Int = '@'.code
    const val CODE_PLUS: Int = '+'.code
    const val CODE_PERCENT: Int = '%'.code
    const val CODE_CLOSING_PARENTHESIS: Int = ')'.code
    const val CODE_CLOSING_SQUARE_BRACKET: Int = ']'.code
    const val CODE_CLOSING_CURLY_BRACKET: Int = '}'.code
    const val CODE_CLOSING_ANGLE_BRACKET: Int = '>'.code
    const val CODE_INVERTED_QUESTION_MARK: Int = 0xBF // ¿
    const val CODE_INVERTED_EXCLAMATION_MARK: Int = 0xA1 // ¡
    const val CODE_GRAVE_ACCENT: Int = '`'.code
    const val CODE_CIRCUMFLEX_ACCENT: Int = '^'.code
    const val CODE_TILDE: Int = '~'.code

    const val REGEXP_PERIOD: String = "\\."
    const val STRING_SPACE: String = " "

    /**
     * Special keys code. Must be negative.
     * These should be aligned with constants in
     * [com.android.inputmethod.keyboard.internal.KeyboardCodesSet].
     */
    const val CODE_SHIFT: Int = -1
    const val CODE_CAPSLOCK: Int = -2
    const val CODE_SWITCH_ALPHA_SYMBOL: Int = -3
    const val CODE_OUTPUT_TEXT: Int = -4
    const val CODE_DELETE: Int = -5
    const val CODE_SETTINGS: Int = -6
    const val CODE_SHORTCUT: Int = -7
    const val CODE_ACTION_NEXT: Int = -8
    const val CODE_ACTION_PREVIOUS: Int = -9
    const val CODE_LANGUAGE_SWITCH: Int = -10
    const val CODE_EMOJI: Int = -11
    const val CODE_SHIFT_ENTER: Int = -12
    const val CODE_SYMBOL_SHIFT: Int = -13
    const val CODE_ALPHA_FROM_EMOJI: Int = -14

    // Code value representing the code is not specified.
    const val CODE_UNSPECIFIED: Int = -15

    fun isLetterCode(code: Int): Boolean {
        return code >= CODE_SPACE
    }

    fun printableCode(code: Int): String {
        when (code) {
            CODE_SHIFT -> return "shift"
            CODE_CAPSLOCK -> return "capslock"
            CODE_SWITCH_ALPHA_SYMBOL -> return "symbol"
            CODE_OUTPUT_TEXT -> return "text"
            CODE_DELETE -> return "delete"
            CODE_SETTINGS -> return "settings"
            CODE_SHORTCUT -> return "shortcut"
            CODE_ACTION_NEXT -> return "actionNext"
            CODE_ACTION_PREVIOUS -> return "actionPrevious"
            CODE_LANGUAGE_SWITCH -> return "languageSwitch"
            CODE_EMOJI -> return "emoji"
            CODE_SHIFT_ENTER -> return "shiftEnter"
            CODE_ALPHA_FROM_EMOJI -> return "alpha"
            CODE_UNSPECIFIED -> return "unspec"
            CODE_TAB -> return "tab"
            CODE_ENTER -> return "enter"
            CODE_SPACE -> return "space"
            else -> {
                if (code < CODE_SPACE) return String.format("\\u%02X", code)
                if (code < 0x100) return String.format("%c", code)
                if (code < 0x10000) return String.format("\\u%04X", code)
                return String.format("\\U%05X", code)
            }
        }
    }

    fun printableCodes(codes: IntArray): String {
        val sb = StringBuilder()
        var addDelimiter = false
        for (code in codes) {
            if (code == NOT_A_CODE) break
            if (addDelimiter) sb.append(", ")
            sb.append(printableCode(code))
            addDelimiter = true
        }
        return "[$sb]"
    }

    /**
     * Screen metrics (a.k.a. Device form factor) constants of
     * [com.android.inputmethod.latin.R.integer.config_screen_metrics].
     */
    const val SCREEN_METRICS_SMALL_PHONE: Int = 0
    const val SCREEN_METRICS_LARGE_PHONE: Int = 1
    const val SCREEN_METRICS_LARGE_TABLET: Int = 2
    const val SCREEN_METRICS_SMALL_TABLET: Int = 3

    @UsedForTesting
    fun isPhone(screenMetrics: Int): Boolean {
        return screenMetrics == SCREEN_METRICS_SMALL_PHONE
                || screenMetrics == SCREEN_METRICS_LARGE_PHONE
    }

    @UsedForTesting
    fun isTablet(screenMetrics: Int): Boolean {
        return screenMetrics == SCREEN_METRICS_SMALL_TABLET
                || screenMetrics == SCREEN_METRICS_LARGE_TABLET
    }

    /**
     * Default capacity of gesture points container.
     * This constant is used by [com.android.inputmethod.keyboard.internal.BatchInputArbiter]
     * and etc. to preallocate regions that contain gesture event points.
     */
    const val DEFAULT_GESTURE_POINTS_CAPACITY: Int = 128

    const val MAX_IME_DECODER_RESULTS: Int = 20
    const val DECODER_SCORE_SCALAR: Int = 1000000
    const val DECODER_MAX_SCORE: Int = 1000000000

    const val EVENT_BACKSPACE: Int = 1
    const val EVENT_REJECTION: Int = 2
    const val EVENT_REVERT: Int = 3

    object Color {
        /**
         * The alpha value for fully opaque.
         */
        const val ALPHA_OPAQUE: Int = 255
    }

    object ImeOption {
        /**
         * The private IME option used to indicate that no microphone should be shown for a given
         * text field. For instance, this is specified by the search dialog when the dialog is
         * already showing a voice search button.
         *
         */
        @Deprecated("Use {@link ImeOption#NO_MICROPHONE} with package name prefixed.")
        const val NO_MICROPHONE_COMPAT: String = "nm"

        /**
         * The private IME option used to indicate that no microphone should be shown for a given
         * text field. For instance, this is specified by the search dialog when the dialog is
         * already showing a voice search button.
         */
        const val NO_MICROPHONE: String = "noMicrophoneKey"

        /**
         * The private IME option used to indicate that no settings key should be shown for a given
         * text field.
         */
        const val NO_SETTINGS_KEY: String = "noSettingsKey"

        /**
         * The private IME option used to indicate that the given text field needs ASCII code points
         * input.
         *
         */
        @Deprecated("Use EditorInfo#IME_FLAG_FORCE_ASCII.")
        const val FORCE_ASCII: String = "forceAscii"

        /**
         * The private IME option used to suppress the floating gesture preview for a given text
         * field. This overrides the corresponding keyboard settings preference.
         * [com.android.inputmethod.latin.settings.SettingsValues.mGestureFloatingPreviewTextEnabled]
         */
        const val NO_FLOATING_GESTURE_PREVIEW: String = "noGestureFloatingPreview"
    }

    object Subtype {
        /**
         * The subtype mode used to indicate that the subtype is a keyboard.
         */
        const val KEYBOARD_MODE: String = "keyboard"

        object ExtraValue {
            /**
             * The subtype extra value used to indicate that this subtype is capable of
             * entering ASCII characters.
             */
            const val ASCII_CAPABLE: String = "AsciiCapable"

            /**
             * The subtype extra value used to indicate that this subtype is enabled
             * when the default subtype is not marked as ascii capable.
             */
            const val ENABLED_WHEN_DEFAULT_IS_NOT_ASCII_CAPABLE: String =
                "EnabledWhenDefaultIsNotAsciiCapable"

            /**
             * The subtype extra value used to indicate that this subtype is capable of
             * entering emoji characters.
             */
            const val EMOJI_CAPABLE: String = "EmojiCapable"

            /**
             * The subtype extra value used to indicate that this subtype requires a network
             * connection to work.
             */
            const val REQ_NETWORK_CONNECTIVITY: String = "requireNetworkConnectivity"

            /**
             * The subtype extra value used to indicate that the display name of this subtype
             * contains a "%s" for printf-like replacement and it should be replaced by
             * this extra value.
             * This extra value is supported on JellyBean and later.
             */
            const val UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME: String =
                "UntranslatableReplacementStringInSubtypeName"

            /**
             * The subtype extra value used to indicate this subtype keyboard layout set name.
             * This extra value is private to LatinIME.
             */
            const val KEYBOARD_LAYOUT_SET: String = "KeyboardLayoutSet"

            /**
             * The subtype extra value used to indicate that this subtype is an additional subtype
             * that the user defined. This extra value is private to LatinIME.
             */
            const val IS_ADDITIONAL_SUBTYPE: String = "isAdditionalSubtype"

            /**
             * The subtype extra value used to specify the combining rules.
             */
            const val COMBINING_RULES: String = "CombiningRules"
        }
    }

    object TextUtils {
        /**
         * Capitalization mode for [android.text.TextUtils.getCapsMode]: don't capitalize
         * characters.  This value may be used with
         * [android.text.TextUtils.CAP_MODE_CHARACTERS],
         * [android.text.TextUtils.CAP_MODE_WORDS], and
         * [android.text.TextUtils.CAP_MODE_SENTENCES].
         */
        // TODO: Straighten this out. It's bizarre to have to use android.text.TextUtils.CAP_MODE_*
        // except for OFF that is in Constants.TextUtils.
        const val CAP_MODE_OFF: Int = 0
    }
}
