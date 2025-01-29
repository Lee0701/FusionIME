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
package com.android.inputmethod.keyboard

import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.android.inputmethod.keyboard.internal.KeyDrawParams
import com.android.inputmethod.keyboard.internal.KeySpecParser
import com.android.inputmethod.keyboard.internal.KeyStyle
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.keyboard.internal.KeyboardRow
import com.android.inputmethod.keyboard.internal.MoreKeySpec
import com.android.inputmethod.keyboard.internal.MoreKeySpec.LettersOnBaseLayout
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import java.util.Locale

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 */
open class Key : Comparable<Key> {
    /**
     * The key code (unicode or custom code) that this key generates.
     */
    val code: Int

    /** Label to display  */
    val label: String?

    /** Hint label to display on the key in conjunction with the label  */
    val hintLabel: String?

    /** Flags of the label  */
    private val mLabelFlags: Int

    /** Icon to display instead of a label. Icon takes precedence over a label  */
    val iconId: Int

    /**
     * Gets the width of the key in pixels, excluding the gap.
     * @return The width of the key in pixels, excluding the gap.
     */
    /** Width of the key, excluding the gap  */
    val width: Int
    /**
     * Gets the height of the key in pixels, excluding the gap.
     * @return The height of the key in pixels, excluding the gap.
     */
    /** Height of the key, excluding the gap  */
    val height: Int
    /**
     * The combined width in pixels of the horizontal gaps belonging to this key, both above and
     * below. I.e., getWidth() + getHorizontalGap() = total width belonging to the key.
     * @return Horizontal gap belonging to this key.
     */
    /**
     * The combined width in pixels of the horizontal gaps belonging to this key, both to the left
     * and to the right. I.e., mWidth + mHorizontalGap = total width belonging to the key.
     */
    val horizontalGap: Int
    /**
     * The combined height in pixels of the vertical gaps belonging to this key, both above and
     * below. I.e., getHeight() + getVerticalGap() = total height belonging to the key.
     * @return Vertical gap belonging to this key.
     */
    /**
     * The combined height in pixels of the vertical gaps belonging to this key, both above and
     * below. I.e., mHeight + mVerticalGap = total height belonging to the key.
     */
    val verticalGap: Int
    /**
     * Gets the x-coordinate of the top-left corner of the key in pixels, excluding the gap.
     * @return The x-coordinate of the top-left corner of the key in pixels, excluding the gap.
     */
    /** X coordinate of the top-left corner of the key in the keyboard layout, excluding the gap.  */
    open val x: Int
    /**
     * Gets the y-coordinate of the top-left corner of the key in pixels, excluding the gap.
     * @return The y-coordinate of the top-left corner of the key in pixels, excluding the gap.
     */
    /** Y coordinate of the top-left corner of the key in the keyboard layout, excluding the gap.  */
    open val y: Int

    /** Hit bounding box of the key  */
    val hitBox: Rect = Rect()

    /** More keys. It is guaranteed that this is null or an array of one or more elements  */
    var moreKeys: Array<MoreKeySpec>

    /** More keys column number and flags  */
    private val mMoreKeysColumnAndFlags: Int

    /** Background type that represents different key background visual than normal one.  */
    private val mBackgroundType: Int
    private val mActionFlags: Int
    val visualAttributes: KeyVisualAttributes?
    private val mOptionalAttributes: OptionalAttributes?

    private class OptionalAttributes(
        outputText: String?, altCode: Int,
        disabledIconId: Int, visualInsetsLeft: Int, visualInsetsRight: Int
    ) {
        /** Text to output when pressed. This can be multiple characters, like ".com"  */
        val mOutputText: String?
        val mAltCode: Int

        /** Icon for disabled state  */
        val mDisabledIconId: Int

        /** The visual insets  */
        val mVisualInsetsLeft: Int
        val mVisualInsetsRight: Int

        init {
            mOutputText = outputText
            mAltCode = altCode
            mDisabledIconId = disabledIconId
            mVisualInsetsLeft = visualInsetsLeft
            mVisualInsetsRight = visualInsetsRight
        }

        companion object {
            fun newInstance(
                outputText: String?, altCode: Int,
                disabledIconId: Int, visualInsetsLeft: Int, visualInsetsRight: Int
            ): OptionalAttributes? {
                if (outputText == null && altCode == Constants.CODE_UNSPECIFIED && disabledIconId == KeyboardIconsSet.ICON_UNDEFINED && visualInsetsLeft == 0 && visualInsetsRight == 0) {
                    return null
                }
                return OptionalAttributes(
                    outputText, altCode, disabledIconId, visualInsetsLeft,
                    visualInsetsRight
                )
            }
        }
    }

    private val mHashCode: Int

    /** The current pressed state of this key  */
    private var mPressed: Boolean = false

    /** Key is enabled and responds on press  */
    var isEnabled: Boolean = true

