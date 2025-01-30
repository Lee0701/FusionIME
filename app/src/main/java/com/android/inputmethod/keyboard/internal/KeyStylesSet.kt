/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.inputmethod.keyboard.internal

import android.content.res.TypedArray
import android.util.Log
import android.util.SparseArray
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.utils.XmlParseUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

class KeyStylesSet(textsSet: KeyboardTextsSet) {
    private val mStyles: HashMap<String, KeyStyle> = HashMap()

    private val mTextsSet: KeyboardTextsSet

    private val mEmptyKeyStyle: KeyStyle

    init {
        mTextsSet = textsSet
        mEmptyKeyStyle = EmptyKeyStyle(textsSet)
        mStyles.put(EMPTY_STYLE_NAME, mEmptyKeyStyle)
    }

    private class EmptyKeyStyle(textsSet: KeyboardTextsSet) : KeyStyle(textsSet) {
        override fun getStringArray(a: TypedArray, index: Int): Array<String?>? {
            return parseStringArray(a, index)
        }

        override fun getString(a: TypedArray, index: Int): String? {
            return parseString(a, index)
        }

        override fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int {
            return a.getInt(index, defaultValue)
        }

        override fun getFlags(a: TypedArray, index: Int): Int {
            return a.getInt(index, 0)
        }
    }

    private class DeclaredKeyStyle(
        parentStyleName: String,
        textsSet: KeyboardTextsSet,
        styles: HashMap<String, KeyStyle>
    ) : KeyStyle(textsSet) {
        private val mStyles: HashMap<String, KeyStyle>
        private val mParentStyleName: String
        private val mStyleAttributes: SparseArray<Any?> = SparseArray()

        init {
            mParentStyleName = parentStyleName
            mStyles = styles
        }

        override fun getStringArray(a: TypedArray, index: Int): Array<String?>? {
            if (a.hasValue(index)) {
                return parseStringArray(a, index)
            }
            val value: Any? = mStyleAttributes.get(index)
            if (value != null) {
                val array: Array<String> = value as Array<String>
                return array.copyOf(array.size)
            }
            val parentStyle: KeyStyle? = mStyles.get(mParentStyleName)
            return parentStyle!!.getStringArray(a, index)
        }

        override fun getString(a: TypedArray, index: Int): String? {
            if (a.hasValue(index)) {
                return parseString(a, index)
            }
            val value: Any? = mStyleAttributes.get(index)
            if (value != null) {
                return value as String
            }
            val parentStyle: KeyStyle? = mStyles.get(mParentStyleName)
            return parentStyle!!.getString(a, index)
        }

        override fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int {
            if (a.hasValue(index)) {
                return a.getInt(index, defaultValue)
            }
            val value: Any? = mStyleAttributes.get(index)
            if (value != null) {
                return value as Int
            }
            val parentStyle: KeyStyle? = mStyles.get(mParentStyleName)
            return parentStyle!!.getInt(a, index, defaultValue)
        }

        override fun getFlags(a: TypedArray, index: Int): Int {
            val parentFlags: Int = mStyles.get(mParentStyleName)!!.getFlags(a, index)
            val value: Int? = mStyleAttributes.get(index) as Int?
            val styleFlags: Int = if ((value != null)) value else 0
            val flags: Int = a.getInt(index, 0)
            return flags or styleFlags or parentFlags
        }

        fun readKeyAttributes(keyAttr: TypedArray) {
            // TODO: Currently not all Key attributes can be declared as style.
            readString(keyAttr, R.styleable.Keyboard_Key_altCode)
            readString(keyAttr, R.styleable.Keyboard_Key_keySpec)
            readString(keyAttr, R.styleable.Keyboard_Key_keyHintLabel)
            readStringArray(keyAttr, R.styleable.Keyboard_Key_moreKeys)
            readStringArray(keyAttr, R.styleable.Keyboard_Key_additionalMoreKeys)
            readFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags)
            readString(keyAttr, R.styleable.Keyboard_Key_keyIconDisabled)
            readInt(keyAttr, R.styleable.Keyboard_Key_maxMoreKeysColumn)
            readInt(keyAttr, R.styleable.Keyboard_Key_backgroundType)
            readFlags(keyAttr, R.styleable.Keyboard_Key_keyActionFlags)
        }

        fun readString(a: TypedArray, index: Int) {
            if (a.hasValue(index)) {
                mStyleAttributes.put(index, parseString(a, index))
            }
        }

        fun readInt(a: TypedArray, index: Int) {
            if (a.hasValue(index)) {
                mStyleAttributes.put(index, a.getInt(index, 0))
            }
        }

        fun readFlags(a: TypedArray, index: Int) {
            if (a.hasValue(index)) {
                val styleFlags: Int = mStyleAttributes.get(index) as Int? ?: 0
                mStyleAttributes.put(index, a.getInt(index, 0) or styleFlags)
            }
        }

        fun readStringArray(a: TypedArray, index: Int) {
            if (a.hasValue(index)) {
                mStyleAttributes.put(index, parseStringArray(a, index))
            }
        }
    }

    @Throws(XmlPullParserException::class)
    fun parseKeyStyleAttributes(
        keyStyleAttr: TypedArray, keyAttrs: TypedArray,
        parser: XmlPullParser
    ) {
        val styleName: String = keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_styleName)
            ?: throw XmlParseUtils.ParseException(
                KeyboardBuilder.TAG_KEY_STYLE + " has no styleName attribute", parser
            )
        if (DEBUG) {
            Log.d(
                TAG, String.format(
                    "<%s styleName=%s />",
                    KeyboardBuilder.TAG_KEY_STYLE, styleName
                )
            )
            if (mStyles.containsKey(styleName)) {
                Log.d(
                    TAG,
                    (KeyboardBuilder.TAG_KEY_STYLE + " " + styleName + " is overridden at "
                            + parser.getPositionDescription())
                )
            }
        }

        val parentStyleInAttr: String? = keyStyleAttr.getString(
            R.styleable.Keyboard_KeyStyle_parentStyle
        )
        if (parentStyleInAttr != null && !mStyles.containsKey(parentStyleInAttr)) {
            throw XmlParseUtils.ParseException(
                "Unknown parentStyle " + parentStyleInAttr, parser
            )
        }
        val parentStyleName: String = if ((parentStyleInAttr == null))
            EMPTY_STYLE_NAME
        else
            parentStyleInAttr
        val style: DeclaredKeyStyle = DeclaredKeyStyle(parentStyleName, mTextsSet, mStyles)
        style.readKeyAttributes(keyAttrs)
        mStyles.put(styleName, style)
    }

    @Throws(XmlParseUtils.ParseException::class)
    fun getKeyStyle(keyAttr: TypedArray, parser: XmlPullParser): KeyStyle {
        val styleName: String? = keyAttr.getString(R.styleable.Keyboard_Key_keyStyle)
        if (styleName == null) {
            return mEmptyKeyStyle
        }
        val style: KeyStyle? = mStyles.get(styleName)
        if (style == null) {
            throw XmlParseUtils.ParseException("Unknown key style: " + styleName, parser)
        }
        return style
    }

    companion object {
        private val TAG: String = KeyStylesSet::class.java.getSimpleName()
        private const val DEBUG: Boolean = false

        private const val EMPTY_STYLE_NAME: String = "<empty>"
    }
}
