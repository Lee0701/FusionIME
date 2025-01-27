/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.view.textservice.SuggestionsInfo
import java.lang.reflect.Field

object SuggestionsInfoCompatUtils {
    // Note that SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS has been introduced
    // in API level 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1).
    private val FIELD_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS: Field? = CompatUtils.getField(
        SuggestionsInfo::class.java, "RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS"
    )
    private val OBJ_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS: Int = CompatUtils.getFieldValue(
        null,  /* receiver */null,  /* defaultValue */
        FIELD_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
    ) as Int
    private val RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS: Int =
        if (OBJ_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS != null)
            OBJ_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
        else
            0

    /**
     * Returns the flag value of the attributes of the suggestions that can be obtained by
     * [SuggestionsInfo.getSuggestionsAttributes]: this tells that the text service thinks
     * the result suggestions include highly recommended ones.
     */
    fun getValueOf_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS(): Int {
        return RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
    }
}
