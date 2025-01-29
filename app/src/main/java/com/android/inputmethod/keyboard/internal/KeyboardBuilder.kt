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
package com.android.inputmethod.keyboard.internal

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Key.Spacer
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardId
import com.android.inputmethod.keyboard.KeyboardTheme
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.utils.ResourceUtils
import com.android.inputmethod.latin.utils.XmlParseUtils
import com.android.inputmethod.latin.utils.XmlParseUtils.IllegalAttribute
import com.android.inputmethod.latin.utils.XmlParseUtils.IllegalEndTag
import com.android.inputmethod.latin.utils.XmlParseUtils.IllegalStartTag
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Locale
import kotlin.math.max

/**
 * Keyboard Building helper.
 *
 * This class parses Keyboard XML file and eventually build a Keyboard.
 * The Keyboard XML file looks like:
 * <pre>
 * &lt;!-- xml/keyboard.xml --&gt;
 * &lt;Keyboard keyboard_attributes*&gt;
 * &lt;!-- Keyboard Content --&gt;
 * &lt;Row row_attributes*&gt;
 * &lt;!-- Row Content --&gt;
 * &lt;Key key_attributes* /&gt;
 * &lt;Spacer horizontalGap="32.0dp" /&gt;
 * &lt;include keyboardLayout="@xml/other_keys"&gt;
 * ...
 * &lt;/Row&gt;
 * &lt;include keyboardLayout="@xml/other_rows"&gt;
 * ...
 * &lt;/Keyboard&gt;
</pre> *
 * The XML file which is included in other file must have &lt;merge&gt; as root element,
 * such as:
 * <pre>
 * &lt;!-- xml/other_keys.xml --&gt;
 * &lt;merge&gt;
 * &lt;Key key_attributes* /&gt;
 * ...
 * &lt;/merge&gt;
</pre> *
 * and
 * <pre>
 * &lt;!-- xml/other_rows.xml --&gt;
 * &lt;merge&gt;
 * &lt;Row row_attributes*&gt;
 * &lt;Key key_attributes* /&gt;
 * &lt;/Row&gt;
 * ...
 * &lt;/merge&gt;
</pre> *
 * You can also use switch-case-default tags to select Rows and Keys.
 * <pre>
 * &lt;switch&gt;
 * &lt;case case_attribute*&gt;
 * &lt;!-- Any valid tags at switch position --&gt;
 * &lt;/case&gt;
 * ...
 * &lt;default&gt;
 * &lt;!-- Any valid tags at switch position --&gt;
 * &lt;/default&gt;
 * &lt;/switch&gt;
</pre> *
 * You can declare Key style and specify styles within Key tags.
 * <pre>
 * &lt;switch&gt;
 * &lt;case mode="email"&gt;
 * &lt;key-style styleName="f1-key" parentStyle="modifier-key"
 * keyLabel=".com"
 * /&gt;
 * &lt;/case&gt;
 * &lt;case mode="url"&gt;
 * &lt;key-style styleName="f1-key" parentStyle="modifier-key"
 * keyLabel="http://"
 * /&gt;
 * &lt;/case&gt;
 * &lt;/switch&gt;
 * ...
 * &lt;Key keyStyle="shift-key" ... /&gt;
</pre> *
 */
// TODO: Write unit tests for this class.
open class KeyboardBuilder<KP : KeyboardParams>(context: Context, params: KP) {
    protected val mParams: KP
    protected val mContext: Context
    protected val mResources: Resources

    private var mCurrentY: Int = 0
    private var mCurrentRow: KeyboardRow? = null
    private var mLeftEdge: Boolean = false
    private var mTopEdge: Boolean = false
    private var mRightEdgeKey: Key? = null

    fun setAllowRedundantMoreKes(enabled: Boolean) {
        mParams.mAllowRedundantMoreKeys = enabled
    }

