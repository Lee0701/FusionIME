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
package com.android.inputmethod.compat

import android.text.Spannable
import android.text.Spanned
import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@UsedForTesting
object LocaleSpanCompatUtils {
    private val TAG: String = LocaleSpanCompatUtils::class.java.getSimpleName()

    // Note that LocaleSpan(Locale locale) has been introduced in API level 17
    // (Build.VERSION_CODE.JELLY_BEAN_MR1).
    private fun getLocaleSpanClass(): Class<*>? {
        try {
            return Class.forName("android.text.style.LocaleSpan")
        } catch (e: ClassNotFoundException) {
            return null
        }
    }

    private val LOCALE_SPAN_TYPE: Class<*>?
    private val LOCALE_SPAN_CONSTRUCTOR: Constructor<*>?
    private val LOCALE_SPAN_GET_LOCALE: Method?

    init {
        LOCALE_SPAN_TYPE = getLocaleSpanClass()
        LOCALE_SPAN_CONSTRUCTOR = CompatUtils.getConstructor(
            LOCALE_SPAN_TYPE,
            Locale::class.java
        )
        LOCALE_SPAN_GET_LOCALE = CompatUtils.getMethod(LOCALE_SPAN_TYPE, "getLocale")
    }

    @UsedForTesting
    fun isLocaleSpanAvailable(): Boolean {
        return (LOCALE_SPAN_CONSTRUCTOR != null && LOCALE_SPAN_GET_LOCALE != null)
    }

    @UsedForTesting
    fun newLocaleSpan(locale: Locale?): Any? {
        return CompatUtils.newInstance(LOCALE_SPAN_CONSTRUCTOR, locale)
    }

    @UsedForTesting
    fun getLocaleFromLocaleSpan(localeSpan: Any?): Locale? {
        return CompatUtils.invoke(localeSpan, null, LOCALE_SPAN_GET_LOCALE) as Locale?
    }

