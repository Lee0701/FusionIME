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

import android.text.Spanned
import android.text.style.SuggestionSpan
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a range of text, relative to the current cursor position.
 */
class TextRange(
    textAtCursor: CharSequence, wordAtCursorStartIndex: Int,
    wordAtCursorEndIndex: Int, cursorIndex: Int, hasUrlSpans: Boolean
) {
    private val mTextAtCursor: CharSequence
    private val mWordAtCursorStartIndex: Int
    private val mWordAtCursorEndIndex: Int
    private val mCursorIndex: Int

    val mWord: CharSequence
    val mHasUrlSpans: Boolean

    val numberOfCharsInWordBeforeCursor: Int
        get() = mCursorIndex - mWordAtCursorStartIndex

    val numberOfCharsInWordAfterCursor: Int
        get() = mWordAtCursorEndIndex - mCursorIndex

    fun length(): Int {
        return mWord.length
    }

    val suggestionSpansAtWord: Array<SuggestionSpan?>
        /**
         * Gets the suggestion spans that are put squarely on the word, with the exact start
         * and end of the span matching the boundaries of the word.
         * @return the list of spans.
         */
        get() {
            if (!(mTextAtCursor is Spanned && mWord is Spanned)) {
                return arrayOfNulls(0)
            }
            val text = mTextAtCursor
            // Note: it's fine to pass indices negative or greater than the length of the string
            // to the #getSpans() method. The reason we need to get from -1 to +1 is that, the
            // spans were cut at the cursor position, and #getSpans(start, end) does not return
            // spans that end at `start' or begin at `end'. Consider the following case:
            //              this| is          (The | symbolizes the cursor position
            //              ---- ---
            // In this case, the cursor is in position 4, so the 0~7 span has been split into
            // a 0~4 part and a 4~7 part.
            // If we called #getSpans(0, 4) in this case, we would only get the part from 0 to 4
            // of the span, and not the part from 4 to 7, so we would not realize the span actually
            // extends from 0 to 7. But if we call #getSpans(-1, 5) we'll get both the 0~4 and
            // the 4~7 spans and we can merge them accordingly.
            // Any span starting more than 1 char away from the word boundaries in any direction
            // does not touch the word, so we don't need to consider it. That's why requesting
            // -1 ~ +1 is enough.
            // Of course this is only relevant if the cursor is at one end of the word. If it's
            // in the middle, the -1 and +1 are not necessary, but they are harmless.
            val spans = text.getSpans(
                mWordAtCursorStartIndex - 1,
                mWordAtCursorEndIndex + 1, SuggestionSpan::class.java
            )
            var readIndex = 0
            var writeIndex = 0
            while (readIndex < spans.size) {
                val span = spans[readIndex]
                // The span may be null, as we null them when we find duplicates. Cf a few lines
                // down.
                if (null == span) {
                    ++readIndex
                    continue
                }
                // Tentative span start and end. This may be modified later if we realize the
                // same span is also applied to other parts of the string.
                var spanStart = text.getSpanStart(span)
                var spanEnd = text.getSpanEnd(span)
                for (i in readIndex + 1 until spans.size) {
                    if (span == spans[i]) {
                        // We found the same span somewhere else. Read the new extent of this
                        // span, and adjust our values accordingly.
                        spanStart = min(
                            spanStart.toDouble(),
                            text.getSpanStart(spans[i]).toDouble()
                        ).toInt()
                        spanEnd = max(
                            spanEnd.toDouble(),
                            text.getSpanEnd(spans[i]).toDouble()
                        ).toInt()
                        // ...and mark the span as processed.
                        spans[i] = null
                    }
                }
                if (spanStart == mWordAtCursorStartIndex && spanEnd == mWordAtCursorEndIndex) {
                    // If the span does not start and stop here, ignore it. It probably extends
                    // past the start or end of the word, as happens in missing space correction
                    // or EasyEditSpans put by voice input.
                    spans[writeIndex++] = spans[readIndex]
                }
                ++readIndex
            }
            return if (writeIndex == readIndex) spans else Arrays.copyOfRange(
                spans,
                0,
                writeIndex
            )
        }

    init {
        if (wordAtCursorStartIndex < 0 || cursorIndex < wordAtCursorStartIndex || cursorIndex > wordAtCursorEndIndex || wordAtCursorEndIndex > textAtCursor.length) {
            throw IndexOutOfBoundsException()
        }
        mTextAtCursor = textAtCursor
        mWordAtCursorStartIndex = wordAtCursorStartIndex
        mWordAtCursorEndIndex = wordAtCursorEndIndex
        mCursorIndex = cursorIndex
        mHasUrlSpans = hasUrlSpans
        mWord = mTextAtCursor.subSequence(mWordAtCursorStartIndex, mWordAtCursorEndIndex)
    }
}