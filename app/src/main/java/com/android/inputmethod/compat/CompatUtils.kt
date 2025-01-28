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

import android.text.TextUtils
import android.util.Log
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object CompatUtils {
    private val TAG: String = CompatUtils::class.java.getSimpleName()

    fun getClass(className: String): Class<*>? {
        try {
            return Class.forName(className)
        } catch (e: ClassNotFoundException) {
            return null
        }
    }

    fun getMethod(
        targetClass: Class<*>?, name: String?,
        vararg parameterTypes: Class<*>?
    ): Method? {
        if (targetClass == null || TextUtils.isEmpty(name)) {
            return null
        }
        try {
            return targetClass.getMethod(name, *parameterTypes)
        } catch (e: SecurityException) {
            // ignore
        } catch (e: NoSuchMethodException) {
        }
        return null
    }

    fun getField(targetClass: Class<*>?, name: String?): Field? {
        if (targetClass == null || TextUtils.isEmpty(name)) {
            return null
        }
        try {
            return targetClass.getField(name)
        } catch (e: SecurityException) {
            // ignore
        } catch (e: NoSuchFieldException) {
        }
        return null
    }

    fun getConstructor(
        targetClass: Class<*>?,
        vararg types: Class<*>?
    ): Constructor<*>? {
        if (targetClass == null || types == null) {
            return null
        }
        try {
            return targetClass.getConstructor(*types)
        } catch (e: SecurityException) {
            // ignore
        } catch (e: NoSuchMethodException) {
        }
        return null
    }

    fun newInstance(constructor: Constructor<*>?, vararg args: Any?): Any? {
        if (constructor == null) {
            return null
        }
        try {
            return constructor.newInstance(*args)
        } catch (e: InstantiationException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Exception in newInstance", e)
        }
        return null
    }

    fun invoke(
        receiver: Any?, defaultValue: Any?,
        method: Method?, vararg args: Any?
    ): Any? {
        if (method == null) {
            return defaultValue
        }
        try {
            return method.invoke(receiver, *args)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in invoke", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in invoke", e)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Exception in invoke", e)
        }
        return defaultValue
    }

    fun getFieldValue(
        receiver: Any?, defaultValue: Any?,
        field: Field?
    ): Any? {
        if (field == null) {
            return defaultValue
        }
        try {
            return field.get(receiver)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in getFieldValue", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in getFieldValue", e)
        }
        return defaultValue
    }

    fun setFieldValue(receiver: Any?, field: Field?, value: Any?) {
        if (field == null) {
            return
        }
        try {
            field.set(receiver, value)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in setFieldValue", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in setFieldValue", e)
        }
    }

    fun getClassWrapper(className: String): ClassWrapper {
        return ClassWrapper(getClass(className))
    }

    class ClassWrapper(targetClass: Class<*>?) {
        private val mClass: Class<*>?

        init {
            mClass = targetClass
        }

        fun exists(): Boolean {
            return mClass != null
        }

        fun <T> getMethod(
            name: String?,
            defaultValue: T, vararg parameterTypes: Class<*>?
        ): ToObjectMethodWrapper<T> {
            return ToObjectMethodWrapper(
                getMethod(mClass, name, *parameterTypes),
                defaultValue
            )
        }

        fun getPrimitiveMethod(
            name: String?, defaultValue: Int,
            vararg parameterTypes: Class<*>?
        ): ToIntMethodWrapper {
            return ToIntMethodWrapper(
                getMethod(mClass, name, *parameterTypes),
                defaultValue
            )
        }

        fun getPrimitiveMethod(
            name: String?, defaultValue: Float,
            vararg parameterTypes: Class<*>?
        ): ToFloatMethodWrapper {
            return ToFloatMethodWrapper(
                getMethod(mClass, name, *parameterTypes),
                defaultValue
            )
        }

        fun getPrimitiveMethod(
            name: String?,
            defaultValue: Boolean, vararg parameterTypes: Class<*>?
        ): ToBooleanMethodWrapper {
            return ToBooleanMethodWrapper(
                getMethod(mClass, name, *parameterTypes),
                defaultValue
            )
        }
    }

    class ToObjectMethodWrapper<T>(method: Method?, defaultValue: T) {
        private val mMethod: Method?
        private val mDefaultValue: T

        init {
            mMethod = method
            mDefaultValue = defaultValue
        }

        fun invoke(receiver: Any?, vararg args: Any?): T? {
            return CompatUtils.invoke(receiver, mDefaultValue, mMethod, *args) as T?
        }
    }

    class ToIntMethodWrapper(method: Method?, defaultValue: Int) {
        private val mMethod: Method?
        private val mDefaultValue: Int

        init {
            mMethod = method
            mDefaultValue = defaultValue
        }

        fun invoke(receiver: Any?, vararg args: Any?): Int {
            return CompatUtils.invoke(receiver, mDefaultValue, mMethod, *args) as Int
        }
    }

    class ToFloatMethodWrapper(method: Method?, defaultValue: Float) {
        private val mMethod: Method? = method
        private val mDefaultValue: Float = defaultValue

        fun invoke(receiver: Any?, vararg args: Any?): Float {
            return CompatUtils.invoke(receiver, mDefaultValue, mMethod, *args) as Float
        }
    }

    class ToBooleanMethodWrapper(method: Method?, defaultValue: Boolean) {
        private val mMethod: Method?
        private val mDefaultValue: Boolean

        init {
            mMethod = method
            mDefaultValue = defaultValue
        }

        fun invoke(receiver: Any?, vararg args: Any?): Boolean {
            return CompatUtils.invoke(receiver, mDefaultValue, mMethod, *args) as Boolean
        }
    }
}
