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

import java.util.Locale
import java.util.TreeMap

/**
 * A class to help with handling different writing scripts.
 */
object ScriptUtils {
    // Used for hardware keyboards
    const val SCRIPT_UNKNOWN: Int = -1

    const val SCRIPT_ARABIC: Int = 0
    const val SCRIPT_ARMENIAN: Int = 1
    const val SCRIPT_BENGALI: Int = 2
    const val SCRIPT_CYRILLIC: Int = 3
    const val SCRIPT_DEVANAGARI: Int = 4
    const val SCRIPT_GEORGIAN: Int = 5
    const val SCRIPT_GREEK: Int = 6
    const val SCRIPT_HEBREW: Int = 7
    const val SCRIPT_KANNADA: Int = 8
    const val SCRIPT_KHMER: Int = 9
    const val SCRIPT_LAO: Int = 10
    const val SCRIPT_LATIN: Int = 11
    const val SCRIPT_MALAYALAM: Int = 12
    const val SCRIPT_MYANMAR: Int = 13
    const val SCRIPT_SINHALA: Int = 14
    const val SCRIPT_TAMIL: Int = 15
    const val SCRIPT_TELUGU: Int = 16
    const val SCRIPT_THAI: Int = 17

    private val mLanguageCodeToScriptCode =
        TreeMap<String, Int>()

    init {
        mLanguageCodeToScriptCode[""] = SCRIPT_LATIN // default
        mLanguageCodeToScriptCode["ar"] = SCRIPT_ARABIC
        mLanguageCodeToScriptCode["hy"] = SCRIPT_ARMENIAN
        mLanguageCodeToScriptCode["bn"] = SCRIPT_BENGALI
        mLanguageCodeToScriptCode["bg"] = SCRIPT_CYRILLIC
        mLanguageCodeToScriptCode["sr"] = SCRIPT_CYRILLIC
        mLanguageCodeToScriptCode["ru"] = SCRIPT_CYRILLIC
        mLanguageCodeToScriptCode["ka"] = SCRIPT_GEORGIAN
        mLanguageCodeToScriptCode["el"] = SCRIPT_GREEK
        mLanguageCodeToScriptCode["iw"] = SCRIPT_HEBREW
        mLanguageCodeToScriptCode["km"] = SCRIPT_KHMER
        mLanguageCodeToScriptCode["lo"] = SCRIPT_LAO
        mLanguageCodeToScriptCode["ml"] = SCRIPT_MALAYALAM
        mLanguageCodeToScriptCode["my"] = SCRIPT_MYANMAR
        mLanguageCodeToScriptCode["si"] = SCRIPT_SINHALA
        mLanguageCodeToScriptCode["ta"] = SCRIPT_TAMIL
        mLanguageCodeToScriptCode["te"] = SCRIPT_TELUGU
        mLanguageCodeToScriptCode["th"] = SCRIPT_THAI
    }

