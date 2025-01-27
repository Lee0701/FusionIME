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

import android.view.inputmethod.InputConnection
import com.android.inputmethod.compat.CompatUtils.ToBooleanMethodWrapper

object InputConnectionCompatUtils {
    private val sInputConnectionType: CompatUtils.ClassWrapper
    private val sRequestCursorUpdatesMethod: ToBooleanMethodWrapper

    init {
        sInputConnectionType = CompatUtils.ClassWrapper(InputConnection::class.java)
        sRequestCursorUpdatesMethod = sInputConnectionType.getPrimitiveMethod(
            "requestCursorUpdates", false, Int::class.javaPrimitiveType
        )
    }

    fun isRequestCursorUpdatesAvailable(): Boolean {
        return sRequestCursorUpdatesMethod != null
    }

    /**
     * Local copies of some constants in InputConnection until the SDK becomes publicly available.
     */
    private val CURSOR_UPDATE_IMMEDIATE: Int = 1 shl 0
    private val CURSOR_UPDATE_MONITOR: Int = 1 shl 1

    private fun requestCursorUpdatesImpl(
        inputConnection: InputConnection?,
        cursorUpdateMode: Int
    ): Boolean {
        if (!isRequestCursorUpdatesAvailable()) {
            return false
        }
        return sRequestCursorUpdatesMethod.invoke(inputConnection, cursorUpdateMode)
    }

    /**
     * Requests the editor to call back [InputMethodManager.updateCursorAnchorInfo].
     * @param inputConnection the input connection to which the request is to be sent.
     * @param enableMonitor `true` to request the editor to call back the method whenever the
     * cursor/anchor position is changed.
     * @param requestImmediateCallback `true` to request the editor to call back the method
     * as soon as possible to notify the current cursor/anchor position to the input method.
     * @return `false` if the request is not handled. Otherwise returns `true`.
     */
    fun requestCursorUpdates(
        inputConnection: InputConnection?,
        enableMonitor: Boolean, requestImmediateCallback: Boolean
    ): Boolean {
        val cursorUpdateMode: Int = ((if (enableMonitor) CURSOR_UPDATE_MONITOR else 0)
                or (if (requestImmediateCallback) CURSOR_UPDATE_IMMEDIATE else 0))
        return requestCursorUpdatesImpl(inputConnection, cursorUpdateMode)
    }
}
