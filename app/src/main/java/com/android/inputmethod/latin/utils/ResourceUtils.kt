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
package com.android.inputmethod.latin.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowManager
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.settings.SettingsValues
import java.util.regex.PatternSyntaxException
import kotlin.math.max
import kotlin.math.min

object ResourceUtils {
    private val TAG: String = ResourceUtils::class.java.simpleName

    const val UNDEFINED_RATIO: Float = -1.0f
    const val UNDEFINED_DIMENSION: Int = -1

    private val sDeviceOverrideValueMap = HashMap<String, String?>()

    private val BUILD_KEYS_AND_VALUES = arrayOf(
        "HARDWARE", Build.HARDWARE,
        "MODEL", Build.MODEL,
        "BRAND", Build.BRAND,
        "MANUFACTURER", Build.MANUFACTURER
    )
    private val sBuildKeyValues =
        HashMap<String, String>()
    private val sBuildKeyValuesDebugString: String

    init {
        val keyValuePairs = ArrayList<String?>()
        val keyCount = BUILD_KEYS_AND_VALUES.size / 2
        for (i in 0 until keyCount) {
            val index = i * 2
            val key = BUILD_KEYS_AND_VALUES[index]
            val value = BUILD_KEYS_AND_VALUES[index + 1]
            sBuildKeyValues[key] = value
            keyValuePairs.add("$key=$value")
        }
        sBuildKeyValuesDebugString = "[" + TextUtils.join(" ", keyValuePairs) + "]"
    }

    fun getDeviceOverrideValue(
        res: Resources, overrideResId: Int,
        defaultValue: String?
    ): String? {
        val orientation = res.configuration.orientation
        val key = "$overrideResId-$orientation"
        if (sDeviceOverrideValueMap.containsKey(key)) {
            return sDeviceOverrideValueMap[key]
        }

        val overrideArray = res.getStringArray(overrideResId)
        val overrideValue = findConstantForKeyValuePairs(sBuildKeyValues, overrideArray)
        // The overrideValue might be an empty string.
        if (overrideValue != null) {
            Log.i(
                TAG, ("Find override value:"
                        + " resource=" + res.getResourceEntryName(overrideResId)
                        + " build=" + sBuildKeyValuesDebugString
                        + " override=" + overrideValue)
            )
            sDeviceOverrideValueMap[key] =
                overrideValue
            return overrideValue
        }

        sDeviceOverrideValueMap[key] =
            defaultValue
        return defaultValue
    }

    /**
     * Find the condition that fulfills specified key value pairs from an array of
     * "condition,constant", and return the corresponding string constant. A condition is
     * "pattern1[:pattern2...] (or an empty string for the default). A pattern is
     * "key=regexp_value" string. The condition matches only if all patterns of the condition
     * are true for the specified key value pairs.
     *
     * For example, "condition,constant" has the following format.
     * - HARDWARE=mako,constantForNexus4
     * - MODEL=Nexus 4:MANUFACTURER=LGE,constantForNexus4
     * - ,defaultConstant
     *
     * @param keyValuePairs attributes to be used to look for a matched condition.
     * @param conditionConstantArray an array of "condition,constant" elements to be searched.
     * @return the constant part of the matched "condition,constant" element. Returns null if no
     * condition matches.
     * @see com.android.inputmethod.latin.utils.ResourceUtilsTests.testFindConstantForKeyValuePairsRegexp
     */
    @UsedForTesting
    fun findConstantForKeyValuePairs(
        keyValuePairs: HashMap<String, String>?,
        conditionConstantArray: Array<String>?
    ): String? {
        if (conditionConstantArray == null || keyValuePairs == null) {
            return null
        }
        var foundValue: String? = null
        for (conditionConstant in conditionConstantArray) {
            val posComma = conditionConstant.indexOf(',')
            if (posComma < 0) {
                Log.w(
                    TAG,
                    "Array element has no comma: $conditionConstant"
                )
                continue
            }
            val condition = conditionConstant.substring(0, posComma)
            if (condition.isEmpty()) {
                Log.w(
                    TAG,
                    "Array element has no condition: $conditionConstant"
                )
                continue
            }
            try {
                if (fulfillsCondition(keyValuePairs, condition)) {
                    // Take first match
                    if (foundValue == null) {
                        foundValue = conditionConstant.substring(posComma + 1)
                    }
                    // And continue walking through all conditions.
                }
            } catch (e: DeviceOverridePatternSyntaxError) {
                Log.w(TAG, "Syntax error, ignored", e)
            }
        }
        return foundValue
    }

    @Throws(DeviceOverridePatternSyntaxError::class)
    private fun fulfillsCondition(
        keyValuePairs: HashMap<String, String>,
        condition: String
    ): Boolean {
        val patterns = condition.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // Check all patterns in a condition are true
        var matchedAll = true
        for (pattern in patterns) {
            val posEqual = pattern.indexOf('=')
            if (posEqual < 0) {
                throw DeviceOverridePatternSyntaxError("Pattern has no '='", condition)
            }
            val key = pattern.substring(0, posEqual)
            val value = keyValuePairs[key]
                ?: throw DeviceOverridePatternSyntaxError("Unknown key", condition)
            val patternRegexpValue = pattern.substring(posEqual + 1)
            try {
                if (!value.matches(patternRegexpValue.toRegex())) {
                    matchedAll = false
                    // And continue walking through all patterns.
                }
            } catch (e: PatternSyntaxException) {
                throw DeviceOverridePatternSyntaxError("Syntax error", condition, e)
            }
        }
        return matchedAll
    }

