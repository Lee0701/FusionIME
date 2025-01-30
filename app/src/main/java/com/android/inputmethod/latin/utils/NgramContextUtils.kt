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

import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.settings.SpacingAndPunctuations
import java.util.Arrays
import java.util.regex.Pattern

object NgramContextUtils {
    private val NEWLINE_REGEX: Pattern = Pattern.compile("[\\r\\n]+")
    private val SPACE_REGEX: Pattern = Pattern.compile("\\s+")

    // Get context information from nth word before the cursor. n = 1 retrieves the words
    // immediately before the cursor, n = 2 retrieves the words before that, and so on. This splits
    // on whitespace only.
    // Also, it won't return words that end in a separator (if the nth word before the cursor
    // ends in a separator, it returns information representing beginning-of-sentence).
    // Example (when Constants.MAX_PREV_WORD_COUNT_FOR_N_GRAM is 2):
    // (n = 1) "abc def|" -> abc, def
    // (n = 1) "abc def |" -> abc, def
    // (n = 1) "abc 'def|" -> empty, 'def
    // (n = 1) "abc def. |" -> beginning-of-sentence
    // (n = 1) "abc def . |" -> beginning-of-sentence
    // (n = 2) "abc def|" -> beginning-of-sentence, abc
    // (n = 2) "abc def |" -> beginning-of-sentence, abc
    // (n = 2) "abc 'def|" -> empty. The context is different from "abc def", but we cannot
    // represent this situation using NgramContext. See TODO in the method.
    // TODO: The next example's result should be "abc, def". This have to be fixed before we
    // retrieve the prior context of Beginning-of-Sentence.
    // (n = 2) "abc def. |" -> beginning-of-sentence, abc
    // (n = 2) "abc def . |" -> abc, def
    // (n = 2) "abc|" -> beginning-of-sentence
    // (n = 2) "abc |" -> beginning-of-sentence
    // (n = 2) "abc. def|" -> beginning-of-sentence
    fun getNgramContextFromNthPreviousWord(
        prev: CharSequence?,
        spacingAndPunctuations: SpacingAndPunctuations, n: Int
    ): NgramContext {
        if (prev == null) return NgramContext.EMPTY_PREV_WORDS_INFO
        val lines = NEWLINE_REGEX.split(prev)
        if (lines.size == 0) {
            return NgramContext(WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)
        }
        val w = SPACE_REGEX.split(lines[lines.size - 1])
        val prevWordsInfo =
            arrayOfNulls<WordInfo>(DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM)
        Arrays.fill(prevWordsInfo, WordInfo.EMPTY_WORD_INFO)
        for (i in prevWordsInfo.indices) {
            val focusedWordIndex = w.size - n - i
            // Referring to the word after the focused word.
            if ((focusedWordIndex + 1) >= 0 && (focusedWordIndex + 1) < w.size) {
                val wordFollowingTheNthPrevWord = w[focusedWordIndex + 1]
                if (!wordFollowingTheNthPrevWord.isEmpty()) {
                    val firstChar = wordFollowingTheNthPrevWord[0]
                    if (spacingAndPunctuations.isWordConnector(firstChar.code)) {
                        // The word following the focused word is starting with a word connector.
                        // TODO: Return meaningful context for this case.
                        break
                    }
                }
            }
            // If we can't find (n + i) words, the context is beginning-of-sentence.
            if (focusedWordIndex < 0) {
                prevWordsInfo[i] = WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO
                break
            }

            val focusedWord = w[focusedWordIndex]
            // If the word is empty, the context is beginning-of-sentence.
            val length = focusedWord.length
            if (length <= 0) {
                prevWordsInfo[i] = WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO
                break
            }
            // If the word ends in a sentence terminator, the context is beginning-of-sentence.
            val lastChar = focusedWord[length - 1]
            if (spacingAndPunctuations.isSentenceTerminator(lastChar.code)) {
                prevWordsInfo[i] = WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO
                break
            }
            // If ends in a word separator or connector, the context is unclear.
            // TODO: Return meaningful context for this case.
            if (spacingAndPunctuations.isWordSeparator(lastChar.code)
                || spacingAndPunctuations.isWordConnector(lastChar.code)
            ) {
                break
            }
            prevWordsInfo[i] = WordInfo(focusedWord)
        }
        return NgramContext(*prevWordsInfo)
    }
}