    /*
     * Returns whether the code point is a letter that makes sense for the specified
     * locale for this spell checker.
     * The dictionaries supported by Latin IME are described in res/xml/spellchecker.xml
     * and is limited to EFIGS languages and Russian.
     * Hence at the moment this explicitly tests for Cyrillic characters or Latin characters
     * as appropriate, and explicitly excludes CJK, Arabic and Hebrew characters.
     */
    fun isLetterPartOfScript(codePoint: Int, scriptId: Int): Boolean {
        return when (scriptId) {
            SCRIPT_ARABIC ->             // Arabic letters can be in any of the following blocks:
                // Arabic U+0600..U+06FF
                // Arabic Supplement, Thaana U+0750..U+077F, U+0780..U+07BF
                // Arabic Extended-A U+08A0..U+08FF
                // Arabic Presentation Forms-A U+FB50..U+FDFF
                // Arabic Presentation Forms-B U+FE70..U+FEFF
                (codePoint >= 0x600 && codePoint <= 0x6FF)
                        || (codePoint >= 0x750 && codePoint <= 0x7BF)
                        || (codePoint >= 0x8A0 && codePoint <= 0x8FF)
                        || (codePoint >= 0xFB50 && codePoint <= 0xFDFF)
                        || (codePoint >= 0xFE70 && codePoint <= 0xFEFF)

            SCRIPT_ARMENIAN ->             // Armenian letters are in the Armenian unicode block, U+0530..U+058F and
                // Alphabetic Presentation Forms block, U+FB00..U+FB4F, but only in the Armenian part
                // of that block, which is U+FB13..U+FB17.
                (codePoint >= 0x530 && codePoint <= 0x58F
                        || codePoint >= 0xFB13 && codePoint <= 0xFB17)

            SCRIPT_BENGALI ->             // Bengali unicode block is U+0980..U+09FF
                (codePoint >= 0x980 && codePoint <= 0x9FF)

            SCRIPT_CYRILLIC ->             // All Cyrillic characters are in the 400~52F block. There are some in the upper
                // Unicode range, but they are archaic characters that are not used in modern
                // Russian and are not used by our dictionary.
                codePoint >= 0x400 && codePoint <= 0x52F && Character.isLetter(codePoint)

            SCRIPT_DEVANAGARI ->             // Devanagari unicode block is +0900..U+097F
                (codePoint >= 0x900 && codePoint <= 0x97F)

            SCRIPT_GEORGIAN ->             // Georgian letters are in the Georgian unicode block, U+10A0..U+10FF,
                // or Georgian supplement block, U+2D00..U+2D2F
                (codePoint >= 0x10A0 && codePoint <= 0x10FF
                        || codePoint >= 0x2D00 && codePoint <= 0x2D2F)

            SCRIPT_GREEK ->             // Greek letters are either in the 370~3FF range (Greek & Coptic), or in the
                // 1F00~1FFF range (Greek extended). Our dictionary contains both sort of characters.
                // Our dictionary also contains a few words with 0xF2; it would be best to check
                // if that's correct, but a web search does return results for these words so
                // they are probably okay.
                (codePoint >= 0x370 && codePoint <= 0x3FF)
                        || (codePoint >= 0x1F00 && codePoint <= 0x1FFF)
                        || codePoint == 0xF2

            SCRIPT_HEBREW ->             // Hebrew letters are in the Hebrew unicode block, which spans from U+0590 to U+05FF,
                // or in the Alphabetic Presentation Forms block, U+FB00..U+FB4F, but only in the
                // Hebrew part of that block, which is U+FB1D..U+FB4F.
                (codePoint >= 0x590 && codePoint <= 0x5FF
                        || codePoint >= 0xFB1D && codePoint <= 0xFB4F)

            SCRIPT_KANNADA ->             // Kannada unicode block is U+0C80..U+0CFF
                (codePoint >= 0xC80 && codePoint <= 0xCFF)

            SCRIPT_KHMER ->             // Khmer letters are in unicode block U+1780..U+17FF, and the Khmer symbols block
                // is U+19E0..U+19FF
                (codePoint >= 0x1780 && codePoint <= 0x17FF
                        || codePoint >= 0x19E0 && codePoint <= 0x19FF)

            SCRIPT_LAO ->             // The Lao block is U+0E80..U+0EFF
                (codePoint >= 0xE80 && codePoint <= 0xEFF)

            SCRIPT_LATIN ->             // Our supported latin script dictionaries (EFIGS) at the moment only include
                // characters in the C0, C1, Latin Extended A and B, IPA extensions unicode
                // blocks. As it happens, those are back-to-back in the code range 0x40 to 0x2AF,
                // so the below is a very efficient way to test for it. As for the 0-0x3F, it's
                // excluded from isLetter anyway.
                codePoint <= 0x2AF && Character.isLetter(codePoint)

            SCRIPT_MALAYALAM ->             // Malayalam unicode block is U+0D00..U+0D7F
                (codePoint >= 0xD00 && codePoint <= 0xD7F)

            SCRIPT_MYANMAR ->             // Myanmar has three unicode blocks :
                // Myanmar U+1000..U+109F
                // Myanmar extended-A U+AA60..U+AA7F
                // Myanmar extended-B U+A9E0..U+A9FF
                (codePoint >= 0x1000 && codePoint <= 0x109F || codePoint >= 0xAA60 && codePoint <= 0xAA7F || codePoint >= 0xA9E0 && codePoint <= 0xA9FF)

            SCRIPT_SINHALA ->             // Sinhala unicode block is U+0D80..U+0DFF
                (codePoint >= 0xD80 && codePoint <= 0xDFF)

            SCRIPT_TAMIL ->             // Tamil unicode block is U+0B80..U+0BFF
                (codePoint >= 0xB80 && codePoint <= 0xBFF)

            SCRIPT_TELUGU ->             // Telugu unicode block is U+0C00..U+0C7F
                (codePoint >= 0xC00 && codePoint <= 0xC7F)

            SCRIPT_THAI ->             // Thai unicode block is U+0E00..U+0E7F
                (codePoint >= 0xE00 && codePoint <= 0xE7F)

            SCRIPT_UNKNOWN -> true
            else ->             // Should never come here
                throw RuntimeException("Impossible value of script: $scriptId")
        }
    }

    /**
     * @param locale spell checker locale
     * @return internal Latin IME script code that maps to a language code
     * {@see http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes}
     */
    fun getScriptFromSpellCheckerLocale(locale: Locale): Int {
        return mLanguageCodeToScriptCode[locale.language] ?: mLanguageCodeToScriptCode[""]!!
    }
}
