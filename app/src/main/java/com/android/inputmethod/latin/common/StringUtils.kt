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
import java.util.Arrays
import java.util.Locale
import javax.annotation.Nonnull

object StringUtils {
    const val CAPITALIZE_NONE: Int = 0 // No caps, or mixed case
    const val CAPITALIZE_FIRST: Int = 1 // First only
    const val CAPITALIZE_ALL: Int = 2 // All caps

    @Nonnull
    private const val EMPTY_STRING = ""

    private const val CHAR_LINE_FEED = 0X000A.toChar()
    private const val CHAR_VERTICAL_TAB = 0X000B.toChar()
    private const val CHAR_FORM_FEED = 0X000C.toChar()
    private const val CHAR_CARRIAGE_RETURN = 0X000D.toChar()
    private const val CHAR_NEXT_LINE = 0X0085.toChar()
    private const val CHAR_LINE_SEPARATOR = 0X2028.toChar()
    private const val CHAR_PARAGRAPH_SEPARATOR = 0X2029.toChar()

    // Taken from android.text.TextUtils. We are extensively using this method in many places,
    // some of which don't have the android libraries available.
    /**
     * Returns true if the string is null or 0-length.
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    fun isEmpty(str: CharSequence?): Boolean {
        return (str == null || str.length == 0)
    }

    // Taken from android.text.TextUtils to cut the dependency to the Android framework.
    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param delimiter the delimiter
     * @param tokens an array objects to be joined. Strings will be formed from
     * the objects by calling object.toString().
     */
    @Nonnull
    fun join(
        @Nonnull delimiter: CharSequence?,
        @Nonnull tokens: Iterable<*>
    ): String {
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }

    // Taken from android.text.TextUtils to cut the dependency to the Android framework.
    /**
     * Returns true if a and b are equal, including if they are both null.
     *
     * *Note: In platform versions 1.1 and earlier, this method only worked well if
     * both the arguments were instances of String.*
     * @param a first CharSequence to check
     * @param b second CharSequence to check
     * @return true if a and b are equal
     */
    fun equals(a: CharSequence?, b: CharSequence?): Boolean {
        if (a === b) {
            return true
        }
        val length: Int
        if (a != null && b != null && (a.length.also { length = it }) == b.length) {
            if (a is String && b is String) {
                return a == b
            }
            for (i in 0 until length) {
                if (a[i] != b[i]) {
                    return false
                }
            }
            return true
        }
        return false
    }

    fun codePointCount(text: CharSequence?): Int {
        if (isEmpty(text)) {
            return 0
        }
        return Character.codePointCount(text, 0, text!!.length)
    }

    @Nonnull
    fun newSingleCodePointString(codePoint: Int): String {
        if (Character.charCount(codePoint) == 1) {
            // Optimization: avoid creating a temporary array for characters that are
            // represented by a single char value
            return codePoint.toChar().toString()
        }
        // For surrogate pair
        return String(Character.toChars(codePoint))
    }

    fun containsInArray(
        @Nonnull text: String,
        @Nonnull array: Array<String>
    ): Boolean {
        for (element in array) {
            if (text == element) {
                return true
            }
        }
        return false
    }

    /**
     * Comma-Splittable Text is similar to Comma-Separated Values (CSV) but has much simpler syntax.
     * Unlike CSV, Comma-Splittable Text has no escaping mechanism, so that the text can't contain
     * a comma character in it.
     */
    @Nonnull
    private const val SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT = ","