    /**
     * Constructor for a key on `MoreKeyKeyboard`, on `MoreSuggestions`,
     * and in a <GridRows></GridRows>.
     */
    constructor(
        label: String?, iconId: Int, code: Int,
        outputText: String?, hintLabel: String?,
        labelFlags: Int, backgroundType: Int, x: Int, y: Int,
        width: Int, height: Int, horizontalGap: Int, verticalGap: Int
    ) {
        this.width = width - horizontalGap
        this.height = height - verticalGap
        this.horizontalGap = horizontalGap
        this.verticalGap = verticalGap
        this.hintLabel = hintLabel
        mLabelFlags = labelFlags
        mBackgroundType = backgroundType
        // TODO: Pass keyActionFlags as an argument.
        mActionFlags = ACTION_FLAGS_NO_KEY_PREVIEW
        moreKeys = emptyArray()
        mMoreKeysColumnAndFlags = 0
        this.label = label
        mOptionalAttributes = OptionalAttributes.newInstance(
            outputText,
            Constants.CODE_UNSPECIFIED,
            KeyboardIconsSet.ICON_UNDEFINED,
            0,  /* visualInsetsLeft */
            0 /* visualInsetsRight */
        )
        this.code = code
        isEnabled = (code != Constants.CODE_UNSPECIFIED)
        this.iconId = iconId
        // Horizontal gap is divided equally to both sides of the key.
        this.x = x + this.horizontalGap / 2
        this.y = y
        hitBox.set(x, y, x + width + 1, y + height)
        visualAttributes = null

        mHashCode = computeHashCode(this)
    }