    fun getDefaultKeyboardWidth(context: Context): Int {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.VANILLA_ICE_CREAM) {
            // Since Android 15’s edge-to-edge enforcement, window insets should be considered.
            val wm = context.getSystemService(WindowManager::class.java)
            val windowMetrics = wm.currentWindowMetrics
            val insets =
                windowMetrics
                    .windowInsets
                    .getInsetsIgnoringVisibility(
                        WindowInsets.Type.systemBars()
                                or WindowInsets.Type.displayCutout()
                    )
            return windowMetrics.bounds.width() - insets.left - insets.right
        }
        val dm = context.resources.displayMetrics
        return dm.widthPixels
    }

    fun getKeyboardHeight(res: Resources, settingsValues: SettingsValues): Int {
        val defaultKeyboardHeight = getDefaultKeyboardHeight(res)
        if (settingsValues.mHasKeyboardResize) {
            // mKeyboardHeightScale Ranges from [.5,1.2], from xml/prefs_screen_debug.xml
            return (defaultKeyboardHeight * settingsValues.mKeyboardHeightScale).toInt()
        }
        return defaultKeyboardHeight
    }

    fun getDefaultKeyboardHeight(res: Resources): Int {
        val dm = res.displayMetrics
        val keyboardHeightInDp = getDeviceOverrideValue(
            res, R.array.keyboard_heights, null /* defaultValue */
        )
        val keyboardHeight = if (TextUtils.isEmpty(keyboardHeightInDp)) {
            res.getDimension(R.dimen.config_default_keyboard_height)
        } else {
            keyboardHeightInDp!!.toFloat() * dm.density
        }
        val maxKeyboardHeight = res.getFraction(
            R.fraction.config_max_keyboard_height, dm.heightPixels, dm.heightPixels
        )
        var minKeyboardHeight = res.getFraction(
            R.fraction.config_min_keyboard_height, dm.heightPixels, dm.heightPixels
        )
        if (minKeyboardHeight < 0.0f) {
            // Specified fraction was negative, so it should be calculated against display
            // width.
            minKeyboardHeight = -res.getFraction(
                R.fraction.config_min_keyboard_height, dm.widthPixels, dm.widthPixels
            )
        }
        // Keyboard height will not exceed maxKeyboardHeight and will not be less than
        // minKeyboardHeight.
        return max(
            min(keyboardHeight.toDouble(), maxKeyboardHeight.toDouble()),
            minKeyboardHeight.toDouble()
        ) as Int
    }

    fun isValidFraction(fraction: Float): Boolean {
        return fraction >= 0.0f
    }

    // {@link Resources#getDimensionPixelSize(int)} returns at least one pixel size.
    fun isValidDimensionPixelSize(dimension: Int): Boolean {
        return dimension > 0
    }

    // {@link Resources#getDimensionPixelOffset(int)} may return zero pixel offset.
    fun isValidDimensionPixelOffset(dimension: Int): Boolean {
        return dimension >= 0
    }

    fun getFloatFromFraction(res: Resources, fractionResId: Int): Float {
        return res.getFraction(fractionResId, 1, 1)
    }

    fun getFraction(a: TypedArray, index: Int, defValue: Float): Float {
        val value = a.peekValue(index)
        if (value == null || !isFractionValue(value)) {
            return defValue
        }
        return a.getFraction(index, 1, 1, defValue)
    }

    fun getFraction(a: TypedArray, index: Int): Float {
        return getFraction(a, index, UNDEFINED_RATIO)
    }

    fun getDimensionPixelSize(a: TypedArray, index: Int): Int {
        val value = a.peekValue(index)
        if (value == null || !isDimensionValue(value)) {
            return UNDEFINED_DIMENSION
        }
        return a.getDimensionPixelSize(index, UNDEFINED_DIMENSION)
    }

    fun getDimensionOrFraction(
        a: TypedArray, index: Int, base: Int,
        defValue: Float
    ): Float {
        val value = a.peekValue(index) ?: return defValue
        if (isFractionValue(value)) {
            return a.getFraction(index, base, base, defValue)
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue)
        }
        return defValue
    }

    fun getEnumValue(a: TypedArray, index: Int, defValue: Int): Int {
        val value = a.peekValue(index) ?: return defValue
        if (isIntegerValue(value)) {
            return a.getInt(index, defValue)
        }
        return defValue
    }

    fun isFractionValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_FRACTION
    }

    fun isDimensionValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_DIMENSION
    }

    fun isIntegerValue(v: TypedValue): Boolean {
        return v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT
    }

    fun isStringValue(v: TypedValue): Boolean {
        return v.type == TypedValue.TYPE_STRING
    }

    internal class DeviceOverridePatternSyntaxError @JvmOverloads constructor(
        message: String, expression: String,
        throwable: Throwable? = null
    ) :
        Exception("$message: $expression", throwable)
}