    fun containsInCommaSplittableText(
        @Nonnull text: String,
        extraValues: String?
    ): Boolean {
        if (isEmpty(extraValues)) {
            return false
        }
        return containsInArray(
            text,
            extraValues!!.split(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT.toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        )
    }

    @Nonnull
    fun removeFromCommaSplittableTextIfExists(
        @Nonnull text: String,
        extraValues: String?
    ): String? {
        if (isEmpty(extraValues)) {
            return EMPTY_STRING
        }
        val elements = extraValues!!.split(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT.toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        if (!containsInArray(text, elements)) {
            return extraValues
        }
        val result = ArrayList<String>(elements.size - 1)
        for (element in elements) {
            if (text != element) {
                result.add(element)
            }
        }
        return join(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT, result)
    }

    /**
     * Remove duplicates from an array of strings.
     *
     * This method will always keep the first occurrence of all strings at their position
     * in the array, removing the subsequent ones.
     */
    fun removeDupes(@Nonnull suggestions: ArrayList<String?>) {
        if (suggestions.size < 2) {
            return
        }
        var i = 1
        // Don't cache suggestions.size(), since we may be removing items
        while (i < suggestions.size) {
            val cur = suggestions[i]
            // Compare each suggestion with each previous suggestion
            var j = 0
            while (j < i) {
                val previous = suggestions[j]
                if (equals(cur, previous)) {
                    suggestions.removeAt(i)
                    i--
                    break
                }
                j++
            }
            i++
        }
    }

    @Nonnull
    fun capitalizeFirstCodePoint(
        @Nonnull s: String,
        @Nonnull locale: Locale
    ): String {
        if (s.length <= 1) {
            return s.uppercase(getLocaleUsedForToTitleCase(locale))
        }
        // Please refer to the comment below in
        // {@link #capitalizeFirstAndDowncaseRest(String,Locale)} as this has the same shortcomings
        val cutoff = s.offsetByCodePoints(0, 1)
        return (s.substring(0, cutoff).uppercase(getLocaleUsedForToTitleCase(locale))
                + s.substring(cutoff))
    }

    @Nonnull
    fun capitalizeFirstAndDowncaseRest(
        @Nonnull s: String,
        @Nonnull locale: Locale?
    ): String {
        if (s.length <= 1) {
            return s.uppercase(
                getLocaleUsedForToTitleCase(
                    locale!!
                )
            )
        }
        // TODO: fix the bugs below
        // - It does not work for Serbian, because it fails to account for the "lj" character,
        // which should be "Lj" in title case and "LJ" in upper case.
        // - It does not work for Dutch, because it fails to account for the "ij" digraph when it's
        // written as two separate code points. They are two different characters but both should
        // be capitalized as "IJ" as if they were a single letter in most words (not all). If the
        // unicode char for the ligature is used however, it works.
        val cutoff = s.offsetByCodePoints(0, 1)
        return (s.substring(0, cutoff).uppercase(
            getLocaleUsedForToTitleCase(
                locale!!
            )
        )
                + s.substring(cutoff).lowercase(locale))
    }

    @Nonnull
    fun toCodePointArray(@Nonnull charSequence: CharSequence?): IntArray {
        return toCodePointArray(charSequence!!, 0, charSequence.length)
    }

    @Nonnull
    private val EMPTY_CODEPOINTS = intArrayOf()

    /**
     * Converts a range of a string to an array of code points.
     * @param charSequence the source string.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @return a new array of code points. At most endIndex - startIndex, but possibly less.
     */
    @Nonnull
    fun toCodePointArray(
        @Nonnull charSequence: CharSequence,
        startIndex: Int, endIndex: Int
    ): IntArray {
        val length = charSequence.length
        if (length <= 0) {
            return EMPTY_CODEPOINTS
        }
        val codePoints =
            IntArray(Character.codePointCount(charSequence, startIndex, endIndex))
        copyCodePointsAndReturnCodePointCount(
            codePoints, charSequence, startIndex, endIndex,
            false /* downCase */
        )
        return codePoints
    }

    /**
     * Copies the codepoints in a CharSequence to an int array.
     *
     * This method assumes there is enough space in the array to store the code points. The size
     * can be measured with Character#codePointCount(CharSequence, int, int) before passing to this
     * method. If the int array is too small, an ArrayIndexOutOfBoundsException will be thrown.
     * Also, this method makes no effort to be thread-safe. Do not modify the CharSequence while
     * this method is running, or the behavior is undefined.
     * This method can optionally downcase code points before copying them, but it pays no attention
     * to locale while doing so.
     *
     * @param destination the int array.
     * @param charSequence the CharSequence.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @param downCase if this is true, code points will be downcased before being copied.
     * @return the number of copied code points.
     */
    fun copyCodePointsAndReturnCodePointCount(
        @Nonnull destination: IntArray,
        @Nonnull charSequence: CharSequence?, startIndex: Int, endIndex: Int,
        downCase: Boolean
    ): Int {
        var destIndex = 0
        var index = startIndex
        while (index < endIndex
        ) {
            val codePoint = Character.codePointAt(charSequence, index)
            // TODO: stop using this, as it's not aware of the locale and does not always do
            // the right thing.
            destination[destIndex] = if (downCase) codePoint.lowercaseChar() else codePoint
            destIndex++
            index = Character.offsetByCodePoints(charSequence, index, 1)
        }
        return destIndex
    }

    @Nonnull
    fun toSortedCodePointArray(@Nonnull string: String?): IntArray {
        val codePoints = toCodePointArray(string)
        Arrays.sort(codePoints)
        return codePoints
    }

    /**
     * Construct a String from a code point array
     *
     * @param codePoints a code point array that is null terminated when its logical length is
     * shorter than the array length.
     * @return a string constructed from the code point array.
     */
    @Nonnull
    fun getStringFromNullTerminatedCodePointArray(
        @Nonnull codePoints: IntArray
    ): String {
        var stringLength = codePoints.size
        for (i in codePoints.indices) {
            if (codePoints[i] == 0) {
                stringLength = i
                break
            }
        }
        return String(codePoints, 0,  /* offset */stringLength)
    }

    // This method assumes the text is not null. For the empty string, it returns CAPITALIZE_NONE.
    fun getCapitalizationType(@Nonnull text: String): Int {
        // If the first char is not uppercase, then the word is either all lower case or
        // camel case, and in either case we return CAPITALIZE_NONE.
        val len = text.length
        var index = 0
        while (index < len) {
            if (Character.isLetter(text.codePointAt(index))) {
                break
            }
            index = text.offsetByCodePoints(index, 1)
        }
        if (index == len) return CAPITALIZE_NONE
        if (!Character.isUpperCase(text.codePointAt(index))) {
            return CAPITALIZE_NONE
        }
        var capsCount = 1
        var letterCount = 1
        index = text.offsetByCodePoints(index, 1)
        while (index < len
        ) {
            if (1 != capsCount && letterCount != capsCount) break
            val codePoint = text.codePointAt(index)
            if (Character.isUpperCase(codePoint)) {
                ++capsCount
                ++letterCount
            } else if (Character.isLetter(codePoint)) {
                // We need to discount non-letters since they may not be upper-case, but may
                // still be part of a word (e.g. single quote or dash, as in "IT'S" or "FULL-TIME")
                ++letterCount
            }
            index = text.offsetByCodePoints(index, 1)
        }
        // We know the first char is upper case. So we want to test if either every letter other
        // than the first is lower case, or if they are all upper case. If the string is exactly
        // one char long, then we will arrive here with letterCount 1, and this is correct, too.
        if (1 == capsCount) return CAPITALIZE_FIRST
        return (if (letterCount == capsCount) CAPITALIZE_ALL else CAPITALIZE_NONE)
    }

    fun isIdenticalAfterUpcase(@Nonnull text: String): Boolean {
        val length = text.length
        var i = 0
        while (i < length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint) && !Character.isUpperCase(codePoint)) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }

    fun isIdenticalAfterDowncase(@Nonnull text: String): Boolean {
        val length = text.length
        var i = 0
        while (i < length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint) && !Character.isLowerCase(codePoint)) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }

