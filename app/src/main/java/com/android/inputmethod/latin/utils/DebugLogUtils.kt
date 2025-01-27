/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.inputmethod.latin.utils

import android.util.Log
import com.android.inputmethod.latin.define.DebugFlags

/**
 * A class for logging and debugging utility methods.
 */
object DebugLogUtils {
    private val TAG: String = DebugLogUtils::class.java.simpleName
    private const val sDBG = DebugFlags.DEBUG_ENABLED

    /**
     * Calls .toString() on its non-null argument or returns "null"
     * @param o the object to convert to a string
     * @return the result of .toString() or null
     */
    fun s(o: Any?): String {
        return o?.toString() ?: "null"
    }

    val stackTrace: String
        /**
         * Get the string representation of the current stack trace, for debugging purposes.
         * @return a readable, carriage-return-separated string for the current stack trace.
         */
        get() = getStackTrace(Int.MAX_VALUE - 1)

    /**
     * Get the string representation of the current stack trace, for debugging purposes.
     * @param limit the maximum number of stack frames to be returned.
     * @return a readable, carriage-return-separated string for the current stack trace.
     */
    fun getStackTrace(limit: Int): String {
        val sb = StringBuilder()
        try {
            throw RuntimeException()
        } catch (e: RuntimeException) {
            val frames = e.stackTrace
            // Start at 1 because the first frame is here and we don't care about it
            var j = 1
            while (j < frames.size && j < limit + 1) {
                sb.append(frames[j].toString() + "\n")
                ++j
            }
        }
        return sb.toString()
    }

    /**
     * Get the stack trace contained in an exception as a human-readable string.
     * @param t the throwable
     * @return the human-readable stack trace
     */
    fun getStackTrace(t: Throwable): String {
        val sb = StringBuilder()
        val frames = t.stackTrace
        for (j in frames.indices) {
            sb.append(frames[j].toString() + "\n")
        }
        return sb.toString()
    }

    /**
     * Helper log method to ease null-checks and adding spaces.
     *
     * This sends all arguments to the log, separated by spaces. Any null argument is converted
     * to the "null" string. It uses a very visible tag and log level for debugging purposes.
     *
     * @param args the stuff to send to the log
     */
    fun l(vararg args: Any?) {
        if (!sDBG) return
        val sb = StringBuilder()
        for (o in args) {
            sb.append(s(o).toString())
            sb.append(" ")
        }
        Log.e(TAG, sb.toString())
    }

    /**
     * Helper log method to put stuff in red.
     *
     * This does the same as #l but prints in red
     *
     * @param args the stuff to send to the log
     */
    fun r(vararg args: Any?) {
        if (!sDBG) return
        val sb = StringBuilder("\u001B[31m")
        for (o in args) {
            sb.append(s(o).toString())
            sb.append(" ")
        }
        sb.append("\u001B[0m")
        Log.e(TAG, sb.toString())
    }
}
