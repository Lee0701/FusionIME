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
package com.android.inputmethod.latin.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.TextUtils
import android.text.style.SuggestionSpan
import android.text.style.URLSpan
import com.android.inputmethod.annotations.UsedForTesting
import java.util.regex.Pattern

object SpannableStringUtils {
    /**
     * Copies the spans from the region `start...end` in
     * `source` to the region
     * `destoff...destoff+end-start` in `dest`.
     * Spans in `source` that begin before `start`
     * or end after `end` but overlap this range are trimmed
     * as if they began at `start` or ended at `end`.
     * Only SuggestionSpans that don't have the SPAN_PARAGRAPH span are copied.
     *
     * This code is almost entirely taken from [TextUtils.copySpansFrom], except for the
     * kind of span that is copied.
     *
     * @throws IndexOutOfBoundsException if any of the copied spans
     * are out of range in `dest`.
     */
    fun copyNonParagraphSuggestionSpansFrom(
        source: Spanned, start: Int, end: Int,
        dest: Spannable, destoff: Int
    ) {
        val spans: Array<Any> = source.getSpans(
            start, end,
            SuggestionSpan::class.java
        )

        for (i in spans.indices) {
            var fl = source.getSpanFlags(spans[i])
            // We don't care about the PARAGRAPH flag in LatinIME code. However, if this flag
            // is set, Spannable#setSpan will throw an exception unless the span is on the edge
            // of a word. But the spans have been split into two by the getText{Before,After}Cursor
            // methods, so after concatenation they may end in the middle of a word.
            // Since we don't use them, we can just remove them and avoid crashing.
            fl = fl and Spanned.SPAN_PARAGRAPH.inv()

            var st = source.getSpanStart(spans[i])
            var en = source.getSpanEnd(spans[i])

            if (st < start) st = start
            if (en > end) en = end

            dest.setSpan(
                spans[i], st - start + destoff, en - start + destoff,
                fl
            )
        }
    }

    /**
     * Returns a CharSequence concatenating the specified CharSequences, retaining their
     * SuggestionSpans that don't have the PARAGRAPH flag, but not other spans.
     *
     * This code is almost entirely taken from [TextUtils.concat], except
     * it calls copyNonParagraphSuggestionSpansFrom instead of [TextUtils.copySpansFrom].
     */
    fun concatWithNonParagraphSuggestionSpansOnly(vararg text: CharSequence): CharSequence {
        if (text.size == 0) {
            return ""
        }

        if (text.size == 1) {
            return text[0]
        }

        var spanned = false
        for (i in text.indices) {
            if (text[i] is Spanned) {
                spanned = true
                break
            }
        }

        val sb = StringBuilder()
        for (i in text.indices) {
            sb.append(text[i])
        }

        if (!spanned) {
            return sb.toString()
        }

        val ss = SpannableString(sb)
        var off = 0
        for (i in text.indices) {
            val len = text[i].length

            if (text[i] is Spanned) {
                copyNonParagraphSuggestionSpansFrom(text[i] as Spanned, 0, len, ss, off)
            }

            off += len
        }

        return SpannedString(ss)
    }

    fun hasUrlSpans(
        text: CharSequence?,
        startIndex: Int, endIndex: Int
    ): Boolean {
        if (text !is Spanned) {
            return false // Not spanned, so no link
        }
        // getSpans(x, y) does not return spans that start on x or end on y. x-1, y+1 does the
        // trick, and works in all cases even if startIndex <= 0 or endIndex >= text.length().
        val spans = text.getSpans(
            startIndex - 1, endIndex + 1,
            URLSpan::class.java
        )
        return null != spans && spans.size > 0
    }

    /**
     * Splits the given `charSequence` with at occurrences of the given `regex`.
     *
     *
     * This is equivalent to
     * `charSequence.toString().split(regex, preserveTrailingEmptySegments ? -1 : 0)`
     * except that the spans are preserved in the result array.
     *
     * @param charSequence the character sequence to be split.
     * @param regex the regex pattern to be used as the separator.
     * @param preserveTrailingEmptySegments `true` to preserve the trailing empty
     * segments. Otherwise, trailing empty segments will be removed before being returned.
     * @return the array which contains the result. All the spans in the `charSequence`
     * is preserved.
     */
    @UsedForTesting
    fun split(
        charSequence: CharSequence, regex: String,
        preserveTrailingEmptySegments: Boolean
    ): Array<CharSequence> {
        // A short-cut for non-spanned strings.
        if (charSequence !is Spanned) {
            // -1 means that trailing empty segments will be preserved.
            return charSequence.toString().split(
                regex.toRegex(),
                if (preserveTrailingEmptySegments) -1 else 0.coerceAtLeast(0)
            ).toTypedArray()
        }

        // Hereafter, emulate String.split for CharSequence.
        val sequences = ArrayList<CharSequence>()
        val matcher = Pattern.compile(regex).matcher(charSequence)
        var nextStart = 0
        var matched = false
        while (matcher.find()) {
            sequences.add(charSequence.subSequence(nextStart, matcher.start()))
            nextStart = matcher.end()
            matched = true
        }
        if (!matched) {
            // never matched. preserveTrailingEmptySegments is ignored in this case.
            return arrayOf(charSequence)
        }
        sequences.add(charSequence.subSequence(nextStart, charSequence.length))
        if (!preserveTrailingEmptySegments) {
            for (i in sequences.indices.reversed()) {
                if (!TextUtils.isEmpty(sequences[i])) {
                    break
                }
                sequences.removeAt(i)
            }
        }
        return sequences.toTypedArray<CharSequence>()
    }
}