    fun isIdenticalAfterCapitalizeEachWord(
        @Nonnull text: String,
        @Nonnull sortedSeparators: IntArray?
    ): Boolean {
        var needsCapsNext = true
        val len = text.length
        var i = 0
        while (i < len) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint)) {
                if ((needsCapsNext && !Character.isUpperCase(codePoint))
                    || (!needsCapsNext && !Character.isLowerCase(codePoint))
                ) {
                    return false
                }
            }
            // We need a capital letter next if this is a separator.
            needsCapsNext = (Arrays.binarySearch(sortedSeparators, codePoint) >= 0)
            i = text.offsetByCodePoints(i, 1)
        }
        return true
    }

    // TODO: like capitalizeFirst*, this does not work perfectly for Dutch because of the IJ digraph
    // which should be capitalized together in *some* cases.
    @Nonnull
    fun capitalizeEachWord(
        @Nonnull text: String,
        @Nonnull sortedSeparators: IntArray?, @Nonnull locale: Locale?
    ): String {
        val builder = StringBuilder()
        var needsCapsNext = true
        val len = text.length
        var i = 0
        while (i < len) {
            val nextChar = text.substring(i, text.offsetByCodePoints(i, 1))
            if (needsCapsNext) {
                builder.append(nextChar.uppercase(locale!!))
            } else {
                builder.append(nextChar.lowercase(locale!!))
            }
            // We need a capital letter next if this is a separator.
            needsCapsNext = (Arrays.binarySearch(sortedSeparators, nextChar.codePointAt(0)) >= 0)
            i = text.offsetByCodePoints(i, 1)
        }
        return builder.toString()
    }

    /**
     * Approximates whether the text before the cursor looks like a URL.
     *
     * This is not foolproof, but it should work well in the practice.
     * Essentially it walks backward from the cursor until it finds something that's not a letter,
     * digit, or common URL symbol like underscore. If it hasn't found a period yet, then it
     * does not look like a URL.
     * If the text:
     * - starts with www and contains a period
     * - starts with a slash preceded by either a slash, whitespace, or start-of-string
     * Then it looks like a URL and we return true. Otherwise, we return false.
     *
     * Note: this method is called quite often, and should be fast.
     *
     * TODO: This will return that "abc./def" and ".abc/def" look like URLs to keep down the
     * code complexity, but ideally it should not. It's acceptable for now.
     */
    fun lastPartLooksLikeURL(@Nonnull text: CharSequence): Boolean {
        var i = text.length
        if (0 == i) {
            return false
        }
        var wCount = 0
        var slashCount = 0
        var hasSlash = false
        var hasPeriod = false
        var codePoint = 0
        while (i > 0) {
            codePoint = Character.codePointBefore(text, i)
            if (codePoint < Constants.CODE_PERIOD || codePoint > 'z'.code) {
                // Handwavy heuristic to see if that's a URL character. Anything between period
                // and z. This includes all lower- and upper-case ascii letters, period,
                // underscore, arrobase, question mark, equal sign. It excludes spaces, exclamation
                // marks, double quotes...
                // Anything that's not a URL-like character causes us to break from here and
                // evaluate normally.
                break
            }
            if (Constants.CODE_PERIOD == codePoint) {
                hasPeriod = true
            }
            if (Constants.CODE_SLASH == codePoint) {
                hasSlash = true
                if (2 == ++slashCount) {
                    return true
                }
            } else {
                slashCount = 0
            }
            if ('w'.code == codePoint) {
                ++wCount
            } else {
                wCount = 0
            }
            i = Character.offsetByCodePoints(text, i, -1)
        }
        // End of the text run.
        // If it starts with www and includes a period, then it looks like a URL.
        if (wCount >= 3 && hasPeriod) {
            return true
        }
        // If it starts with a slash, and the code point before is whitespace, it looks like an URL.
        if (1 == slashCount && (0 == i || Character.isWhitespace(codePoint))) {
            return true
        }
        // If it has both a period and a slash, it looks like an URL.
        if (hasPeriod && hasSlash) {
            return true
        }
        // Otherwise, it doesn't look like an URL.
        return false
    }

    /**
     * Examines the string and returns whether we're inside a double quote.
     *
     * This is used to decide whether we should put an automatic space before or after a double
     * quote character. If we're inside a quotation, then we want to close it, so we want a space
     * after and not before. Otherwise, we want to open the quotation, so we want a space before
     * and not after. Exception: after a digit, we never want a space because the "inch" or
     * "minutes" use cases is dominant after digits.
     * In the practice, we determine whether we are in a quotation or not by finding the previous
     * double quote character, and looking at whether it's followed by whitespace. If so, that
     * was a closing quotation mark, so we're not inside a double quote. If it's not followed
     * by whitespace, then it was an opening quotation mark, and we're inside a quotation.
     *
     * @param text the text to examine.
     * @return whether we're inside a double quote.
     */
    fun isInsideDoubleQuoteOrAfterDigit(@Nonnull text: CharSequence): Boolean {
        var i = text.length
        if (0 == i) {
            return false
        }
        var codePoint = Character.codePointBefore(text, i)
        if (Character.isDigit(codePoint)) {
            return true
        }
        var prevCodePoint = 0
        while (i > 0) {
            codePoint = Character.codePointBefore(text, i)
            if (Constants.CODE_DOUBLE_QUOTE == codePoint) {
                // If we see a double quote followed by whitespace, then that
                // was a closing quote.
                if (Character.isWhitespace(prevCodePoint)) {
                    return false
                }
            }
            if (Character.isWhitespace(codePoint) && Constants.CODE_DOUBLE_QUOTE == prevCodePoint) {
                // If we see a double quote preceded by whitespace, then that
                // was an opening quote. No need to continue seeking.
                return true
            }
            i -= Character.charCount(codePoint)
            prevCodePoint = codePoint
        }
        // We reached the start of text. If the first char is a double quote, then we're inside
        // a double quote. Otherwise we're not.
        return Constants.CODE_DOUBLE_QUOTE == codePoint
    }

    fun isEmptyStringOrWhiteSpaces(@Nonnull s: String): Boolean {
        val N = codePointCount(s)
        for (i in 0 until N) {
            if (!Character.isWhitespace(s.codePointAt(i))) {
                return false
            }
        }
        return true
    }

    @UsedForTesting
    @Nonnull
    fun byteArrayToHexString(bytes: ByteArray?): String {
        if (bytes == null || bytes.size == 0) {
            return EMPTY_STRING
        }
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xff))
        }
        return sb.toString()
    }

    /**
     * Convert hex string to byte array. The string length must be an even number.
     */
    @UsedForTesting
    fun hexStringToByteArray(hexString: String?): ByteArray? {
        if (isEmpty(hexString)) {
            return null
        }
        val N = hexString!!.length
        if (N % 2 != 0) {
            throw NumberFormatException(
                ("Input hex string length must be an even number."
                        + " Length = " + N)
            )
        }
        val bytes = ByteArray(N / 2)
        var i = 0
        while (i < N) {
            bytes[i / 2] = ((hexString[i].digitToIntOrNull(16) ?: -1 shl 4)
            + hexString[i + 1].digitToIntOrNull(16)!! ?: -1).toByte()
            i += 2
        }
        return bytes
    }

    private const val LANGUAGE_GREEK = "el"

    @Nonnull
    private fun getLocaleUsedForToTitleCase(@Nonnull locale: Locale): Locale {
        // In Greek locale {@link String#toUpperCase(Locale)} eliminates accents from its result.
        // In order to get accented upper case letter, {@link Locale#ROOT} should be used.
        if (LANGUAGE_GREEK == locale.language) {
            return Locale.ROOT
        }
        return locale
    }

    fun toTitleCaseOfKeyLabel(
        label: String?,
        @Nonnull locale: Locale
    ): String? {
        if (label == null) {
            return label
        }
        return label.uppercase(getLocaleUsedForToTitleCase(locale))
    }

    fun toTitleCaseOfKeyCode(code: Int, @Nonnull locale: Locale): Int {
        if (!Constants.isLetterCode(code)) {
            return code
        }
        val label = newSingleCodePointString(code)
        val titleCaseLabel =
            toTitleCaseOfKeyLabel(label, locale)!!
        return if (codePointCount(titleCaseLabel) == 1)
            titleCaseLabel.codePointAt(0)
        else
            Constants.CODE_UNSPECIFIED
    }

    fun getTrailingSingleQuotesCount(@Nonnull charSequence: CharSequence): Int {
        val lastIndex = charSequence.length - 1
        var i = lastIndex
        while (i >= 0 && charSequence[i].code == Constants.CODE_SINGLE_QUOTE) {
            --i
        }
        return lastIndex - i
    }

    /**
     * Returns whether the last composed word contains line-breaking character (e.g. CR or LF).
     * @param text the text to be examined.
     * @return `true` if the last composed word contains line-breaking separator.
     */
    fun hasLineBreakCharacter(text: String?): Boolean {
        if (isEmpty(text)) {
            return false
        }
        for (i in text!!.length - 1 downTo 0) {
            val c = text[i]
            when (c) {
                CHAR_LINE_FEED, CHAR_VERTICAL_TAB, CHAR_FORM_FEED, CHAR_CARRIAGE_RETURN, CHAR_NEXT_LINE, CHAR_LINE_SEPARATOR, CHAR_PARAGRAPH_SEPARATOR -> return true
            }
        }
        return false
    }

    @UsedForTesting
    class Stringizer<E> {
        @UsedForTesting
        @Nonnull
        fun stringize(element: E?): String {
            if (element == null) {
                return "null"
            }
            return element.toString()
        }

        @UsedForTesting
        @Nonnull
        fun join(array: Array<E>?): String {
            return joinStringArray(toStringArray(array), null /* delimiter */)
        }

        @UsedForTesting
        fun join(array: Array<E>?, delimiter: String?): String {
            return joinStringArray(toStringArray(array), delimiter)
        }

        @Nonnull
        protected fun toStringArray(array: Array<E>?): Array<String?> {
            if (array == null) {
                return EMPTY_STRING_ARRAY
            }
            val stringArray = arrayOfNulls<String>(array.size)
            for (index in array.indices) {
                stringArray[index] = stringize(array[index])
            }
            return stringArray
        }

        @Nonnull
        protected fun joinStringArray(
            @Nonnull stringArray: Array<String?>,
            delimiter: String?
        ): String {
            if (delimiter == null) {
                return stringArray.contentToString()
            }
            val sb = StringBuilder()
            for (index in stringArray.indices) {
                sb.append(if (index == 0) "[" else delimiter)
                sb.append(stringArray[index])
            }
            return "$sb]"
        }

        companion object {
            @Nonnull
            private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)
        }
    }
}
