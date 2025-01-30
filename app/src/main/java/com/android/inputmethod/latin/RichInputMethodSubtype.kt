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
package com.android.inputmethod.latin

import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.compat.BuildCompatUtils
import com.android.inputmethod.compat.InputMethodSubtypeCompatUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.Constants.Subtype.ExtraValue
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils
import ee.oyatl.ime.fusion.R
import java.util.Locale

/**
 * Enrichment class for InputMethodSubtype to enable concurrent multi-lingual input.
 *
 * Right now, this returns the extra value of its primary subtype.
 */
// non final for easy mocking.
class RichInputMethodSubtype(subtype: InputMethodSubtype) {
    // TODO: remove this method
    val rawSubtype: InputMethodSubtype

    val locale: Locale

    val originalLocale: Locale

    // Extra values are determined by the primary subtype. This is probably right, but
    // we may have to revisit this later.
    fun getExtraValueOf(key: String): String {
        return rawSubtype.getExtraValueOf(key)
    }

    val mode: String
        // The mode is also determined by the primary subtype.
        get() = rawSubtype.mode

    val isNoLanguage: Boolean
        get() = SubtypeLocaleUtils.NO_LANGUAGE == rawSubtype.locale

    val nameForLogging: String
        get() = toString()

    val fullDisplayName: String
        // InputMethodSubtype's display name for spacebar text in its locale.
        get() {
            if (isNoLanguage) return SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(rawSubtype)
            return SubtypeLocaleUtils.getSubtypeLocaleDisplayName(rawSubtype.locale)
        }

    val middleDisplayName: String
        // Get the RichInputMethodSubtype's middle display name in its locale.
        get() {
            if (isNoLanguage) return SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(rawSubtype)
            return SubtypeLocaleUtils.getSubtypeLanguageDisplayName(rawSubtype.locale)
        }

    override fun equals(o: Any?): Boolean {
        if (o !is RichInputMethodSubtype) {
            return false
        }
        val subtype: RichInputMethodSubtype = o
        return rawSubtype == subtype.rawSubtype && locale == subtype.locale
    }

    override fun hashCode(): Int {
        return rawSubtype.hashCode() + locale.hashCode()
    }

    override fun toString(): String {
        return "Multi-lingual subtype: $rawSubtype, $locale"
    }

    val isRtlSubtype: Boolean
        get() {
            // The subtype is considered RTL if the language of the main subtype is RTL.
            return LocaleUtils.isRtlLanguage(locale)
        }

    val keyboardLayoutSetName: String
        get() {
            return SubtypeLocaleUtils.getKeyboardLayoutSetName(rawSubtype)
        }

    init {
        rawSubtype = subtype
        originalLocale = InputMethodSubtypeCompatUtils.getLocaleObject(rawSubtype)
        locale = sLocaleMap.get(
            originalLocale
        ) ?: originalLocale
    }

