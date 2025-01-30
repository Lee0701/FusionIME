/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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
/*
 * This file is porting from Android framework.
 *   frameworks/base/core/java/android/inputmethodservice/Keyboard.java
 *
 * package android.inputmethodservice;
 */
package jp.co.omronsoft.openwnn

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import ee.oyatl.ime.fusion.R
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.StringTokenizer

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 *
 * The layout file for a keyboard contains XML that looks like the following snippet:
 * <pre>
 * &lt;Keyboard
 * android:keyWidth="%10p"
 * android:keyHeight="50px"
 * android:horizontalGap="2px"
 * android:verticalGap="2px" &gt;
 * &lt;Row android:keyWidth="32px" &gt;
 * &lt;Key android:keyLabel="A" /&gt;
 * ...
 * &lt;/Row&gt;
 * ...
 * &lt;/Keyboard&gt;
</pre> *
 */
class Keyboard @JvmOverloads constructor(context: Context, xmlLayoutResId: Int, modeId: Int = 0) {
    /** Keyboard label  */
    private val mLabel: CharSequence? = null

    /** Horizontal gap default for all rows  */
    protected var horizontalGap: Int = 0

    /** Default key width  */
    protected var keyWidth: Int = 0

    /** Default key height  */
    protected var keyHeight: Int = 0

    /** Default gap between rows  */
    protected var verticalGap: Int = 0

    /**
     * Returns whether keyboard is shift state or not.
     *
     * @return  `true` if keyboard is shift state; otherwise, `false`.
     */
    /** Is the keyboard in the shifted state  */
    var isShifted: Boolean = false
        private set

    /** Key instance for the shift key, if present  */
    private var mShiftKey: Key? = null

    /**
     * Returns the shift key index.
     *
     * @return  the shift key index.
     */
    /** Key index for the shift key, if present  */
    var shiftKeyIndex: Int = -1
        private set

    /** Current key width, while loading the keyboard  */
    private val mKeyWidth = 0

    /** Current key height, while loading the keyboard  */
    private val mKeyHeight = 0

    /**
     * Returns the total height of the keyboard
     * @return the total height of the keyboard
     */
    /** Total height of the keyboard, including the padding and keys  */
    var height: Int = 0
        private set

    /**
     * Returns the total minimum width of the keyboard
     * @return the total minimum width of the keyboard
     */
    /**
     * Total width of the keyboard, including left side gaps and keys, but not any gaps on the
     * right side.
     */
    var minWidth: Int = 0
        private set

    /** List of keys in this keyboard  */
    private var mKeys: MutableList<Key>? = null

    /** List of modifier keys such as Shift & Alt, if any  */
    private var mModifierKeys: MutableList<Key>? = null

    /** Width of the screen available to fit the keyboard  */
    private var mDisplayWidth = 0

    /** Height of the screen  */
    private var mDisplayHeight = 0

    /** Keyboard mode, or zero, if none.   */
    private var mKeyboardMode = 0


    private var mCellWidth = 0
    private var mCellHeight = 0
    private var mGridNeighbors: Array<IntArray?>?
    private var mProximityThreshold = 0

    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
     * Some of the key size defaults can be overridden per row from what the [Keyboard]
     * defines.
     */
    class Row {
        /** Default width of a key in this row.  */
        var defaultWidth: Int = 0

        /** Default height of a key in this row.  */
        var defaultHeight: Int = 0

        /** Default horizontal gap between keys in this row.  */
        var defaultHorizontalGap: Int = 0

        /** Vertical gap following this row.  */
        var verticalGap: Int = 0

        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * [EDGE_TOP][Keyboard.EDGE_TOP] and [EDGE_BOTTOM][Keyboard.EDGE_BOTTOM]
         */
        var rowEdgeFlags: Int = 0

        /** The keyboard mode for this row  */
        var mode: Int = 0

        var parent: Keyboard

        /** Constructor  */
        constructor(parent: Keyboard) {
            this.parent = parent
        }

