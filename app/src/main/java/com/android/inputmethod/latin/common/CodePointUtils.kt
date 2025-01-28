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
package com.android.inputmethod.latin.common

import com.android.inputmethod.annotations.UsedForTesting
import java.util.Random
import kotlin.math.abs

// Utility methods related with code points used for tests.
// TODO: Figure out where this class should be.
@UsedForTesting
object CodePointUtils {
    val LATIN_ALPHABETS_LOWER: IntArray = intArrayOf(
        'a'.code,
        'b'.code,
        'c'.code,
        'd'.code,
        'e'.code,
        'f'.code,
        'g'.code,
        'h'.code,
        'i'.code,
        'j'.code,
        'k'.code,
        'l'.code,
        'm'.code,
        'n'.code,
        'o'.code,
        'p'.code,
        'q'.code,
        'r'.code,
        's'.code,
        't'.code,
        'u'.code,
        'v'.code,
        'w'.code,
        'x'.code,
        'y'.code,
        'z'.code,
        0x00E0,  /* LATIN SMALL LETTER A WITH GRAVE */
        0x00E1,  /* LATIN SMALL LETTER A WITH ACUTE */
        0x00E2,  /* LATIN SMALL LETTER A WITH CIRCUMFLEX */
        0x00E3,  /* LATIN SMALL LETTER A WITH TILDE */
        0x00E4,  /* LATIN SMALL LETTER A WITH DIAERESIS */
        0x00E5,  /* LATIN SMALL LETTER A WITH RING ABOVE */
        0x00E6,  /* LATIN SMALL LETTER AE */
        0x00E7,  /* LATIN SMALL LETTER C WITH CEDILLA */
        0x00E8,  /* LATIN SMALL LETTER E WITH GRAVE */
        0x00E9,  /* LATIN SMALL LETTER E WITH ACUTE */
        0x00EA,  /* LATIN SMALL LETTER E WITH CIRCUMFLEX */
        0x00EB,  /* LATIN SMALL LETTER E WITH DIAERESIS */
        0x00EC,  /* LATIN SMALL LETTER I WITH GRAVE */
        0x00ED,  /* LATIN SMALL LETTER I WITH ACUTE */
        0x00EE,  /* LATIN SMALL LETTER I WITH CIRCUMFLEX */
        0x00EF,  /* LATIN SMALL LETTER I WITH DIAERESIS */
        0x00F0,  /* LATIN SMALL LETTER ETH */
        0x00F1,  /* LATIN SMALL LETTER N WITH TILDE */
        0x00F2,  /* LATIN SMALL LETTER O WITH GRAVE */
        0x00F3,  /* LATIN SMALL LETTER O WITH ACUTE */
        0x00F4,  /* LATIN SMALL LETTER O WITH CIRCUMFLEX */
        0x00F5,  /* LATIN SMALL LETTER O WITH TILDE */
        0x00F6,  /* LATIN SMALL LETTER O WITH DIAERESIS */
        0x00F7,  /* LATIN SMALL LETTER O WITH STROKE */
        0x00F9,  /* LATIN SMALL LETTER U WITH GRAVE */
        0x00FA,  /* LATIN SMALL LETTER U WITH ACUTE */
        0x00FB,  /* LATIN SMALL LETTER U WITH CIRCUMFLEX */
        0x00FC,  /* LATIN SMALL LETTER U WITH DIAERESIS */
        0x00FD,  /* LATIN SMALL LETTER Y WITH ACUTE */
        0x00FE,  /* LATIN SMALL LETTER THORN */
        0x00FF /* LATIN SMALL LETTER Y WITH DIAERESIS */
    )

    @UsedForTesting
    fun generateCodePointSet(
        codePointSetSize: Int,
        random: Random
    ): IntArray {
        val codePointSet = IntArray(codePointSetSize)
        var i = codePointSet.size - 1
        while (i >= 0) {
            val r = abs(random.nextInt().toDouble()).toInt()
            if (r < 0) {
                continue
            }
            // Don't insert 0~0x20, but insert any other code point.
            // Code points are in the range 0~0x10FFFF.
            val candidateCodePoint = 0x20 + r % (Character.MAX_CODE_POINT - 0x20)
            // Code points between MIN_ and MAX_SURROGATE are not valid on their own.
            if (candidateCodePoint >= Character.MIN_SURROGATE.code
                && candidateCodePoint <= Character.MAX_SURROGATE.code
            ) {
                continue
            }
            codePointSet[i] = candidateCodePoint
            --i
        }
        return codePointSet
    }

    /**
     * Generates a random word.
     */
    @UsedForTesting
    fun generateWord(
        random: Random,
        codePointSet: IntArray
    ): String {
        val builder = StringBuilder()
        // 8 * 4 = 32 chars max, but we do it the following way so as to bias the random toward
        // longer words. This should be closer to natural language, and more importantly, it will
        // exercise the algorithms in dicttool much more.
        val count = ((1 + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)
                + (abs(random.nextInt().toDouble()) % 5)).toInt())
        while (builder.length < count) {
            builder.appendCodePoint(
                codePointSet[(abs(
                    random.nextInt().toDouble()
                ) % codePointSet.size).toInt()]
            )
        }
        return builder.toString()
    }
}