    companion object {
        private val TAG: String = RichInputMethodSubtype::class.java.getSimpleName()

        private val sLocaleMap: HashMap<Locale, Locale> = initializeLocaleMap()
        private fun initializeLocaleMap(): HashMap<Locale, Locale> {
            val map: HashMap<Locale, Locale> = HashMap()
            if (BuildCompatUtils.EFFECTIVE_SDK_INT >= VERSION_CODES.LOLLIPOP) {
                // Locale#forLanguageTag is available on API Level 21+.
                // TODO: Remove this workaround once when we become able to deal with "sr-Latn".
                map.put(Locale.forLanguageTag("sr-Latn"), Locale("sr_ZZ"))
            }
            return map
        }

        fun getRichInputMethodSubtype(
            subtype: InputMethodSubtype?
        ): RichInputMethodSubtype {
            if (subtype == null) {
                return noLanguageSubtype
            } else {
                return RichInputMethodSubtype(subtype)
            }
        }

        // Placeholer for no language QWERTY subtype. See {@link R.xml.method}.
        private const val SUBTYPE_ID_OF_PLACEHOLDER_NO_LANGUAGE_SUBTYPE: Int = -0x221f402d
        private val EXTRA_VALUE_OF_PLACEHOLDER_NO_LANGUAGE_SUBTYPE: String =
            ("KeyboardLayoutSet=" + SubtypeLocaleUtils.QWERTY
                    + "," + ExtraValue.ASCII_CAPABLE
                    + "," + ExtraValue.ENABLED_WHEN_DEFAULT_IS_NOT_ASCII_CAPABLE
                    + "," + ExtraValue.EMOJI_CAPABLE)

        private val PLACEHOLDER_NO_LANGUAGE_SUBTYPE: RichInputMethodSubtype =
            RichInputMethodSubtype(
                InputMethodSubtypeCompatUtils.newInputMethodSubtype(
                    R.string.subtype_no_language_qwerty, R.drawable.ic_ime_switcher_dark,
                    SubtypeLocaleUtils.NO_LANGUAGE, Constants.Subtype.KEYBOARD_MODE,
                    EXTRA_VALUE_OF_PLACEHOLDER_NO_LANGUAGE_SUBTYPE,
                    false,  /* isAuxiliary */false,  /* overridesImplicitlyEnabledSubtype */
                    SUBTYPE_ID_OF_PLACEHOLDER_NO_LANGUAGE_SUBTYPE
                )!!
            )

        // Caveat: We probably should remove this when we add an Emoji subtype in {@link R.xml.method}.
        // Placeholder Emoji subtype. See {@link R.xml.method}.
        private const val SUBTYPE_ID_OF_PLACEHOLDER_EMOJI_SUBTYPE: Int = -0x2874d130
        private val EXTRA_VALUE_OF_PLACEHOLDER_EMOJI_SUBTYPE: String =
            ("KeyboardLayoutSet=" + SubtypeLocaleUtils.EMOJI
                    + "," + ExtraValue.EMOJI_CAPABLE)

        private val PLACEHOLDER_EMOJI_SUBTYPE: RichInputMethodSubtype = RichInputMethodSubtype(
            InputMethodSubtypeCompatUtils.newInputMethodSubtype(
                R.string.subtype_emoji, R.drawable.ic_ime_switcher_dark,
                SubtypeLocaleUtils.NO_LANGUAGE, Constants.Subtype.KEYBOARD_MODE,
                EXTRA_VALUE_OF_PLACEHOLDER_EMOJI_SUBTYPE,
                false,  /* isAuxiliary */false,  /* overridesImplicitlyEnabledSubtype */
                SUBTYPE_ID_OF_PLACEHOLDER_EMOJI_SUBTYPE
            )!!
        )
        private var sNoLanguageSubtype: RichInputMethodSubtype? = null
        private var sEmojiSubtype: RichInputMethodSubtype? = null

        val noLanguageSubtype: RichInputMethodSubtype
            get() {
                var noLanguageSubtype: RichInputMethodSubtype? =
                    sNoLanguageSubtype
                if (noLanguageSubtype == null) {
                    val rawNoLanguageSubtype: InputMethodSubtype? =
                        RichInputMethodManager.instance
                            .findSubtypeByLocaleAndKeyboardLayoutSet(
                                SubtypeLocaleUtils.NO_LANGUAGE, SubtypeLocaleUtils.QWERTY
                            )
                    if (rawNoLanguageSubtype != null) {
                        noLanguageSubtype = RichInputMethodSubtype(rawNoLanguageSubtype)
                    }
                }
                if (noLanguageSubtype != null) {
                    sNoLanguageSubtype = noLanguageSubtype
                    return noLanguageSubtype
                }
                Log.w(
                    TAG,
                    "Can't find any language with QWERTY subtype"
                )
                Log.w(
                    TAG,
                    "No input method subtype found; returning placeholder subtype: "
                            + PLACEHOLDER_NO_LANGUAGE_SUBTYPE
                )
                return PLACEHOLDER_NO_LANGUAGE_SUBTYPE
            }

        val emojiSubtype: RichInputMethodSubtype
            get() {
                var emojiSubtype: RichInputMethodSubtype? =
                    sEmojiSubtype
                if (emojiSubtype == null) {
                    val rawEmojiSubtype: InputMethodSubtype? =
                        RichInputMethodManager.instance
                            .findSubtypeByLocaleAndKeyboardLayoutSet(
                                SubtypeLocaleUtils.NO_LANGUAGE, SubtypeLocaleUtils.EMOJI
                            )
                    if (rawEmojiSubtype != null) {
                        emojiSubtype = RichInputMethodSubtype(rawEmojiSubtype)
                    }
                }
                if (emojiSubtype != null) {
                    sEmojiSubtype = emojiSubtype
                    return emojiSubtype
                }
                Log.w(TAG, "Can't find emoji subtype")
                Log.w(
                    TAG,
                    "No input method subtype found; returning placeholder subtype: "
                            + PLACEHOLDER_EMOJI_SUBTYPE
                )
                return PLACEHOLDER_EMOJI_SUBTYPE
            }
    }
}