    /**
     * Create a key with the given top-left coordinate and extract its attributes from a key
     * specification string, Key attribute array, key style, and etc.
     *
     * @param keySpec the key specification.
     * @param keyAttr the Key XML attributes array.
     * @param style the [KeyStyle] of this key.
     * @param params the keyboard building parameters.
     * @param row the row that this key belongs to. row's x-coordinate will be the right edge of
     * this key.
     */
    constructor(
        keySpec: String?, keyAttr: TypedArray,
        style: KeyStyle, params: KeyboardParams,
        row: KeyboardRow
    ) {
        horizontalGap = if (isSpacer) 0 else params.mHorizontalGap
        verticalGap = params.mVerticalGap

        val horizontalGapFloat: Float = horizontalGap.toFloat()
        val rowHeight: Int = row.getRowHeight()
        height = rowHeight - verticalGap

        val keyXPos: Float = row.getKeyX(keyAttr)
        val keyWidth: Float = row.getKeyWidth(keyAttr, keyXPos)
        val keyYPos: Int = row.getKeyY()

        // Horizontal gap is divided equally to both sides of the key.
        x = Math.round(keyXPos + horizontalGapFloat / 2)
        y = keyYPos
        width = Math.round(keyWidth - horizontalGapFloat)
        hitBox.set(
            Math.round(keyXPos), keyYPos, Math.round(keyXPos + keyWidth) + 1,
            keyYPos + rowHeight
        )
        // Update row to have current x coordinate.
        row.setXPos(keyXPos + keyWidth)

        mBackgroundType = style.getInt(
            keyAttr,
            R.styleable.Keyboard_Key_backgroundType, row.getDefaultBackgroundType()
        )

        val baseWidth: Int = params.mBaseWidth
        val visualInsetsLeft: Int = Math.round(
            keyAttr.getFraction(
                R.styleable.Keyboard_Key_visualInsetsLeft, baseWidth, baseWidth, 0f
            )
        )
        val visualInsetsRight: Int = Math.round(
            keyAttr.getFraction(
                R.styleable.Keyboard_Key_visualInsetsRight, baseWidth, baseWidth, 0f
            )
        )

        mLabelFlags = (style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags)
                or row.getDefaultKeyLabelFlags())
        val needsToUpcase = needsToUpcase(mLabelFlags, params.mId?.mElementId)
        val localeForUpcasing: Locale = params.mId!!.getLocale()
        var actionFlags: Int = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyActionFlags)
        var moreKeys: Array<String?>? =
            style.getStringArray(keyAttr, R.styleable.Keyboard_Key_moreKeys)

        // Get maximum column order number and set a relevant mode value.
        var moreKeysColumnAndFlags: Int = (MORE_KEYS_MODE_MAX_COLUMN_WITH_AUTO_ORDER
                or style.getInt(
            keyAttr, R.styleable.Keyboard_Key_maxMoreKeysColumn,
            params.mMaxMoreKeysKeyboardColumn
        ))
        var value: Int
        if ((MoreKeySpec.getIntValue(moreKeys, MORE_KEYS_AUTO_COLUMN_ORDER, -1)
                .also { value = it }) > 0
        ) {
            // Override with fixed column order number and set a relevant mode value.
            moreKeysColumnAndFlags = (MORE_KEYS_MODE_FIXED_COLUMN_WITH_AUTO_ORDER
                    or (value and MORE_KEYS_COLUMN_NUMBER_MASK))
        }
        if ((MoreKeySpec.getIntValue(moreKeys, MORE_KEYS_FIXED_COLUMN_ORDER, -1)
                .also { value = it }) > 0
        ) {
            // Override with fixed column order number and set a relevant mode value.
            moreKeysColumnAndFlags = (MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER
                    or (value and MORE_KEYS_COLUMN_NUMBER_MASK))
        }
        if (MoreKeySpec.getBooleanValue(moreKeys, MORE_KEYS_HAS_LABELS)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_HAS_LABELS
        }
        if (MoreKeySpec.getBooleanValue(moreKeys, MORE_KEYS_NEEDS_DIVIDERS)) {
            moreKeysColumnAndFlags = moreKeysColumnAndFlags or MORE_KEYS_FLAGS_NEEDS_DIVIDERS
        }
        if (MoreKeySpec.getBooleanValue(moreKeys, MORE_KEYS_NO_PANEL_AUTO_MORE_KEY)) {
            moreKeysColumnAndFlags =
                moreKeysColumnAndFlags or MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY
        }
        mMoreKeysColumnAndFlags = moreKeysColumnAndFlags

        val additionalMoreKeys: Array<String?>?
        if ((mLabelFlags and LABEL_FLAGS_DISABLE_ADDITIONAL_MORE_KEYS) != 0) {
            additionalMoreKeys = null
        } else {
            additionalMoreKeys = style.getStringArray(
                keyAttr,
                R.styleable.Keyboard_Key_additionalMoreKeys
            )
        }
        moreKeys = MoreKeySpec.insertAdditionalMoreKeys(moreKeys, additionalMoreKeys)
        if (moreKeys != null) {
            actionFlags = actionFlags or ACTION_FLAGS_ENABLE_LONG_PRESS
            this.moreKeys = moreKeys.map { MoreKeySpec(it!!, needsToUpcase, localeForUpcasing) }.toTypedArray()
        } else {
            this.moreKeys = emptyArray()
        }
        mActionFlags = actionFlags

        iconId = KeySpecParser.getIconId(keySpec)
        val disabledIconId: Int = KeySpecParser.getIconId(
            style.getString(
                keyAttr,
                R.styleable.Keyboard_Key_keyIconDisabled
            )
        )

        val code: Int = KeySpecParser.getCode(keySpec)
        if ((mLabelFlags and LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL) != 0) {
            label = params.mId?.mCustomActionLabel
        } else if (code >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // This is a workaround to have a key that has a supplementary code point in its label.
            // Because we can put a string in resource neither as a XML entity of a supplementary
            // code point nor as a surrogate pair.
            label = StringBuilder().appendCodePoint(code).toString()
        } else {
            val label: String? = KeySpecParser.getLabel(keySpec)
            this.label = if (needsToUpcase)
                StringUtils.toTitleCaseOfKeyLabel(label, localeForUpcasing)
            else
                label
        }
        if ((mLabelFlags and LABEL_FLAGS_DISABLE_HINT_LABEL) != 0) {
            hintLabel = null
        } else {
            val hintLabel: String? = style.getString(
                keyAttr, R.styleable.Keyboard_Key_keyHintLabel
            )
            this.hintLabel = if (needsToUpcase)
                StringUtils.toTitleCaseOfKeyLabel(hintLabel, localeForUpcasing)
            else
                hintLabel
        }
        var outputText: String? = KeySpecParser.getOutputText(keySpec)
        if (needsToUpcase) {
            outputText = StringUtils.toTitleCaseOfKeyLabel(outputText, localeForUpcasing)
        }
        // Choose the first letter of the label as primary code if not specified.
        if (code == Constants.CODE_UNSPECIFIED && TextUtils.isEmpty(outputText)
            && !TextUtils.isEmpty(label)
        ) {
            if (StringUtils.codePointCount(label) == 1) {
                // Use the first letter of the hint label if shiftedLetterActivated flag is
                // specified.
                if (hasShiftedLetterHint() && isShiftedLetterActivated) {
                    this.code = hintLabel!!.codePointAt(0)
                } else {
                    this.code = label!!.codePointAt(0)
                }
            } else {
                // In some locale and case, the character might be represented by multiple code
                // points, such as upper case Eszett of German alphabet.
                outputText = label
                this.code = Constants.CODE_OUTPUT_TEXT
            }
        } else if (code == Constants.CODE_UNSPECIFIED && outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) {
                this.code = outputText.codePointAt(0)
                outputText = null
            } else {
                this.code = Constants.CODE_OUTPUT_TEXT
            }
        } else {
            this.code = if (needsToUpcase)
                StringUtils.toTitleCaseOfKeyCode(code, localeForUpcasing)
            else
                code
        }
        val altCodeInAttr: Int = KeySpecParser.parseCode(
            style.getString(keyAttr, R.styleable.Keyboard_Key_altCode), Constants.CODE_UNSPECIFIED
        )
        val altCode: Int = if (needsToUpcase)
            StringUtils.toTitleCaseOfKeyCode(altCodeInAttr, localeForUpcasing)
        else
            altCodeInAttr
        mOptionalAttributes = OptionalAttributes.newInstance(
            outputText, altCode,
            disabledIconId, visualInsetsLeft, visualInsetsRight
        )
        visualAttributes = KeyVisualAttributes.newInstance(keyAttr)
        mHashCode = computeHashCode(this)
    }

    /**
     * Copy constructor for DynamicGridKeyboard.GridKey.
     *
     * @param key the original key.
     */
    protected constructor(key: Key) : this(key, key.moreKeys)

    private constructor(key: Key, moreKeys: Array<MoreKeySpec>) {
        // Final attributes.
        code = key.code
        label = key.label
        hintLabel = key.hintLabel
        mLabelFlags = key.mLabelFlags
        iconId = key.iconId
        width = key.width
        height = key.height
        horizontalGap = key.horizontalGap
        verticalGap = key.verticalGap
        x = key.x
        y = key.y
        hitBox.set(key.hitBox)
        this.moreKeys = moreKeys
        mMoreKeysColumnAndFlags = key.mMoreKeysColumnAndFlags
        mBackgroundType = key.mBackgroundType
        mActionFlags = key.mActionFlags
        visualAttributes = key.visualAttributes
        mOptionalAttributes = key.mOptionalAttributes
        mHashCode = key.mHashCode
        // Key state.
        mPressed = key.mPressed
        isEnabled = key.isEnabled
    }

    private fun equalsInternal(o: Key): Boolean {
        if (this === o) return true
        return o.x == x && o.y == y && o.width == width && o.height == height && o.code == code && TextUtils.equals(
            o.label,
            label
        )
                && TextUtils.equals(o.hintLabel, hintLabel)
                && o.iconId == iconId && o.mBackgroundType == mBackgroundType && o.moreKeys.contentEquals(
            moreKeys
        ) && TextUtils.equals(o.outputText, outputText)
                && o.mActionFlags == mActionFlags && o.mLabelFlags == mLabelFlags
    }

    override fun compareTo(other: Key): Int {
        if (equalsInternal(other)) return 0
        if (mHashCode > other.mHashCode) return 1
        return -1
    }

    override fun hashCode(): Int {
        return mHashCode
    }

    override fun equals(other: Any?): Boolean {
        return other is Key && equalsInternal(other)
    }

    override fun toString(): String {
        return toShortString() + " " + x + "," + y + " " + width + "x" + height
    }

    fun toShortString(): String? {
        val code: Int = code
        if (code == Constants.CODE_OUTPUT_TEXT) {
            return outputText
        }
        return Constants.printableCode(code)
    }

    fun toLongString(): String {
        val iconId: Int = iconId
        val topVisual: String? = if ((iconId == KeyboardIconsSet.ICON_UNDEFINED))
            KeyboardIconsSet.PREFIX_ICON + KeyboardIconsSet.getIconName(iconId)
        else
            label
        val hintLabel: String? = hintLabel
        val visual: String? = if ((hintLabel == null)) topVisual else topVisual + "^" + hintLabel
        return toString() + " " + visual + "/" + backgroundName(mBackgroundType)
    }

    fun markAsLeftEdge(params: KeyboardParams) {
        hitBox.left = params.mLeftPadding
    }

    fun markAsRightEdge(params: KeyboardParams) {
        hitBox.right = params.mOccupiedWidth - params.mRightPadding
    }

    fun markAsTopEdge(params: KeyboardParams) {
        hitBox.top = params.mTopPadding
    }

    fun markAsBottomEdge(params: KeyboardParams) {
        hitBox.bottom = params.mOccupiedHeight + params.mBottomPadding
    }

    val isSpacer: Boolean
        get() {
            return this is Spacer
        }

    val isActionKey: Boolean
        get() {
            return mBackgroundType == BACKGROUND_TYPE_ACTION
        }

    val isShift: Boolean
        get() {
            return code == Constants.CODE_SHIFT
        }

    val isModifier: Boolean
        get() {
            return code == Constants.CODE_SHIFT || code == Constants.CODE_SWITCH_ALPHA_SYMBOL
        }

    val isRepeatable: Boolean
        get() {
            return (mActionFlags and ACTION_FLAGS_IS_REPEATABLE) != 0
        }

    fun noKeyPreview(): Boolean {
        return (mActionFlags and ACTION_FLAGS_NO_KEY_PREVIEW) != 0
    }

    fun altCodeWhileTyping(): Boolean {
        return (mActionFlags and ACTION_FLAGS_ALT_CODE_WHILE_TYPING) != 0
    }

    val isLongPressEnabled: Boolean
        get() {
            // We need not start long press timer on the key which has activated shifted letter.
            return (mActionFlags and ACTION_FLAGS_ENABLE_LONG_PRESS) != 0
                    && (mLabelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED) == 0
        }

    fun selectTypeface(params: KeyDrawParams): Typeface {
        when (mLabelFlags and LABEL_FLAGS_FONT_MASK) {
            LABEL_FLAGS_FONT_NORMAL -> return Typeface.DEFAULT
            LABEL_FLAGS_FONT_MONO_SPACE -> return Typeface.MONOSPACE
            LABEL_FLAGS_FONT_DEFAULT ->             // The type-face is specified by keyTypeface attribute.
                return params.mTypeface

            else ->
                return params.mTypeface
        }
    }

    fun selectTextSize(params: KeyDrawParams): Int {
        when (mLabelFlags and LABEL_FLAGS_FOLLOW_KEY_TEXT_RATIO_MASK) {
            LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO -> return params.mLetterSize
            LABEL_FLAGS_FOLLOW_KEY_LARGE_LETTER_RATIO -> return params.mLargeLetterSize
            LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO -> return params.mLabelSize
            LABEL_FLAGS_FOLLOW_KEY_HINT_LABEL_RATIO -> return params.mHintLabelSize
            else -> return if (StringUtils.codePointCount(label) == 1) params.mLetterSize else params.mLabelSize
        }
    }

    fun selectTextColor(params: KeyDrawParams): Int {
        if ((mLabelFlags and LABEL_FLAGS_FOLLOW_FUNCTIONAL_TEXT_COLOR) != 0) {
            return params.mFunctionalTextColor
        }
        return if (isShiftedLetterActivated) params.mTextInactivatedColor else params.mTextColor
    }

    fun selectHintTextSize(params: KeyDrawParams): Int {
        if (hasHintLabel()) {
            return params.mHintLabelSize
        }
        if (hasShiftedLetterHint()) {
            return params.mShiftedLetterHintSize
        }
        return params.mHintLetterSize
    }

    fun selectHintTextColor(params: KeyDrawParams): Int {
        if (hasHintLabel()) {
            return params.mHintLabelColor
        }
        if (hasShiftedLetterHint()) {
            return if (isShiftedLetterActivated)
                params.mShiftedLetterHintActivatedColor
            else
                params.mShiftedLetterHintInactivatedColor
        }
        return params.mHintLetterColor
    }

    fun selectMoreKeyTextSize(params: KeyDrawParams): Int {
        return if (hasLabelsInMoreKeys()) params.mLabelSize else params.mLetterSize
    }

    val previewLabel: String?
        get() {
            return if (isShiftedLetterActivated) hintLabel else label
        }

    private fun previewHasLetterSize(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO) != 0
                || StringUtils.codePointCount(previewLabel) == 1
    }

    fun selectPreviewTextSize(params: KeyDrawParams): Int {
        if (previewHasLetterSize()) {
            return params.mPreviewTextSize
        }
        return params.mLetterSize
    }

    fun selectPreviewTypeface(params: KeyDrawParams): Typeface {
        if (previewHasLetterSize()) {
            return selectTypeface(params)
        }
        return Typeface.DEFAULT_BOLD
    }

    fun isAlignHintLabelToBottom(defaultFlags: Int): Boolean {
        return ((mLabelFlags or defaultFlags) and LABEL_FLAGS_ALIGN_HINT_LABEL_TO_BOTTOM) != 0
    }

    val isAlignIconToBottom: Boolean
        get() {
            return (mLabelFlags and LABEL_FLAGS_ALIGN_ICON_TO_BOTTOM) != 0
        }

    val isAlignLabelOffCenter: Boolean
        get() {
            return (mLabelFlags and LABEL_FLAGS_ALIGN_LABEL_OFF_CENTER) != 0
        }

    fun hasPopupHint(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_HAS_POPUP_HINT) != 0
    }

    fun hasShiftedLetterHint(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_HAS_SHIFTED_LETTER_HINT) != 0
                && !TextUtils.isEmpty(hintLabel)
    }

    fun hasHintLabel(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_HAS_HINT_LABEL) != 0
    }

    fun needsAutoXScale(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_AUTO_X_SCALE) != 0
    }

    fun needsAutoScale(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_AUTO_SCALE) == LABEL_FLAGS_AUTO_SCALE
    }

    fun needsToKeepBackgroundAspectRatio(defaultFlags: Int): Boolean {
        return ((mLabelFlags or defaultFlags) and LABEL_FLAGS_KEEP_BACKGROUND_ASPECT_RATIO) != 0
    }

    fun hasCustomActionLabel(): Boolean {
        return (mLabelFlags and LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL) != 0
    }

    private val isShiftedLetterActivated: Boolean
        get() {
            return (mLabelFlags and LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED) != 0
                    && !TextUtils.isEmpty(hintLabel)
        }

    val moreKeysColumnNumber: Int
        get() {
            return mMoreKeysColumnAndFlags and MORE_KEYS_COLUMN_NUMBER_MASK
        }

    val isMoreKeysFixedColumn: Boolean
        get() {
            return (mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_COLUMN) != 0
        }

    val isMoreKeysFixedOrder: Boolean
        get() {
            return (mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_FIXED_ORDER) != 0
        }

    fun hasLabelsInMoreKeys(): Boolean {
        return (mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_HAS_LABELS) != 0
    }

    val moreKeyLabelFlags: Int
        get() {
            val labelSizeFlag: Int = if (hasLabelsInMoreKeys())
                LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO
            else
                LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO
            return labelSizeFlag or LABEL_FLAGS_AUTO_X_SCALE
        }

    fun needsDividersInMoreKeys(): Boolean {
        return (mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_NEEDS_DIVIDERS) != 0
    }

    fun hasNoPanelAutoMoreKey(): Boolean {
        return (mMoreKeysColumnAndFlags and MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY) != 0
    }

    val outputText: String?
        get() {
            val attrs: OptionalAttributes? = mOptionalAttributes
            return if ((attrs != null)) attrs.mOutputText else null
        }

    val altCode: Int
        get() {
            val attrs: OptionalAttributes? = mOptionalAttributes
            return if ((attrs != null)) attrs.mAltCode else Constants.CODE_UNSPECIFIED
        }

    open fun getIcon(iconSet: KeyboardIconsSet, alpha: Int): Drawable? {
        val attrs: OptionalAttributes? = mOptionalAttributes
        val disabledIconId: Int =
            if ((attrs != null)) attrs.mDisabledIconId else KeyboardIconsSet.ICON_UNDEFINED
        val iconId: Int = if (isEnabled) iconId else disabledIconId
        val icon: Drawable? = iconSet.getIconDrawable(iconId)
        if (icon != null) {
            icon.setAlpha(alpha)
        }
        return icon
    }

    fun getPreviewIcon(iconSet: KeyboardIconsSet): Drawable? {
        return iconSet.getIconDrawable(iconId)
    }

    val drawX: Int
        get() {
            val x: Int = x
            val attrs: OptionalAttributes? = mOptionalAttributes
            return if ((attrs == null)) x else x + attrs.mVisualInsetsLeft
        }

    val drawWidth: Int
        get() {
            val attrs: OptionalAttributes? = mOptionalAttributes
            return if ((attrs == null))
                width
            else
                width - attrs.mVisualInsetsLeft - attrs.mVisualInsetsRight
        }

    /**
     * Informs the key that it has been pressed, in case it needs to change its appearance or
     * state.
     * @see .onReleased
     */
    fun onPressed() {
        mPressed = true
    }

    /**
     * Informs the key that it has been released, in case it needs to change its appearance or
     * state.
     * @see .onPressed
     */
    fun onReleased() {
        mPressed = false
    }

    /**
     * Detects if a point falls on this key.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return whether or not the point falls on the key. If the key is attached to an edge, it
     * will assume that all points between the key and the edge are considered to be on the key.
     * @see .markAsLeftEdge
     */
    fun isOnKey(x: Int, y: Int): Boolean {
        return hitBox.contains(x, y)
    }

    /**
     * Returns the square of the distance to the nearest edge of the key and the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the square of the distance of the point from the nearest edge of the key
     */
    fun squaredDistanceToEdge(x: Int, y: Int): Int {
        val left: Int = this.x
        val right: Int = left + width
        val top: Int = this.y
        val bottom: Int = top + height
        val edgeX: Int = if (x < left) left else (if (x > right) right else x)
        val edgeY: Int = if (y < top) top else (if (y > bottom) bottom else y)
        val dx: Int = x - edgeX
        val dy: Int = y - edgeY
        return dx * dx + dy * dy
    }

    internal class KeyBackgroundState private constructor(vararg attrs: Int) {
        private val mReleasedState: IntArray
        private val mPressedState: IntArray

        fun getState(pressed: Boolean): IntArray {
            return if (pressed) mPressedState else mReleasedState
        }

        init {
            mReleasedState = attrs
            mPressedState = attrs.copyOf(attrs.size + 1)
            mPressedState[attrs.size] = android.R.attr.state_pressed
        }

        companion object {
            val STATES: Array<KeyBackgroundState> = arrayOf(
                // 0: BACKGROUND_TYPE_EMPTY
                KeyBackgroundState(android.R.attr.state_empty),  // 1: BACKGROUND_TYPE_NORMAL
                KeyBackgroundState(),  // 2: BACKGROUND_TYPE_FUNCTIONAL
                KeyBackgroundState(),  // 3: BACKGROUND_TYPE_STICKY_OFF
                KeyBackgroundState(android.R.attr.state_checkable),  // 4: BACKGROUND_TYPE_STICKY_ON
                KeyBackgroundState(
                    android.R.attr.state_checkable,
                    android.R.attr.state_checked
                ),  // 5: BACKGROUND_TYPE_ACTION
                KeyBackgroundState(android.R.attr.state_active),  // 6: BACKGROUND_TYPE_SPACEBAR
                KeyBackgroundState(),
            )
        }
    }

    /**
     * Returns the background drawable for the key, based on the current state and type of the key.
     * @return the background drawable of the key.
     * @see android.graphics.drawable.StateListDrawable.setState
     */
    fun selectBackgroundDrawable(
        keyBackground: Drawable,
        functionalKeyBackground: Drawable,
        spacebarBackground: Drawable
    ): Drawable {
        val background: Drawable
        if (mBackgroundType == BACKGROUND_TYPE_FUNCTIONAL) {
            background = functionalKeyBackground
        } else if (mBackgroundType == BACKGROUND_TYPE_SPACEBAR) {
            background = spacebarBackground
        } else {
            background = keyBackground
        }
        val state: IntArray = KeyBackgroundState.STATES.get(mBackgroundType).getState(mPressed)
        background.setState(state)
        return background
    }

    open class Spacer : Key {
        constructor(
            keyAttr: TypedArray, keyStyle: KeyStyle,
            params: KeyboardParams, row: KeyboardRow
        ) : super(null,  /* keySpec */keyAttr, keyStyle, params, row)

        /**
         * This constructor is being used only for divider in more keys keyboard.
         */
        protected constructor(
            params: KeyboardParams, x: Int, y: Int, width: Int,
            height: Int
        ) : super(
            null,  /* label */
            KeyboardIconsSet.ICON_UNDEFINED,
            Constants.CODE_UNSPECIFIED,
            null,  /* outputText */
            null,  /* hintLabel */
            0,  /* labelFlags */
            BACKGROUND_TYPE_EMPTY,
            x,
            y,
            width,
            height,
            params.mHorizontalGap,
            params.mVerticalGap
        )
    }

    companion object {
        private const val LABEL_FLAGS_ALIGN_HINT_LABEL_TO_BOTTOM: Int = 0x02
        private const val LABEL_FLAGS_ALIGN_ICON_TO_BOTTOM: Int = 0x04
        private const val LABEL_FLAGS_ALIGN_LABEL_OFF_CENTER: Int = 0x08

        // Font typeface specification.
        private const val LABEL_FLAGS_FONT_MASK: Int = 0x30
        private const val LABEL_FLAGS_FONT_NORMAL: Int = 0x10
        private const val LABEL_FLAGS_FONT_MONO_SPACE: Int = 0x20
        private const val LABEL_FLAGS_FONT_DEFAULT: Int = 0x30

        // Start of key text ratio enum values
        private const val LABEL_FLAGS_FOLLOW_KEY_TEXT_RATIO_MASK: Int = 0x1C0
        private const val LABEL_FLAGS_FOLLOW_KEY_LARGE_LETTER_RATIO: Int = 0x40
        private const val LABEL_FLAGS_FOLLOW_KEY_LETTER_RATIO: Int = 0x80
        private const val LABEL_FLAGS_FOLLOW_KEY_LABEL_RATIO: Int = 0xC0
        private const val LABEL_FLAGS_FOLLOW_KEY_HINT_LABEL_RATIO: Int = 0x140

        // End of key text ratio mask enum values
        private const val LABEL_FLAGS_HAS_POPUP_HINT: Int = 0x200
        private const val LABEL_FLAGS_HAS_SHIFTED_LETTER_HINT: Int = 0x400
        private const val LABEL_FLAGS_HAS_HINT_LABEL: Int = 0x800

        // The bit to calculate the ratio of key label width against key width. If autoXScale bit is on
        // and autoYScale bit is off, the key label may be shrunk only for X-direction.
        // If both autoXScale and autoYScale bits are on, the key label text size may be auto scaled.
        private const val LABEL_FLAGS_AUTO_X_SCALE: Int = 0x4000
        private const val LABEL_FLAGS_AUTO_Y_SCALE: Int = 0x8000
        private val LABEL_FLAGS_AUTO_SCALE: Int = (LABEL_FLAGS_AUTO_X_SCALE
                or LABEL_FLAGS_AUTO_Y_SCALE)
        private const val LABEL_FLAGS_PRESERVE_CASE: Int = 0x10000
        private const val LABEL_FLAGS_SHIFTED_LETTER_ACTIVATED: Int = 0x20000
        private const val LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL: Int = 0x40000
        private const val LABEL_FLAGS_FOLLOW_FUNCTIONAL_TEXT_COLOR: Int = 0x80000
        private const val LABEL_FLAGS_KEEP_BACKGROUND_ASPECT_RATIO: Int = 0x100000
        private const val LABEL_FLAGS_DISABLE_HINT_LABEL: Int = 0x40000000
        private const val LABEL_FLAGS_DISABLE_ADDITIONAL_MORE_KEYS: Int = -0x80000000

        private const val MORE_KEYS_COLUMN_NUMBER_MASK: Int = 0x000000ff

        // If this flag is specified, more keys keyboard should have the specified number of columns.
        // Otherwise more keys keyboard should have less than or equal to the specified maximum number
        // of columns.
        private const val MORE_KEYS_FLAGS_FIXED_COLUMN: Int = 0x00000100

        // If this flag is specified, the order of more keys is determined by the order in the more
        // keys' specification. Otherwise the order of more keys is automatically determined.
        private const val MORE_KEYS_FLAGS_FIXED_ORDER: Int = 0x00000200
        private const val MORE_KEYS_MODE_MAX_COLUMN_WITH_AUTO_ORDER: Int = 0
        private val MORE_KEYS_MODE_FIXED_COLUMN_WITH_AUTO_ORDER: Int = MORE_KEYS_FLAGS_FIXED_COLUMN
        private val MORE_KEYS_MODE_FIXED_COLUMN_WITH_FIXED_ORDER: Int =
            (MORE_KEYS_FLAGS_FIXED_COLUMN or MORE_KEYS_FLAGS_FIXED_ORDER)
        private const val MORE_KEYS_FLAGS_HAS_LABELS: Int = 0x40000000
        private const val MORE_KEYS_FLAGS_NEEDS_DIVIDERS: Int = 0x20000000
        private const val MORE_KEYS_FLAGS_NO_PANEL_AUTO_MORE_KEY: Int = 0x10000000

        // TODO: Rename these specifiers to !autoOrder! and !fixedOrder! respectively.
        private const val MORE_KEYS_AUTO_COLUMN_ORDER: String = "!autoColumnOrder!"
        private const val MORE_KEYS_FIXED_COLUMN_ORDER: String = "!fixedColumnOrder!"
        private const val MORE_KEYS_HAS_LABELS: String = "!hasLabels!"
        private const val MORE_KEYS_NEEDS_DIVIDERS: String = "!needsDividers!"
        private const val MORE_KEYS_NO_PANEL_AUTO_MORE_KEY: String = "!noPanelAutoMoreKey!"

        const val BACKGROUND_TYPE_EMPTY: Int = 0
        const val BACKGROUND_TYPE_NORMAL: Int = 1
        const val BACKGROUND_TYPE_FUNCTIONAL: Int = 2
        const val BACKGROUND_TYPE_STICKY_OFF: Int = 3
        const val BACKGROUND_TYPE_STICKY_ON: Int = 4
        const val BACKGROUND_TYPE_ACTION: Int = 5
        const val BACKGROUND_TYPE_SPACEBAR: Int = 6

        private const val ACTION_FLAGS_IS_REPEATABLE: Int = 0x01
        private const val ACTION_FLAGS_NO_KEY_PREVIEW: Int = 0x02
        private const val ACTION_FLAGS_ALT_CODE_WHILE_TYPING: Int = 0x04
        private const val ACTION_FLAGS_ENABLE_LONG_PRESS: Int = 0x08

        fun removeRedundantMoreKeys(
            key: Key,
            lettersOnBaseLayout: LettersOnBaseLayout
        ): Key {
            val moreKeys: Array<MoreKeySpec> = key.moreKeys
            val filteredMoreKeys: Array<MoreKeySpec> =
                MoreKeySpec.removeRedundantMoreKeys(
                    moreKeys, lettersOnBaseLayout
                ) ?: emptyArray()
            return if (filteredMoreKeys.contentEquals(moreKeys)) key else Key(key, filteredMoreKeys)
        }

        private fun needsToUpcase(labelFlags: Int, keyboardElementId: Int?): Boolean {
            if ((labelFlags and LABEL_FLAGS_PRESERVE_CASE) != 0) return false
            return when (keyboardElementId) {
                KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED, KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED, KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED, KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED -> true
                else -> false
            }
        }

        private fun computeHashCode(key: Key): Int {
            return arrayOf<Any?>(
                key.x,
                key.y,
                key.width,
                key.height,
                key.code,
                key.label,
                key.hintLabel,
                key.iconId,
                key.mBackgroundType,
                key.moreKeys.contentHashCode(),
                key.outputText,
                key.mActionFlags,
                key.mLabelFlags,  // Key can be distinguishable without the following members.
                // key.mOptionalAttributes.mAltCode,
                // key.mOptionalAttributes.mDisabledIconId,
                // key.mOptionalAttributes.mPreviewIconId,
                // key.mHorizontalGap,
                // key.mVerticalGap,
                // key.mOptionalAttributes.mVisualInsetLeft,
                // key.mOptionalAttributes.mVisualInsetRight,
                // key.mMaxMoreKeysColumn,
            ).contentHashCode()
        }

        private fun backgroundName(backgroundType: Int): String? {
            when (backgroundType) {
                BACKGROUND_TYPE_EMPTY -> return "empty"
                BACKGROUND_TYPE_NORMAL -> return "normal"
                BACKGROUND_TYPE_FUNCTIONAL -> return "functional"
                BACKGROUND_TYPE_STICKY_OFF -> return "stickyOff"
                BACKGROUND_TYPE_STICKY_ON -> return "stickyOn"
                BACKGROUND_TYPE_ACTION -> return "action"
                BACKGROUND_TYPE_SPACEBAR -> return "spacebar"
                else -> return null
            }
        }
    }
}