    /**
     * Ensures that the specified range is covered with only one [LocaleSpan] with the given
     * locale. If the region is already covered by one or more [LocaleSpan], their ranges are
     * updated so that each character has only one locale.
     * @param spannable the spannable object to be updated.
     * @param start the start index from which [LocaleSpan] is attached (inclusive).
     * @param end the end index to which [LocaleSpan] is attached (exclusive).
     * @param locale the locale to be attached to the specified range.
     */
    @UsedForTesting
    fun updateLocaleSpan(
        spannable: Spannable, start: Int,
        end: Int, locale: Locale
    ) {
        if (end < start) {
            Log.e(TAG, "Invalid range: start=" + start + " end=" + end)
            return
        }
        if (!isLocaleSpanAvailable()) {
            return
        }
        // A brief summary of our strategy;
        //   1. Enumerate all LocaleSpans between [start - 1, end + 1].
        //   2. For each LocaleSpan S:
        //      - Update the range of S so as not to cover [start, end] if S doesn't have the
        //        expected locale.
        //      - Mark S as "to be merged" if S has the expected locale.
        //   3. Merge all the LocaleSpans that are marked as "to be merged" into one LocaleSpan.
        //      If no appropriate span is found, create a new one with newLocaleSpan method.
        val searchStart: Int = max((start - 1).toDouble(), 0.0).toInt()
        val searchEnd: Int =
            min((end + 1).toDouble(), spannable.length.toDouble()).toInt()
        // LocaleSpans found in the target range. See the step 1 in the above comment.
        val existingLocaleSpans: Array<out Any> = spannable.getSpans(
            searchStart, searchEnd,
            LOCALE_SPAN_TYPE
        )
        // LocaleSpans that are marked as "to be merged". See the step 2 in the above comment.
        val existingLocaleSpansToBeMerged: ArrayList<Any> = ArrayList()
        var isStartExclusive: Boolean = true
        var isEndExclusive: Boolean = true
        var newStart: Int = start
        var newEnd: Int = end
        for (existingLocaleSpan: Any in existingLocaleSpans) {
            val attachedLocale: Locale? = getLocaleFromLocaleSpan(existingLocaleSpan)
            if (locale != attachedLocale) {
                // This LocaleSpan does not have the expected locale. Update its range if it has
                // an intersection with the range [start, end] (the first case of the step 2 in the
                // above comment).
                removeLocaleSpanFromRange(existingLocaleSpan, spannable, start, end)
                continue
            }
            val spanStart: Int = spannable.getSpanStart(existingLocaleSpan)
            val spanEnd: Int = spannable.getSpanEnd(existingLocaleSpan)
            if (spanEnd < spanStart) {
                Log.e(TAG, "Invalid span: spanStart=" + spanStart + " spanEnd=" + spanEnd)
                continue
            }
            if (spanEnd < start || end < spanStart) {
                // No intersection found.
                continue
            }

            // Here existingLocaleSpan has the expected locale and an intersection with the
            // range [start, end] (the second case of the the step 2 in the above comment).
            val spanFlag: Int = spannable.getSpanFlags(existingLocaleSpan)
            if (spanStart < newStart) {
                newStart = spanStart
                isStartExclusive = ((spanFlag and Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ==
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (newEnd < spanEnd) {
                newEnd = spanEnd
                isEndExclusive = ((spanFlag and Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ==
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            existingLocaleSpansToBeMerged.add(existingLocaleSpan)
        }

        var originalLocaleSpanFlag: Int = 0
        var localeSpan: Any? = null
        if (existingLocaleSpansToBeMerged.isEmpty()) {
            // If there is no LocaleSpan that is marked as to be merged, create a new one.
            localeSpan = newLocaleSpan(locale)
        } else {
            // Reuse the first LocaleSpan to avoid unnecessary object instantiation.
            localeSpan = existingLocaleSpansToBeMerged.get(0)
            originalLocaleSpanFlag = spannable.getSpanFlags(localeSpan)
            // No need to keep other instances.
            for (i in 1 until existingLocaleSpansToBeMerged.size) {
                spannable.removeSpan(existingLocaleSpansToBeMerged.get(i))
            }
        }
        val localeSpanFlag: Int = getSpanFlag(
            originalLocaleSpanFlag, isStartExclusive,
            isEndExclusive
        )
        spannable.setSpan(localeSpan, newStart, newEnd, localeSpanFlag)
    }

    private fun removeLocaleSpanFromRange(
        localeSpan: Any,
        spannable: Spannable, removeStart: Int, removeEnd: Int
    ) {
        if (!isLocaleSpanAvailable()) {
            return
        }
        val spanStart: Int = spannable.getSpanStart(localeSpan)
        val spanEnd: Int = spannable.getSpanEnd(localeSpan)
        if (spanStart > spanEnd) {
            Log.e(TAG, "Invalid span: spanStart=" + spanStart + " spanEnd=" + spanEnd)
            return
        }
        if (spanEnd < removeStart) {
            // spanStart < spanEnd < removeStart < removeEnd
            return
        }
        if (removeEnd < spanStart) {
            // spanStart < removeEnd < spanStart < spanEnd
            return
        }
        val spanFlags: Int = spannable.getSpanFlags(localeSpan)
        if (spanStart < removeStart) {
            if (removeEnd < spanEnd) {
                // spanStart < removeStart < removeEnd < spanEnd
                val locale: Locale? = getLocaleFromLocaleSpan(localeSpan)
                spannable.setSpan(localeSpan, spanStart, removeStart, spanFlags)
                val attionalLocaleSpan: Any? = newLocaleSpan(locale)
                spannable.setSpan(attionalLocaleSpan, removeEnd, spanEnd, spanFlags)
                return
            }
            // spanStart < removeStart < spanEnd <= removeEnd
            spannable.setSpan(localeSpan, spanStart, removeStart, spanFlags)
            return
        }
        if (removeEnd < spanEnd) {
            // removeStart <= spanStart < removeEnd < spanEnd
            spannable.setSpan(localeSpan, removeEnd, spanEnd, spanFlags)
            return
        }
        // removeStart <= spanStart < spanEnd < removeEnd
        spannable.removeSpan(localeSpan)
    }

    private fun getSpanFlag(
        originalFlag: Int,
        isStartExclusive: Boolean, isEndExclusive: Boolean
    ): Int {
        return (originalFlag and Spanned.SPAN_POINT_MARK_MASK.inv()) or
                getSpanPointMarkFlag(isStartExclusive, isEndExclusive)
    }

    private fun getSpanPointMarkFlag(
        isStartExclusive: Boolean,
        isEndExclusive: Boolean
    ): Int {
        if (isStartExclusive) {
            return if (isEndExclusive)
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            else
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
        }
        return if (isEndExclusive)
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        else
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
    }
}
