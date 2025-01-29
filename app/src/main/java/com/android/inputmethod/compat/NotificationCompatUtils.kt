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

import android.app.Notification
import android.os.Build
import android.os.Build.VERSION_CODES
import java.lang.reflect.Field
import java.lang.reflect.Method

object NotificationCompatUtils {
    // Note that TextInfo.getCharSequence() is supposed to be available in API level 21 and later.
    private val METHOD_setColor: Method? = CompatUtils.getMethod(
        Notification.Builder::class.java, "setColor",
        Int::class.javaPrimitiveType
    )
    private val METHOD_setVisibility: Method? = CompatUtils.getMethod(
        Notification.Builder::class.java, "setVisibility",
        Int::class.javaPrimitiveType
    )
    private val METHOD_setCategory: Method? = CompatUtils.getMethod(
        Notification.Builder::class.java, "setCategory",
        String::class.java
    )
    private val METHOD_setPriority: Method? = CompatUtils.getMethod(
        Notification.Builder::class.java, "setPriority",
        Int::class.javaPrimitiveType
    )
    private val METHOD_build: Method? =
        CompatUtils.getMethod(Notification.Builder::class.java, "build")
    private val FIELD_VISIBILITY_SECRET: Field? =
        CompatUtils.getField(Notification::class.java, "VISIBILITY_SECRET")
    private val VISIBILITY_SECRET: Int =
        if (null == FIELD_VISIBILITY_SECRET) 0
        else CompatUtils.getFieldValue(
            null, null, FIELD_VISIBILITY_SECRET
        ) as Int
    private val FIELD_CATEGORY_RECOMMENDATION: Field? =
        CompatUtils.getField(
            Notification::class.java, "CATEGORY_RECOMMENDATION"
        )
    private val CATEGORY_RECOMMENDATION: String =
        if (null == FIELD_CATEGORY_RECOMMENDATION) ""
        else CompatUtils.getFieldValue(
            null, null, FIELD_CATEGORY_RECOMMENDATION
        ) as String
    private val FIELD_PRIORITY_LOW: Field? =
        CompatUtils.getField(Notification::class.java, "PRIORITY_LOW")
    private val PRIORITY_LOW: Int =
        if (null == FIELD_PRIORITY_LOW) 0
        else CompatUtils.getFieldValue(
            null, null, FIELD_PRIORITY_LOW
        ) as Int

    // Sets the accent color
    fun setColor(builder: Notification.Builder?, color: Int) {
        CompatUtils.invoke(builder, null, METHOD_setColor, color)
    }

    fun setVisibilityToSecret(builder: Notification.Builder?) {
        CompatUtils.invoke(builder, null, METHOD_setVisibility, VISIBILITY_SECRET)
    }

    fun setCategoryToRecommendation(builder: Notification.Builder?) {
        CompatUtils.invoke(builder, null, METHOD_setCategory, CATEGORY_RECOMMENDATION)
    }

    fun setPriorityToLow(builder: Notification.Builder?) {
        CompatUtils.invoke(builder, null, METHOD_setPriority, PRIORITY_LOW)
    }

    @Suppress("deprecation")
    fun build(builder: Notification.Builder): Notification? {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            // #build was added in API level 16, JELLY_BEAN
            return CompatUtils.invoke(builder, null, METHOD_build) as Notification?
        }
        // #getNotification was deprecated in API level 16, JELLY_BEAN
        return builder.getNotification()
    }
}