        /** Constructor  */
        constructor(res: Resources, parent: Keyboard, parser: XmlResourceParser?) {
            this.parent = parent
            var a = res.obtainAttributes(
                Xml.asAttributeSet(parser),
                R.styleable.AospKeyboard
            )
            defaultWidth = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_keyWidth,
                parent.mDisplayWidth, parent.keyWidth
            )
            defaultHeight = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_keyHeight,
                parent.mDisplayHeight, parent.keyHeight
            )
            defaultHorizontalGap = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_horizontalGap,
                parent.mDisplayWidth, parent.horizontalGap
            )
            verticalGap = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_verticalGap,
                parent.mDisplayHeight, parent.verticalGap
            )
            a.recycle()
            a = res.obtainAttributes(
                Xml.asAttributeSet(parser),
                R.styleable.AospKeyboard_Row
            )
            rowEdgeFlags = a.getInt(R.styleable.AospKeyboard_Row_rowEdgeFlags, 0)
            mode = a.getResourceId(
                R.styleable.AospKeyboard_Row_keyboardMode,
                0
            )
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     */
    class Key(parent: Row) {
        /**
         * All the key codes (unicode or custom code) that this key could generate, zero'th
         * being the most important.
         */
        var codes: IntArray?

        /** Label to display  */
        var label: CharSequence? = null

        /** Icon to display instead of a label. Icon takes precedence over a label  */
        var icon: Drawable? = null

        /** Preview version of the icon, for the preview popup  */
        var iconPreview: Drawable? = null

        /** Width of the key, not including the gap  */
        var width: Int

        /** Height of the key, not including the gap  */
        var height: Int

        /** The horizontal gap before this key  */
        var gap: Int

        /** Whether this key is sticky, i.e., a toggle key  */
        var sticky: Boolean = false

        /** X coordinate of the key in the keyboard layout  */
        var x: Int = 0

        /** Y coordinate of the key in the keyboard layout  */
        var y: Int = 0

        /** The current pressed state of this key  */
        var pressed: Boolean = false

        /** If this is a sticky key, is it on?  */
        var on: Boolean = false

        /** Text to output when pressed. This can be multiple characters, like ".com"  */
        var text: CharSequence? = null

        /** Popup characters  */
        var popupCharacters: CharSequence? = null

        /**
         * Flags that specify the anchoring to edges of the keyboard for detecting touch events
         * that are just out of the boundary of the key. This is a bit mask of
         * [Keyboard.EDGE_LEFT], [Keyboard.EDGE_RIGHT], [Keyboard.EDGE_TOP] and
         * [Keyboard.EDGE_BOTTOM].
         */
        var edgeFlags: Int

        /** Whether this is a modifier key, such as Shift or Alt  */
        var modifier: Boolean = false

        /** The keyboard that this key belongs to  */
        private val keyboard = parent.parent

        /**
         * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
         * keyboard.
         */
        var popupResId: Int = 0

        /** Whether this key repeats itself when held down  */
        var repeatable: Boolean = false

        /** Whether this key is 2nd key  */
        var isSecondKey: Boolean = false

        /** Create an empty key with no attributes.  */
        init {
            height = parent.defaultHeight
            width = parent.defaultWidth
            gap = parent.defaultHorizontalGap
            edgeFlags = parent.rowEdgeFlags
        }

        /** Create a key with the given top-left coordinate and extract its attributes from
         * the XML parser.
         * @param res resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to
         * a [Keyboard].
         * @param x the x coordinate of the top-left
         * @param y the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        constructor(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?) : this(
            parent
        ) {
            this.x = x
            this.y = y

            var a = res.obtainAttributes(
                Xml.asAttributeSet(parser),
                R.styleable.AospKeyboard
            )

            width = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_keyWidth,
                keyboard.mDisplayWidth, parent.defaultWidth
            )
            height = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_keyHeight,
                keyboard.mDisplayHeight, parent.defaultHeight
            )
            gap = getDimensionOrFraction(
                a,
                R.styleable.AospKeyboard_horizontalGap,
                keyboard.mDisplayWidth, parent.defaultHorizontalGap
            )
            a.recycle()
            a = res.obtainAttributes(
                Xml.asAttributeSet(parser),
                R.styleable.AospKeyboard_Key
            )
            this.x += gap
            val codesValue = TypedValue()
            a.getValue(
                R.styleable.AospKeyboard_Key_codes,
                codesValue
            )
            if (codesValue.type == TypedValue.TYPE_INT_DEC
                || codesValue.type == TypedValue.TYPE_INT_HEX
            ) {
                codes = intArrayOf(codesValue.data)
            } else if (codesValue.type == TypedValue.TYPE_STRING) {
                codes = parseCSV(codesValue.string.toString())
            }

            iconPreview = a.getDrawable(R.styleable.AospKeyboard_Key_iconPreview)
            if (iconPreview != null) {
                iconPreview!!.setBounds(
                    0, 0, iconPreview!!.intrinsicWidth,
                    iconPreview!!.intrinsicHeight
                )
            }
            popupCharacters = a.getText(
                R.styleable.AospKeyboard_Key_popupCharacters
            )
            popupResId = a.getResourceId(
                R.styleable.AospKeyboard_Key_popupKeyboard, 0
            )
            repeatable = a.getBoolean(
                R.styleable.AospKeyboard_Key_isRepeatable, false
            )
            modifier = a.getBoolean(
                R.styleable.AospKeyboard_Key_isModifier, false
            )
            sticky = a.getBoolean(
                R.styleable.AospKeyboard_Key_isSticky, false
            )
            edgeFlags = a.getInt(R.styleable.AospKeyboard_Key_keyEdgeFlags, 0)
            edgeFlags = edgeFlags or parent.rowEdgeFlags

            icon = a.getDrawable(
                R.styleable.AospKeyboard_Key_keyIcon
            )
            if (icon != null) {
                icon!!.setBounds(0, 0, icon!!.intrinsicWidth, icon!!.intrinsicHeight)
            }
            label = a.getText(R.styleable.AospKeyboard_Key_keyLabel)
            text = a.getText(R.styleable.AospKeyboard_Key_keyOutputText)

            if (codes == null && !TextUtils.isEmpty(label)) {
                codes = intArrayOf(label.get(0).code)
            }
            a.recycle()
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.WnnKeyboard_Key)
            isSecondKey = a.getBoolean(R.styleable.WnnKeyboard_Key_isSecondKey, false)
            a.recycle()
        }

        /**
         * Informs the key that it has been pressed, in case it needs to change its appearance or
         * state.
         * @see .onReleased
         */
        fun onPressed() {
            pressed = !pressed
        }

        /**
         * Changes the pressed state of the key. If it is a sticky key, it will also change the
         * toggled state of the key if the finger was release inside.
         * @param inside whether the finger was released inside the key
         * @see .onPressed
         */
        fun onReleased(inside: Boolean) {
            pressed = !pressed
            if (sticky) {
                on = !on
            }
        }

        fun parseCSV(value: String): IntArray {
            var count = 0
            var lastIndex = 0
            if (value.length > 0) {
                count++
                while ((value.indexOf(",", lastIndex + 1).also { lastIndex = it }) > 0) {
                    count++
                }
            }
            val values = IntArray(count)
            count = 0
            val st = StringTokenizer(value, ",")
            while (st.hasMoreTokens()) {
                try {
                    values[count++] = st.nextToken().toInt()
                } catch (nfe: NumberFormatException) {
                    Log.e(
                        TAG,
                        "Error parsing keycodes $value"
                    )
                }
            }
            return values
        }

        /**
         * Detects if a point falls inside this key.
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return whether or not the point falls inside the key. If the key is attached to an edge,
         * it will assume that all points between the key and the edge are considered to be inside
         * the key.
         */
        fun isInside(x: Int, y: Int): Boolean {
            val leftEdge = (edgeFlags and EDGE_LEFT) > 0
            val rightEdge = (edgeFlags and EDGE_RIGHT) > 0
            val topEdge = (edgeFlags and EDGE_TOP) > 0
            val bottomEdge = (edgeFlags and EDGE_BOTTOM) > 0
            return if ((x >= this.x || (leftEdge && x <= this.x + this.width))
                && (x < this.x + this.width || (rightEdge && x >= this.x))
                && (y >= this.y || (topEdge && y <= this.y + this.height))
                && (y < this.y + this.height || (bottomEdge && y >= this.y))
            ) {
                true
            } else {
                false
            }
        }

        /**
         * Detects if a area falls inside this key.
         * @param x the x-coordinate of the area
         * @param y the y-coordinate of the area
         * @param w the width of the area
         * @param h the height of the area
         * @return whether or not the area falls inside the key.
         */
        fun isInside(x: Int, y: Int, w: Int, h: Int): Boolean {
            return if ((this.x <= (x + w)) && (x <= (this.x + this.width))
                && (this.y <= (y + h)) && (y <= (this.y + this.height))
            ) {
                true
            } else {
                false
            }
        }

        /**
         * Returns the square of the distance between the center of the key and the given point.
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return the square of the distance of the point from the center of the key
         */
        fun squaredDistanceFrom(x: Int, y: Int): Int {
            val xDist = this.x + width / 2 - x
            val yDist = this.y + height / 2 - y
            return xDist * xDist + yDist * yDist
        }

        val currentDrawableState: IntArray
            /**
             * Returns the drawable state for the key, based on the current state and type of the key.
             * @return the drawable state of the key.
             * @see android.graphics.drawable.StateListDrawable.setState
             */
            get() {
                var states =
                    KEY_STATE_NORMAL

                if (on) {
                    states = if (pressed) {
                        KEY_STATE_PRESSED_ON
                    } else {
                        KEY_STATE_NORMAL_ON
                    }
                } else {
                    if (sticky) {
                        states = if (pressed) {
                            KEY_STATE_PRESSED_OFF
                        } else {
                            KEY_STATE_NORMAL_OFF
                        }
                    } else {
                        if (pressed) {
                            states =
                                KEY_STATE_PRESSED
                        }
                    }
                }
                return states
            }

        companion object {
            private val KEY_STATE_NORMAL_ON = intArrayOf(
                android.R.attr.state_checkable,
                android.R.attr.state_checked
            )

            private val KEY_STATE_PRESSED_ON = intArrayOf(
                android.R.attr.state_pressed,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
            )

            private val KEY_STATE_NORMAL_OFF = intArrayOf(
                android.R.attr.state_checkable
            )

            private val KEY_STATE_PRESSED_OFF = intArrayOf(
                android.R.attr.state_pressed,
                android.R.attr.state_checkable
            )

            private val KEY_STATE_NORMAL = intArrayOf()

            private val KEY_STATE_PRESSED = intArrayOf(
                android.R.attr.state_pressed
            )
        }
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId keyboard mode identifier
     */
    /**
     * Creates a keyboard from the given xml key layout file.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     */
    init {
        val dm = context.resources.displayMetrics
        mDisplayWidth = dm.widthPixels
        mDisplayHeight = dm.heightPixels

        horizontalGap = 0
        keyWidth = mDisplayWidth / 10
        verticalGap = 0
        keyHeight = keyWidth
        mKeys = ArrayList()
        mModifierKeys = ArrayList()
        mKeyboardMode = modeId
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
    }

    /**
     *
     * Creates a blank keyboard from the given resource file and populates it with the specified
     * characters in left-to-right, top-to-bottom fashion, using the specified number of columns.
     *
     *
     * If the specified number of columns is -1, then the keyboard will fit as many keys as
     * possible in each row.
     * @param context the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters the list of characters to display on the keyboard. One key will be created
     * for each character.
     * @param columns the number of columns of keys to display. If this number is greater than the
     * number of keys that can fit in a row, it will be ignored. If this number is -1, the
     * keyboard will fit as many keys as possible in each row.
     */
    constructor(
        context: Context, layoutTemplateResId: Int,
        characters: CharSequence, columns: Int, horizontalPadding: Int
    ) : this(context, layoutTemplateResId) {
        var x = 0
        var y = 0
        var column = 0
        minWidth = 0

        val row = Row(this)
        row.defaultHeight = keyHeight
        row.defaultWidth = keyWidth
        row.defaultHorizontalGap = horizontalGap
        row.verticalGap = verticalGap
        row.rowEdgeFlags = EDGE_TOP or EDGE_BOTTOM
        val maxColumns = if (columns == -1) Int.MAX_VALUE else columns
        for (i in 0 until characters.length) {
            val c = characters[i]
            if (column >= maxColumns
                || x + keyWidth + horizontalPadding > mDisplayWidth
            ) {
                x = 0
                y += verticalGap + keyHeight
                column = 0
            }
            val key = Key(row)
            key.x = x
            key.y = y
            key.label = c.toString()
            key.codes = intArrayOf(c.code)
            column++
            x += key.width + key.gap
            mKeys!!.add(key)
            if (x > minWidth) {
                minWidth = x
            }
        }
        height = y + keyHeight
    }

    val keys: List<Key>?
        /**
         * Get the list of keys in this keyboard.
         *
         * @return The list of keys.
         */
        get() = mKeys

    val modifierKeys: List<Key>?
        /**
         * Get the list of modifier keys such as Shift & Alt, if any.
         *
         * @return The list of modifier keys.
         */
        get() = mModifierKeys

    /**
     * Sets the keyboard to be shifted.
     *
     * @param shiftState  the keyboard shift state.
     * @return `true` if shift state changed.
     */
    fun setShifted(shiftState: Boolean): Boolean {
        if (mShiftKey != null) {
            mShiftKey!!.on = shiftState
        }
        if (isShifted != shiftState) {
            isShifted = shiftState
            return true
        }
        return false
    }

    private fun computeNearestNeighbors() {
        mCellWidth = (minWidth + GRID_WIDTH - 1) / GRID_WIDTH
        mCellHeight = (height + GRID_HEIGHT - 1) / GRID_HEIGHT
        mGridNeighbors = arrayOfNulls(GRID_SIZE)
        val indices = IntArray(mKeys!!.size)
        val gridWidth = GRID_WIDTH * mCellWidth
        val gridHeight = GRID_HEIGHT * mCellHeight
        var x = 0
        while (x < gridWidth) {
            var y = 0
            while (y < gridHeight) {
                var count = 0
                for (i in mKeys.indices) {
                    val key = mKeys[i]
                    if (key.squaredDistanceFrom(
                            x,
                            y
                        ) < mProximityThreshold || key.squaredDistanceFrom(
                            x + mCellWidth - 1,
                            y
                        ) < mProximityThreshold || (key.squaredDistanceFrom(
                            x + mCellWidth - 1,
                            y + mCellHeight - 1
                        )
                                < mProximityThreshold) || key.squaredDistanceFrom(
                            x,
                            y + mCellHeight - 1
                        ) < mProximityThreshold ||
                        key.isInside(x, y, mCellWidth, mCellHeight)
                    ) {
                        indices[count++] = i
                    }
                }
                val cell = IntArray(count)
                System.arraycopy(indices, 0, cell, 0, count)
                mGridNeighbors!![(y / mCellHeight) * GRID_WIDTH + (x / mCellWidth)] = cell
                y += mCellHeight
            }
            x += mCellWidth
        }
    }

    /**
     * Returns the indices of the keys that are closest to the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the array of integer indices for the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    fun getNearestKeys(x: Int, y: Int): IntArray? {
        if (mGridNeighbors == null) computeNearestNeighbors()
        if (x >= 0 && x < minWidth && y >= 0 && y < height) {
            val index = (y / mCellHeight) * GRID_WIDTH + (x / mCellWidth)
            if (index < GRID_SIZE) {
                return mGridNeighbors!![index]
            }
        }
        return IntArray(0)
    }

    protected fun createRowFromXml(res: Resources, parser: XmlResourceParser?): Row {
        return Row(res, this, parser)
    }

    protected fun createKeyFromXml(
        res: Resources, parent: Row, x: Int, y: Int,
        parser: XmlResourceParser?
    ): Key {
        return Key(res, parent, x, y, parser)
    }

    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        val leftMostKey = false
        var row = 0
        var x = 0
        var y = 0
        var key: Key? = null
        var currentRow: Row? = null
        val res = context.resources
        var skipRow = false

        try {
            var event: Int
            while ((parser.next().also { event = it }) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_ROW == tag) {
                        inRow = true
                        x = 0
                        currentRow = createRowFromXml(res, parser)
                        skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode
                        if (skipRow) {
                            skipToEndOfRow(parser)
                            inRow = false
                        }
                    } else if (TAG_KEY == tag) {
                        inKey = true
                        key = createKeyFromXml(res, currentRow!!, x, y, parser)
                        mKeys!!.add(key)
                        if (key.codes!![0] == KEYCODE_SHIFT) {
                            mShiftKey = key
                            shiftKeyIndex = mKeys.size - 1
                            mModifierKeys!!.add(key)
                        } else if (key.codes!![0] == KEYCODE_ALT) {
                            mModifierKeys!!.add(key)
                        }
                    } else if (TAG_KEYBOARD == tag) {
                        parseKeyboardAttributes(res, parser)
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                        x += key!!.gap + key.width
                        if (x > minWidth) {
                            minWidth = x
                        }
                    } else if (inRow) {
                        inRow = false
                        y += currentRow!!.verticalGap
                        y += currentRow.defaultHeight
                        row++
                    } else {
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error:$e")
            e.printStackTrace()
        }
        height = y - verticalGap
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipToEndOfRow(parser: XmlResourceParser) {
        var event: Int
        while ((parser.next().also { event = it }) != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG
                && parser.name == TAG_ROW
            ) {
                break
            }
        }
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val a = res.obtainAttributes(
            Xml.asAttributeSet(parser),
            R.styleable.AospKeyboard
        )

        keyWidth = getDimensionOrFraction(
            a,
            R.styleable.AospKeyboard_keyWidth,
            mDisplayWidth, mDisplayWidth / 10
        )
        keyHeight = getDimensionOrFraction(
            a,
            R.styleable.AospKeyboard_keyHeight,
            mDisplayHeight, 75
        )
        horizontalGap = getDimensionOrFraction(
            a,
            R.styleable.AospKeyboard_horizontalGap,
            mDisplayWidth, 0
        )
        verticalGap = getDimensionOrFraction(
            a,
            R.styleable.AospKeyboard_verticalGap,
            mDisplayHeight, 0
        )
        mProximityThreshold = (keyWidth * SEARCH_DISTANCE).toInt()
        mProximityThreshold = mProximityThreshold * mProximityThreshold
        a.recycle()
    }

    companion object {
        const val TAG: String = "Keyboard"

        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"

        /** Edge of left  */
        const val EDGE_LEFT: Int = 0x01

        /** Edge of right  */
        const val EDGE_RIGHT: Int = 0x02

        /** Edge of top  */
        const val EDGE_TOP: Int = 0x04

        /** Edge of bottom  */
        const val EDGE_BOTTOM: Int = 0x08

        /** Keycode of SHIFT  */
        const val KEYCODE_SHIFT: Int = -1

        /** Keycode of MODE_CHANGE  */
        const val KEYCODE_MODE_CHANGE: Int = -2

        /** Keycode of CANCEL  */
        const val KEYCODE_CANCEL: Int = -3

        /** Keycode of DONE  */
        const val KEYCODE_DONE: Int = -4

        /** Keycode of DELETE  */
        const val KEYCODE_DELETE: Int = -5

        /** Keycode of ALT  */
        const val KEYCODE_ALT: Int = -6

        private const val GRID_WIDTH = 10
        private const val GRID_HEIGHT = 5
        private const val GRID_SIZE = GRID_WIDTH * GRID_HEIGHT

        /** Number of key widths from current touch point to search for nearest keys.  */
        private const val SEARCH_DISTANCE = 1.8f

        fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Int): Int {
            val value = a.peekValue(index) ?: return defValue
            if (value.type == TypedValue.TYPE_DIMENSION) {
                return a.getDimensionPixelOffset(index, defValue)
            } else if (value.type == TypedValue.TYPE_FRACTION) {
                return Math.round(a.getFraction(index, base, base, defValue.toFloat()))
            }
            return defValue
        }
    }
}