    fun load(xmlId: Int, id: KeyboardId?): KeyboardBuilder<KP> {
        mParams.mId = id
        val parser: XmlResourceParser = mResources.getXml(xmlId)
        try {
            parseKeyboard(parser)
        } catch (e: XmlPullParserException) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e)
            throw IllegalArgumentException(e.message, e)
        } catch (e: IOException) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e)
            throw RuntimeException(e.message, e)
        } finally {
            parser.close()
        }
        return this
    }

    @UsedForTesting
    fun disableTouchPositionCorrectionDataForTest() {
        mParams.mTouchPositionCorrection.setEnabled(false)
    }

    fun setProximityCharsCorrectionEnabled(enabled: Boolean) {
        mParams.mProximityCharsCorrectionEnabled = enabled
    }

    open fun build(): Keyboard {
        return Keyboard(mParams)
    }

    private var mIndent: Int = 0

    init {
        mContext = context
        val res: Resources = context.getResources()
        mResources = res

        mParams = params

        params.GRID_WIDTH = res.getInteger(R.integer.config_keyboard_grid_width)
        params.GRID_HEIGHT = res.getInteger(R.integer.config_keyboard_grid_height)
    }

    private fun startTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, *args))
    }

    private fun endTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(mIndent-- * 2) + format, *args))
    }

    private fun startEndTag(format: String, vararg args: Any) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, *args))
        mIndent--
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyboard(parser: XmlPullParser) {
        if (DEBUG) startTag(
            "<%s> %s", TAG_KEYBOARD,
            mParams.mId!!
        )
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val event: Int = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag: String = parser.getName()
                if (TAG_KEYBOARD == tag) {
                    parseKeyboardAttributes(parser)
                    startKeyboard()
                    parseKeyboardContent(parser, false)
                    return
                }
                throw IllegalStartTag(parser, tag, TAG_KEYBOARD)
            }
        }
    }

    private fun parseKeyboardAttributes(parser: XmlPullParser) {
        val attr: AttributeSet = Xml.asAttributeSet(parser)
        val keyboardAttr: TypedArray = mContext.obtainStyledAttributes(
            attr, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard
        )
        val keyAttr: TypedArray = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try {
            val params: KeyboardParams = mParams
            val height: Int = params.mId!!.mHeight
            val width: Int = params.mId!!.mWidth
            params.mOccupiedHeight = height
            params.mOccupiedWidth = width
            params.mTopPadding = keyboardAttr.getFraction(
                R.styleable.Keyboard_keyboardTopPadding, height, height, 0f
            ).toInt()
            params.mBottomPadding = keyboardAttr.getFraction(
                R.styleable.Keyboard_keyboardBottomPadding, height, height, 0f
            ).toInt()
            params.mLeftPadding = keyboardAttr.getFraction(
                R.styleable.Keyboard_keyboardLeftPadding, width, width, 0f
            ).toInt()
            params.mRightPadding = keyboardAttr.getFraction(
                R.styleable.Keyboard_keyboardRightPadding, width, width, 0f
            ).toInt()

            val baseWidth: Int =
                params.mOccupiedWidth - params.mLeftPadding - params.mRightPadding
            params.mBaseWidth = baseWidth
            params.mDefaultKeyWidth = keyAttr.getFraction(
                R.styleable.Keyboard_Key_keyWidth,
                baseWidth, baseWidth, (baseWidth / DEFAULT_KEYBOARD_COLUMNS).toFloat()
            ).toInt()
            params.mHorizontalGap = keyboardAttr.getFraction(
                R.styleable.Keyboard_horizontalGap, baseWidth, baseWidth, 0f
            ).toInt()
            // TODO: Fix keyboard geometry calculation clearer. Historically vertical gap between
            // rows are determined based on the entire keyboard height including top and bottom
            // paddings.
            params.mVerticalGap = keyboardAttr.getFraction(
                R.styleable.Keyboard_verticalGap, height, height, 0f
            ).toInt()
            val baseHeight: Int = (params.mOccupiedHeight - params.mTopPadding
                    - params.mBottomPadding) + params.mVerticalGap
            params.mBaseHeight = baseHeight
            params.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(
                keyboardAttr,
                R.styleable.Keyboard_rowHeight,
                baseHeight,
                (baseHeight / DEFAULT_KEYBOARD_ROWS).toFloat()
            ).toInt()

            params.mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr)

            params.mMoreKeysTemplate = keyboardAttr.getResourceId(
                R.styleable.Keyboard_moreKeysTemplate, 0
            )
            params.mMaxMoreKeysKeyboardColumn = keyAttr.getInt(
                R.styleable.Keyboard_Key_maxMoreKeysColumn, 5
            )

            params.mThemeId = keyboardAttr.getInt(R.styleable.Keyboard_themeId, 0)
            params.mIconsSet.loadIcons(keyboardAttr)
            params.mTextsSet.setLocale(params.mId!!.getLocale(), mContext)

            val resourceId: Int = keyboardAttr.getResourceId(
                R.styleable.Keyboard_touchPositionCorrectionData, 0
            )
            if (resourceId != 0) {
                val data: Array<String> = mResources.getStringArray(resourceId)
                params.mTouchPositionCorrection.load(data)
            }
        } finally {
            keyAttr.recycle()
            keyboardAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            val event: Int = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag: String = parser.getName()
                if (TAG_ROW == tag) {
                    val row: KeyboardRow = parseRowAttributes(parser)
                    if (DEBUG) startTag("<%s>%s", TAG_ROW, if (skip) " skipped" else "")
                    if (!skip) {
                        startRow(row)
                    }
                    parseRowContent(parser, row, skip)
                } else if (TAG_GRID_ROWS == tag) {
                    if (DEBUG) startTag("<%s>%s", TAG_GRID_ROWS, if (skip) " skipped" else "")
                    parseGridRows(parser, skip)
                } else if (TAG_INCLUDE == tag) {
                    parseIncludeKeyboardContent(parser, skip)
                } else if (TAG_SWITCH == tag) {
                    parseSwitchKeyboardContent(parser, skip)
                } else if (TAG_KEY_STYLE == tag) {
                    parseKeyStyle(parser, skip)
                } else {
                    throw IllegalStartTag(parser, tag, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag: String = parser.getName()
                if (DEBUG) endTag("</%s>", tag)
                if (TAG_KEYBOARD == tag) {
                    endKeyboard()
                    return
                }
                if (TAG_CASE == tag || TAG_DEFAULT == tag || TAG_MERGE == tag) {
                    return
                }
                throw IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    @Throws(XmlPullParserException::class)
    private fun parseRowAttributes(parser: XmlPullParser): KeyboardRow {
        val attr: AttributeSet = Xml.asAttributeSet(parser)
        val keyboardAttr: TypedArray = mResources.obtainAttributes(attr, R.styleable.Keyboard)
        try {
            if (keyboardAttr.hasValue(R.styleable.Keyboard_horizontalGap)) {
                throw IllegalAttribute(parser, TAG_ROW, "horizontalGap")
            }
            if (keyboardAttr.hasValue(R.styleable.Keyboard_verticalGap)) {
                throw IllegalAttribute(parser, TAG_ROW, "verticalGap")
            }
            return KeyboardRow(mResources, mParams, parser, mCurrentY)
        } finally {
            keyboardAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseRowContent(
        parser: XmlPullParser, row: KeyboardRow,
        skip: Boolean
    ) {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            val event: Int = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag: String = parser.getName()
                if (TAG_KEY == tag) {
                    parseKey(parser, row, skip)
                } else if (TAG_SPACER == tag) {
                    parseSpacer(parser, row, skip)
                } else if (TAG_INCLUDE == tag) {
                    parseIncludeRowContent(parser, row, skip)
                } else if (TAG_SWITCH == tag) {
                    parseSwitchRowContent(parser, row, skip)
                } else if (TAG_KEY_STYLE == tag) {
                    parseKeyStyle(parser, skip)
                } else {
                    throw IllegalStartTag(parser, tag, TAG_ROW)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag: String = parser.getName()
                if (DEBUG) endTag("</%s>", tag)
                if (TAG_ROW == tag) {
                    if (!skip) {
                        endRow(row)
                    }
                    return
                }
                if (TAG_CASE == tag || TAG_DEFAULT == tag || TAG_MERGE == tag) {
                    return
                }
                throw IllegalEndTag(parser, tag, TAG_ROW)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseGridRows(parser: XmlPullParser, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_GRID_ROWS, parser)
            if (DEBUG) {
                startEndTag("<%s /> skipped", TAG_GRID_ROWS)
            }
            return
        }
        val gridRows: KeyboardRow = KeyboardRow(mResources, mParams, parser, mCurrentY)
        val gridRowAttr: TypedArray = mResources.obtainAttributes(
            Xml.asAttributeSet(parser), R.styleable.Keyboard_GridRows
        )
        val codesArrayId: Int = gridRowAttr.getResourceId(
            R.styleable.Keyboard_GridRows_codesArray, 0
        )
        val textsArrayId: Int = gridRowAttr.getResourceId(
            R.styleable.Keyboard_GridRows_textsArray, 0
        )
        gridRowAttr.recycle()
        if (codesArrayId == 0 && textsArrayId == 0) {
            throw XmlParseUtils.ParseException(
                "Missing codesArray or textsArray attributes", parser
            )
        }
        if (codesArrayId != 0 && textsArrayId != 0) {
            throw XmlParseUtils.ParseException(
                "Both codesArray and textsArray attributes specifed", parser
            )
        }
        val array: Array<String> = mResources.getStringArray(
            if (codesArrayId != 0) codesArrayId else textsArrayId
        )
        val counts: Int = array.size
        val keyWidth: Float = gridRows.getKeyWidth(null, 0.0f)
        val numColumns: Int = (mParams.mOccupiedWidth / keyWidth).toInt()
        var index: Int = 0
        while (index < counts) {
            val row: KeyboardRow = KeyboardRow(mResources, mParams, parser, mCurrentY)
            startRow(row)
            for (c in 0 until numColumns) {
                val i: Int = index + c
                if (i >= counts) {
                    break
                }
                val label: String
                val code: Int
                val outputText: String?
                val supportedMinSdkVersion: Int
                if (codesArrayId != 0) {
                    val codeArraySpec: String = array.get(i)
                    label = CodesArrayParser.parseLabel(codeArraySpec)
                    code = CodesArrayParser.parseCode(codeArraySpec)
                    outputText = CodesArrayParser.parseOutputText(codeArraySpec)
                    supportedMinSdkVersion =
                        CodesArrayParser.getMinSupportSdkVersion(codeArraySpec)
                } else {
                    val textArraySpec: String = array.get(i)
                    // TODO: Utilize KeySpecParser or write more generic TextsArrayParser.
                    label = textArraySpec
                    code = Constants.CODE_OUTPUT_TEXT
                    outputText = textArraySpec + Constants.CODE_SPACE.toChar()
                    supportedMinSdkVersion = 0
                }
                if (Build.VERSION.SDK_INT < supportedMinSdkVersion) {
                    continue
                }
                val labelFlags: Int = row.getDefaultKeyLabelFlags()
                // TODO: Should be able to assign default keyActionFlags as well.
                val backgroundType: Int = row.getDefaultBackgroundType()
                val x: Int = row.getKeyX(null).toInt()
                val y: Int = row.getKeyY()
                val width: Int = keyWidth.toInt()
                val height: Int = row.getRowHeight()
                val key: Key = Key(
                    label, KeyboardIconsSet.ICON_UNDEFINED, code, outputText,
                    null,  /* hintLabel */labelFlags, backgroundType, x, y, width, height,
                    mParams.mHorizontalGap, mParams.mVerticalGap
                )
                endKey(key)
                row.advanceXPos(keyWidth)
            }
            endRow(row)
            index += numColumns
        }

        XmlParseUtils.checkEndTag(TAG_GRID_ROWS, parser)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKey(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_KEY, parser)
            if (DEBUG) startEndTag("<%s /> skipped", TAG_KEY)
            return
        }
        val keyAttr: TypedArray = mResources.obtainAttributes(
            Xml.asAttributeSet(parser), R.styleable.Keyboard_Key
        )
        val keyStyle: KeyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val keySpec: String? = keyStyle.getString(keyAttr, R.styleable.Keyboard_Key_keySpec)
        if (TextUtils.isEmpty(keySpec)) {
            throw XmlParseUtils.ParseException("Empty keySpec", parser)
        }
        val key: Key = Key(keySpec, keyAttr, keyStyle, mParams, row)
        keyAttr.recycle()
        if (DEBUG) {
            startEndTag(
                "<%s%s %s moreKeys=%s />", TAG_KEY, (if (key.isEnabled) "" else " disabled"),
                key, key.moreKeys.contentToString()
            )
        }
        XmlParseUtils.checkEndTag(TAG_KEY, parser)
        endKey(key)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSpacer(parser: XmlPullParser, row: KeyboardRow, skip: Boolean) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_SPACER, parser)
            if (DEBUG) startEndTag("<%s /> skipped", TAG_SPACER)
            return
        }
        val keyAttr: TypedArray = mResources.obtainAttributes(
            Xml.asAttributeSet(parser), R.styleable.Keyboard_Key
        )
        val keyStyle: KeyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser)
        val spacer: Key = Spacer(keyAttr, keyStyle, mParams, row)
        keyAttr.recycle()
        if (DEBUG) startEndTag("<%s />", TAG_SPACER)
        XmlParseUtils.checkEndTag(TAG_SPACER, parser)
        endKey(spacer)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        parseIncludeInternal(parser, null, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeRowContent(
        parser: XmlPullParser,
        row: KeyboardRow,
        skip: Boolean
    ) {
        parseIncludeInternal(parser, row, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseIncludeInternal(
        parser: XmlPullParser, row: KeyboardRow?,
        skip: Boolean
    ) {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_INCLUDE, parser)
            if (DEBUG) startEndTag("</%s> skipped", TAG_INCLUDE)
            return
        }
        val attr: AttributeSet = Xml.asAttributeSet(parser)
        val keyboardAttr: TypedArray = mResources.obtainAttributes(
            attr, R.styleable.Keyboard_Include
        )
        val keyAttr: TypedArray = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        var keyboardLayout: Int
        try {
            XmlParseUtils.checkAttributeExists(
                keyboardAttr, R.styleable.Keyboard_Include_keyboardLayout, "keyboardLayout",
                TAG_INCLUDE, parser
            )
            keyboardLayout = keyboardAttr.getResourceId(
                R.styleable.Keyboard_Include_keyboardLayout, 0
            )
            if (row != null) {
                // Override current x coordinate.
                row.setXPos(row.getKeyX(keyAttr))
                // Push current Row attributes and update with new attributes.
                row.pushRowAttributes(keyAttr)
            }
        } finally {
            keyboardAttr.recycle()
            keyAttr.recycle()
        }

        XmlParseUtils.checkEndTag(TAG_INCLUDE, parser)
        if (DEBUG) {
            startEndTag(
                "<%s keyboardLayout=%s />", TAG_INCLUDE,
                mResources.getResourceEntryName(keyboardLayout)
            )
        }
        val parserForInclude: XmlResourceParser = mResources.getXml(keyboardLayout)
        try {
            parseMerge(parserForInclude, row, skip)
        } finally {
            if (row != null) {
                // Restore Row attributes.
                row.popRowAttributes()
            }
            parserForInclude.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseMerge(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean) {
        if (DEBUG) startTag("<%s>", TAG_MERGE)
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            val event: Int = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag: String = parser.getName()
                if (TAG_MERGE == tag) {
                    if (row == null) {
                        parseKeyboardContent(parser, skip)
                    } else {
                        parseRowContent(parser, row, skip)
                    }
                    return
                }
                throw XmlParseUtils.ParseException(
                    "Included keyboard layout must have <merge> root element", parser
                )
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchKeyboardContent(parser: XmlPullParser, skip: Boolean) {
        parseSwitchInternal(parser, null, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchRowContent(
        parser: XmlPullParser,
        row: KeyboardRow,
        skip: Boolean
    ) {
        parseSwitchInternal(parser, row, skip)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseSwitchInternal(
        parser: XmlPullParser, row: KeyboardRow?,
        skip: Boolean
    ) {
        if (DEBUG) startTag(
            "<%s> %s", TAG_SWITCH,
            mParams.mId!!
        )
        var selected: Boolean = false
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            val event: Int = parser.next()
            if (event == XmlPullParser.START_TAG) {
                val tag: String = parser.getName()
                if (TAG_CASE == tag) {
                    selected = selected or parseCase(parser, row, if (selected) true else skip)
                } else if (TAG_DEFAULT == tag) {
                    selected = selected or parseDefault(parser, row, if (selected) true else skip)
                } else {
                    throw IllegalStartTag(parser, tag, TAG_SWITCH)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag: String = parser.getName()
                if (TAG_SWITCH == tag) {
                    if (DEBUG) endTag("</%s>", TAG_SWITCH)
                    return
                }
                throw IllegalEndTag(parser, tag, TAG_SWITCH)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCase(parser: XmlPullParser, row: KeyboardRow?, skip: Boolean): Boolean {
        val selected: Boolean = parseCaseCondition(parser)
        if (row == null) {
            // Processing Rows.
            parseKeyboardContent(parser, if (selected) skip else true)
        } else {
            // Processing Keys.
            parseRowContent(parser, row, if (selected) skip else true)
        }
        return selected
    }

    private fun parseCaseCondition(parser: XmlPullParser): Boolean {
        val id: KeyboardId? = mParams.mId
        if (id == null) {
            return true
        }
        val attr: AttributeSet = Xml.asAttributeSet(parser)
        val caseAttr: TypedArray = mResources.obtainAttributes(attr, R.styleable.Keyboard_Case)
        try {
            val keyboardLayoutSetMatched: Boolean = matchString(
                caseAttr,
                R.styleable.Keyboard_Case_keyboardLayoutSet,
                id.mSubtype?.keyboardLayoutSetName!!
            )
            val keyboardLayoutSetElementMatched: Boolean = matchTypedValue(
                caseAttr,
                R.styleable.Keyboard_Case_keyboardLayoutSetElement, id.mElementId,
                KeyboardId.elementIdToName(id.mElementId)!!
            )
            val keyboardThemeMacthed: Boolean = matchTypedValue(
                caseAttr,
                R.styleable.Keyboard_Case_keyboardTheme, mParams.mThemeId,
                KeyboardTheme.getKeyboardThemeName(mParams.mThemeId)
            )
            val modeMatched: Boolean = matchTypedValue(
                caseAttr,
                R.styleable.Keyboard_Case_mode, id.mMode, KeyboardId.modeName(id.mMode)!!
            )
            val navigateNextMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_navigateNext, id.navigateNext()
            )
            val navigatePreviousMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_navigatePrevious, id.navigatePrevious()
            )
            val passwordInputMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_passwordInput, id.passwordInput()
            )
            val clobberSettingsKeyMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_clobberSettingsKey, id.mClobberSettingsKey
            )
            val hasShortcutKeyMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_hasShortcutKey, id.mHasShortcutKey
            )
            val languageSwitchKeyEnabledMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_languageSwitchKeyEnabled,
                id.mLanguageSwitchKeyEnabled
            )
            val isMultiLineMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_isMultiLine, id.isMultiLine()
            )
            val imeActionMatched: Boolean = matchInteger(
                caseAttr,
                R.styleable.Keyboard_Case_imeAction, id.imeAction()
            )
            val isIconDefinedMatched: Boolean = isIconDefined(
                caseAttr,
                R.styleable.Keyboard_Case_isIconDefined, mParams.mIconsSet
            )
            val locale: Locale = id.getLocale()
            val localeCodeMatched: Boolean = matchLocaleCodes(caseAttr, locale)
            val languageCodeMatched: Boolean = matchLanguageCodes(caseAttr, locale)
            val countryCodeMatched: Boolean = matchCountryCodes(caseAttr, locale)
            val splitLayoutMatched: Boolean = matchBoolean(
                caseAttr,
                R.styleable.Keyboard_Case_isSplitLayout, id.mIsSplitLayout
            )
            val selected: Boolean = keyboardLayoutSetMatched && keyboardLayoutSetElementMatched
                    && keyboardThemeMacthed && modeMatched && navigateNextMatched
                    && navigatePreviousMatched && passwordInputMatched && clobberSettingsKeyMatched
                    && hasShortcutKeyMatched && languageSwitchKeyEnabledMatched
                    && isMultiLineMatched && imeActionMatched && isIconDefinedMatched
                    && localeCodeMatched && languageCodeMatched && countryCodeMatched
                    && splitLayoutMatched

            if (DEBUG) {
                startTag(
                    "<%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s>%s", TAG_CASE,
                    textAttr(
                        caseAttr.getString(
                            R.styleable.Keyboard_Case_keyboardLayoutSet
                        ), "keyboardLayoutSet"
                    ),
                    textAttr(
                        caseAttr.getString(
                            R.styleable.Keyboard_Case_keyboardLayoutSetElement
                        ),
                        "keyboardLayoutSetElement"
                    ),
                    textAttr(
                        caseAttr.getString(
                            R.styleable.Keyboard_Case_keyboardTheme
                        ), "keyboardTheme"
                    ),
                    textAttr(caseAttr.getString(R.styleable.Keyboard_Case_mode), "mode"),
                    textAttr(
                        caseAttr.getString(R.styleable.Keyboard_Case_imeAction),
                        "imeAction"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_navigateNext,
                        "navigateNext"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_navigatePrevious,
                        "navigatePrevious"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_clobberSettingsKey,
                        "clobberSettingsKey"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_passwordInput,
                        "passwordInput"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_hasShortcutKey,
                        "hasShortcutKey"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_languageSwitchKeyEnabled,
                        "languageSwitchKeyEnabled"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_isMultiLine,
                        "isMultiLine"
                    ),
                    booleanAttr(
                        caseAttr, R.styleable.Keyboard_Case_isSplitLayout,
                        "splitLayout"
                    ),
                    textAttr(
                        caseAttr.getString(R.styleable.Keyboard_Case_isIconDefined),
                        "isIconDefined"
                    ),
                    textAttr(
                        caseAttr.getString(R.styleable.Keyboard_Case_localeCode),
                        "localeCode"
                    ),
                    textAttr(
                        caseAttr.getString(R.styleable.Keyboard_Case_languageCode),
                        "languageCode"
                    ),
                    textAttr(
                        caseAttr.getString(R.styleable.Keyboard_Case_countryCode),
                        "countryCode"
                    ),
                    if (selected) "" else " skipped"
                )
            }

            return selected
        } finally {
            caseAttr.recycle()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDefault(
        parser: XmlPullParser, row: KeyboardRow?,
        skip: Boolean
    ): Boolean {
        if (DEBUG) startTag("<%s>", TAG_DEFAULT)
        if (row == null) {
            parseKeyboardContent(parser, skip)
        } else {
            parseRowContent(parser, row, skip)
        }
        return true
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseKeyStyle(parser: XmlPullParser, skip: Boolean) {
        val attr: AttributeSet = Xml.asAttributeSet(parser)
        val keyStyleAttr: TypedArray = mResources.obtainAttributes(
            attr, R.styleable.Keyboard_KeyStyle
        )
        val keyAttrs: TypedArray = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key)
        try {
            if (!keyStyleAttr.hasValue(R.styleable.Keyboard_KeyStyle_styleName)) {
                throw XmlParseUtils.ParseException(
                    ("<" + TAG_KEY_STYLE
                            + "/> needs styleName attribute"), parser
                )
            }
            if (DEBUG) {
                startEndTag(
                    "<%s styleName=%s />%s", TAG_KEY_STYLE,
                    keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_styleName)!!,
                    if (skip) " skipped" else ""
                )
            }
            if (!skip) {
                mParams.mKeyStyles.parseKeyStyleAttributes(keyStyleAttr, keyAttrs, parser)
            }
        } finally {
            keyStyleAttr.recycle()
            keyAttrs.recycle()
        }
        XmlParseUtils.checkEndTag(TAG_KEY_STYLE, parser)
    }

    private fun startKeyboard() {
        mCurrentY += mParams.mTopPadding
        mTopEdge = true
    }

    private fun startRow(row: KeyboardRow) {
        addEdgeSpace(mParams.mLeftPadding.toFloat(), row)
        mCurrentRow = row
        mLeftEdge = true
        mRightEdgeKey = null
    }

    private fun endRow(row: KeyboardRow) {
        if (mCurrentRow == null) {
            throw RuntimeException("orphan end row tag")
        }
        if (mRightEdgeKey != null) {
            mRightEdgeKey!!.markAsRightEdge(mParams)
            mRightEdgeKey = null
        }
        addEdgeSpace(mParams.mRightPadding.toFloat(), row)
        mCurrentY += row.getRowHeight()
        mCurrentRow = null
        mTopEdge = false
    }

    private fun endKey(key: Key) {
        mParams.onAddKey(key)
        if (mLeftEdge) {
            key.markAsLeftEdge(mParams)
            mLeftEdge = false
        }
        if (mTopEdge) {
            key.markAsTopEdge(mParams)
        }
        mRightEdgeKey = key
    }

    private fun endKeyboard() {
        mParams.removeRedundantMoreKeys()
        // {@link #parseGridRows(XmlPullParser,boolean)} may populate keyboard rows higher than
        // previously expected.
        val actualHeight: Int = mCurrentY - mParams.mVerticalGap + mParams.mBottomPadding
        mParams.mOccupiedHeight =
            max(mParams.mOccupiedHeight.toDouble(), actualHeight.toDouble()).toInt()
    }

    private fun addEdgeSpace(width: Float, row: KeyboardRow) {
        row.advanceXPos(width)
        mLeftEdge = false
        mRightEdgeKey = null
    }

    companion object {
        private const val BUILDER_TAG: String = "Keyboard.Builder"
        private const val DEBUG: Boolean = false

        // Keyboard XML Tags
        private const val TAG_KEYBOARD: String = "Keyboard"
        private const val TAG_ROW: String = "Row"
        private const val TAG_GRID_ROWS: String = "GridRows"
        private const val TAG_KEY: String = "Key"
        private const val TAG_SPACER: String = "Spacer"
        private const val TAG_INCLUDE: String = "include"
        private const val TAG_MERGE: String = "merge"
        private const val TAG_SWITCH: String = "switch"
        private const val TAG_CASE: String = "case"
        private const val TAG_DEFAULT: String = "default"
        const val TAG_KEY_STYLE: String = "key-style"

        private const val DEFAULT_KEYBOARD_COLUMNS: Int = 10
        private const val DEFAULT_KEYBOARD_ROWS: Int = 4

        private const val SPACES: String = "                                             "

        private fun spaces(count: Int): String {
            return if ((count < SPACES.length)) SPACES.substring(0, count) else SPACES
        }

        private fun matchLocaleCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(caseAttr, R.styleable.Keyboard_Case_localeCode, locale.toString())
        }

        private fun matchLanguageCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(
                caseAttr,
                R.styleable.Keyboard_Case_languageCode,
                locale.getLanguage()
            )
        }

        private fun matchCountryCodes(caseAttr: TypedArray, locale: Locale): Boolean {
            return matchString(caseAttr, R.styleable.Keyboard_Case_countryCode, locale.getCountry())
        }

        private fun matchInteger(a: TypedArray, index: Int, value: Int): Boolean {
            // If <case> does not have "index" attribute, that means this <case> is wild-card for
            // the attribute.
            return !a.hasValue(index) || a.getInt(index, 0) == value
        }

        private fun matchBoolean(a: TypedArray, index: Int, value: Boolean): Boolean {
            // If <case> does not have "index" attribute, that means this <case> is wild-card for
            // the attribute.
            return !a.hasValue(index) || a.getBoolean(index, false) == value
        }

        private fun matchString(a: TypedArray, index: Int, value: String): Boolean {
            // If <case> does not have "index" attribute, that means this <case> is wild-card for
            // the attribute.
            return !a.hasValue(index)
                    || StringUtils.containsInArray(value, a.getString(
                index
            )!!
                .split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            )
        }

        private fun matchTypedValue(
            a: TypedArray, index: Int, intValue: Int,
            strValue: String
        ): Boolean {
            // If <case> does not have "index" attribute, that means this <case> is wild-card for
            // the attribute.
            val v: TypedValue? = a.peekValue(index)
            if (v == null) {
                return true
            }
            if (ResourceUtils.isIntegerValue(v)) {
                return intValue == a.getInt(index, 0)
            }
            if (ResourceUtils.isStringValue(v)) {
                return StringUtils.containsInArray(strValue, a.getString(
                    index
                )!!
                    .split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                )
            }
            return false
        }

        private fun isIconDefined(
            a: TypedArray, index: Int,
            iconsSet: KeyboardIconsSet
        ): Boolean {
            if (!a.hasValue(index)) {
                return true
            }
            val iconName: String? = a.getString(index)
            val iconId: Int = KeyboardIconsSet.getIconId(iconName)
            return iconsSet.getIconDrawable(iconId) != null
        }

        private fun textAttr(value: String?, name: String): String {
            return if (value != null) String.format(" %s=%s", name, value) else ""
        }

        private fun booleanAttr(a: TypedArray, index: Int, name: String): String {
            return if (a.hasValue(index)) String.format(
                " %s=%s",
                name,
                a.getBoolean(index, false)
            ) else
                ""
        }
    }
}
