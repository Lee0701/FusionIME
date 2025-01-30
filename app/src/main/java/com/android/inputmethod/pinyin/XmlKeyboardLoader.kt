/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import com.android.inputmethod.pinyin.SoftKeyToggle.ToggleState
import com.android.inputmethod.pinyin.SoftKeyboard.KeyRow
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.regex.Pattern

/**
 * Class used to load a soft keyboard or a soft keyboard template from xml
 * files.
 */
class XmlKeyboardLoader(private val mContext: Context?) {
    private val mResources: Resources

    /** The event type in parsing the xml file.  */
    private var mXmlEventType: Int = 0

    /**
     * The current soft keyboard template used by the current soft keyboard
     * under loading.
     */
    private var mSkbTemplate: SkbTemplate? = null

    /** The x position for the next key.  */
    var mKeyXPos: Float = 0f

    /** The y position for the next key.  */
    var mKeyYPos: Float = 0f

    /** The width of the keyboard to load.  */
    var mSkbWidth: Int = 0

    /** The height of the keyboard to load.  */
    var mSkbHeight: Int = 0

    /** Key margin in x-way.  */
    var mKeyXMargin: Float = 0f

    /** Key margin in y-way.  */
    var mKeyYMargin: Float = 0f

    /**
     * Used to indicate whether next event has been fetched during processing
     * the the current event.
     */
    var mNextEventFetched: Boolean = false

    var mAttrTmp: String? = null

    internal inner class KeyCommonAttributes(var mXrp: XmlResourceParser) {
        var keyType: Int = 0
        var keyWidth: Float = 0f
        var keyHeight: Float = 0f
        var repeat: Boolean = false
        var balloon: Boolean = true

        // Make sure the default object is not null.
        fun getAttributes(defAttr: KeyCommonAttributes): Boolean {
            keyType = getInteger(mXrp, XMLATTR_KEY_TYPE, defAttr.keyType)
            keyWidth = getFloat(mXrp, XMLATTR_KEY_WIDTH, defAttr.keyWidth)
            keyHeight = getFloat(mXrp, XMLATTR_KEY_HEIGHT, defAttr.keyHeight)
            repeat = getBoolean(mXrp, XMLATTR_KEY_REPEAT, defAttr.repeat)
            balloon = getBoolean(mXrp, XMLATTR_KEY_BALLOON, defAttr.balloon)
            if (keyType < 0 || keyWidth <= 0 || keyHeight <= 0) {
                return false
            }
            return true
        }
    }

    init {
        mResources = mContext!!.getResources()
    }

    fun loadSkbTemplate(resourceId: Int): SkbTemplate? {
        if (null == mContext || 0 == resourceId) {
            return null
        }
        val r: Resources = mResources
        val xrp: XmlResourceParser = r.getXml(resourceId)

        val attrDef: KeyCommonAttributes = KeyCommonAttributes(xrp)
        val attrKey: KeyCommonAttributes = KeyCommonAttributes(xrp)

        mSkbTemplate = SkbTemplate(resourceId)
        var lastKeyTypeId: Int = KEYTYPE_ID_LAST
        var globalColor: Int = 0
        var globalColorHl: Int = 0
        var globalColorBalloon: Int = 0
        try {
            mXmlEventType = xrp.next()
            while (mXmlEventType != XmlResourceParser.END_DOCUMENT) {
                mNextEventFetched = false
                if (mXmlEventType == XmlResourceParser.START_TAG) {
                    val attribute: String = xrp.getName()
                    if (XMLTAG_SKB_TEMPLATE.compareTo(attribute) == 0) {
                        val skbBg: Drawable? = getDrawable(xrp, XMLATTR_SKB_BG, null)
                        val balloonBg: Drawable? = getDrawable(
                            xrp,
                            XMLATTR_BALLOON_BG, null
                        )
                        val popupBg: Drawable? = getDrawable(
                            xrp, XMLATTR_POPUP_BG,
                            null
                        )
                        if (null == skbBg || null == balloonBg || null == popupBg) {
                            return null
                        }
                        mSkbTemplate!!.setBackgrounds(skbBg, balloonBg, popupBg)

                        val xMargin: Float = getFloat(xrp, XMLATTR_KEY_XMARGIN, 0f)
                        val yMargin: Float = getFloat(xrp, XMLATTR_KEY_YMARGIN, 0f)
                        mSkbTemplate!!.setMargins(xMargin, yMargin)

                        // Get default global colors.
                        globalColor = getColor(xrp, XMLATTR_COLOR, 0)
                        globalColorHl = getColor(
                            xrp, XMLATTR_COLOR_HIGHLIGHT,
                            -0x1
                        )
                        globalColorBalloon = getColor(
                            xrp,
                            XMLATTR_COLOR_BALLOON, -0x1
                        )
                    } else if (XMLTAG_KEYTYPE.compareTo(attribute) == 0) {
                        val id: Int = getInteger(xrp, XMLATTR_ID, KEYTYPE_ID_LAST)
                        val bg: Drawable? = getDrawable(xrp, XMLATTR_KEYTYPE_BG, null)
                        val hlBg: Drawable? = getDrawable(
                            xrp, XMLATTR_KEYTYPE_HLBG,
                            null
                        )
                        val color: Int = getColor(xrp, XMLATTR_COLOR, globalColor)
                        val colorHl: Int = getColor(
                            xrp, XMLATTR_COLOR_HIGHLIGHT,
                            globalColorHl
                        )
                        val colorBalloon: Int = getColor(
                            xrp, XMLATTR_COLOR_BALLOON,
                            globalColorBalloon
                        )
                        if (id != lastKeyTypeId + 1) {
                            return null
                        }
                        val keyType: SoftKeyType = mSkbTemplate!!.createKeyType(
                            id,
                            bg, hlBg
                        )
                        keyType.setColors(color, colorHl, colorBalloon)
                        if (!mSkbTemplate!!.addKeyType(keyType)) {
                            return null
                        }
                        lastKeyTypeId = id
                    } else if (XMLTAG_KEYICON.compareTo(attribute) == 0) {
                        val keyCode: Int = getInteger(xrp, XMLATTR_KEY_CODE, 0)
                        val icon: Drawable? = getDrawable(xrp, XMLATTR_KEY_ICON, null)
                        val iconPopup: Drawable? = getDrawable(
                            xrp,
                            XMLATTR_KEY_ICON_POPUP, null
                        )
                        if (null != icon && null != iconPopup) {
                            mSkbTemplate!!.addDefaultKeyIcons(
                                keyCode, icon,
                                iconPopup
                            )
                        }
                    } else if (XMLTAG_KEY.compareTo(attribute) == 0) {
                        val keyId: Int =
                            this.getInteger(xrp, XMLATTR_ID, -1)
                        if (-1 == keyId) return null

                        if (!attrKey.getAttributes(attrDef)) {
                            return null
                        }

                        // Update the key position for the key.
                        mKeyXPos = getFloat(xrp, XMLATTR_START_POS_X, 0f)
                        mKeyYPos = getFloat(xrp, XMLATTR_START_POS_Y, 0f)

                        val softKey: SoftKey? = getSoftKey(xrp, attrKey)
                        if (null == softKey) return null
                        mSkbTemplate!!.addDefaultKey(keyId, softKey)
                    }
                }
                // Get the next tag.
                if (!mNextEventFetched) mXmlEventType = xrp.next()
            }
            xrp.close()
            return mSkbTemplate
        } catch (e: XmlPullParserException) {
            // Log.e(TAG, "Ill-formatted keyboard template resource file");
        } catch (e: IOException) {
            // Log.e(TAG, "Unable to keyboard template resource file");
        }
        return null
    }

    fun loadKeyboard(resourceId: Int, skbWidth: Int, skbHeight: Int): SoftKeyboard? {
        if (null == mContext) return null
        val r: Resources = mResources
        val skbPool: SkbPool = SkbPool.instance
        val xrp: XmlResourceParser = mContext.getResources().getXml(resourceId)
        mSkbTemplate = null
        var softKeyboard: SoftKeyboard? = null
        var skbBg: Drawable?
        var popupBg: Drawable?
        var balloonBg: Drawable?
        var softKey: SoftKey? = null

        val attrDef = KeyCommonAttributes(xrp)
        val attrSkb = KeyCommonAttributes(xrp)
        val attrRow = KeyCommonAttributes(xrp)
        val attrKeys = KeyCommonAttributes(xrp)
        val attrKey = KeyCommonAttributes(xrp)

        mKeyXPos = 0f
        mKeyYPos = 0f
        mSkbWidth = skbWidth
        mSkbHeight = skbHeight

        try {
            mKeyXMargin = 0f
            mKeyYMargin = 0f
            mXmlEventType = xrp.next()
            while (mXmlEventType != XmlResourceParser.END_DOCUMENT) {
                mNextEventFetched = false
                if (mXmlEventType == XmlResourceParser.START_TAG) {
                    var attr: String? = xrp.getName()
                    // 1. Is it the root element, "keyboard"?
                    if (XMLTAG_KEYBOARD.compareTo(attr!!) == 0) {
                        // 1.1 Get the keyboard template id.
                        val skbTemplateId: Int = xrp.getAttributeResourceValue(
                            null,
                            XMLATTR_SKB_TEMPLATE, 0
                        )

                        // 1.2 Try to get the template from pool. If it is not
                        // in, the pool will try to load it.
                        mSkbTemplate = skbPool.getSkbTemplate(
                            skbTemplateId,
                            mContext
                        )

                        if (null == mSkbTemplate
                            || !attrSkb.getAttributes(attrDef)
                        ) {
                            return null
                        }

                        val cacheFlag: Boolean = getBoolean(
                            xrp,
                            XMLATTR_SKB_CACHE_FLAG, DEFAULT_SKB_CACHE_FLAG
                        )
                        val stickyFlag: Boolean = getBoolean(
                            xrp,
                            XMLATTR_SKB_STICKY_FLAG,
                            DEFAULT_SKB_STICKY_FLAG
                        )
                        val isQwerty: Boolean = getBoolean(
                            xrp, XMLATTR_QWERTY,
                            false
                        )
                        val isQwertyUpperCase: Boolean = getBoolean(
                            xrp,
                            XMLATTR_QWERTY_UPPERCASE, false
                        )

                        softKeyboard = SoftKeyboard(
                            resourceId,
                            mSkbTemplate!!, mSkbWidth, mSkbHeight
                        )
                        softKeyboard.setFlags(
                            cacheFlag, stickyFlag, isQwerty,
                            isQwertyUpperCase
                        )

                        mKeyXMargin = getFloat(
                            xrp, XMLATTR_KEY_XMARGIN,
                            mSkbTemplate!!.xMargin
                        )
                        mKeyYMargin = getFloat(
                            xrp, XMLATTR_KEY_YMARGIN,
                            mSkbTemplate!!.yMargin
                        )
                        skbBg = getDrawable(xrp, XMLATTR_SKB_BG, null)
                        popupBg = getDrawable(xrp, XMLATTR_POPUP_BG, null)
                        balloonBg = getDrawable(xrp, XMLATTR_BALLOON_BG, null)
                        if (null != skbBg) {
                            softKeyboard.skbBackground = skbBg
                        }
                        if (null != popupBg) {
                            softKeyboard.popupBackground = popupBg
                        }
                        if (null != balloonBg) {
                            softKeyboard.setKeyBalloonBackground(balloonBg)
                        }
                        softKeyboard.setKeyMargins(mKeyXMargin, mKeyYMargin)
                    } else if (XMLTAG_ROW.compareTo(attr) == 0) {
                        if (!attrRow.getAttributes(attrSkb)) {
                            return null
                        }
                        // Get the starting positions for the row.
                        mKeyXPos = getFloat(xrp, XMLATTR_START_POS_X, 0f)
                        mKeyYPos = getFloat(xrp, XMLATTR_START_POS_Y, mKeyYPos)
                        val rowId: Int = getInteger(
                            xrp, XMLATTR_ROW_ID,
                            KeyRow.ALWAYS_SHOW_ROW_ID
                        )
                        softKeyboard!!.beginNewRow(rowId, mKeyYPos)
                    } else if (XMLTAG_KEYS.compareTo(attr) == 0) {
                        if (null == softKeyboard) return null
                        if (!attrKeys.getAttributes(attrRow)) {
                            return null
                        }

                        var splitter: String? = xrp.getAttributeValue(
                            null,
                            XMLATTR_KEY_SPLITTER
                        )
                        splitter = Pattern.quote(splitter)
                        val labels: String? = xrp.getAttributeValue(
                            null,
                            XMLATTR_KEY_LABELS
                        )
                        val codes: String? = xrp.getAttributeValue(
                            null,
                            XMLATTR_KEY_CODES
                        )
                        if (null == splitter || null == labels) {
                            return null
                        }
                        val labelArr: Array<String> =
                            labels.split(splitter.toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        var codeArr: Array<String>? = null
                        if (null != codes) {
                            codeArr = codes.split(splitter.toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            if (labelArr.size != codeArr.size) {
                                return null
                            }
                        }

                        for (i in labelArr.indices) {
                            softKey = SoftKey()
                            var keyCode: Int = 0
                            if (null != codeArr) {
                                keyCode = codeArr.get(i).toInt()
                            }
                            softKey.setKeyAttribute(
                                keyCode, labelArr.get(i),
                                attrKeys.repeat, attrKeys.balloon
                            )

                            softKey.setKeyType(
                                mSkbTemplate!!
                                    .getKeyType(attrKeys.keyType), null, null
                            )

                            var left: Float
                            var right: Float
                            var top: Float
                            var bottom: Float
                            left = mKeyXPos

                            right = left + attrKeys.keyWidth
                            top = mKeyYPos
                            bottom = top + attrKeys.keyHeight

                            if (right - left < 2 * mKeyXMargin) return null
                            if (bottom - top < 2 * mKeyYMargin) return null

                            softKey.setKeyDimensions(left, top, right, bottom)
                            softKeyboard.addSoftKey(softKey)
                            mKeyXPos = right
                            if (mKeyXPos.toInt() * mSkbWidth > mSkbWidth) {
                                return null
                            }
                        }
                    } else if (XMLTAG_KEY.compareTo(attr) == 0) {
                        if (null == softKeyboard) {
                            return null
                        }
                        if (!attrKey.getAttributes(attrRow)) {
                            return null
                        }

                        val keyId: Int =
                            this.getInteger(xrp, XMLATTR_ID, -1)
                        if (keyId >= 0) {
                            softKey = mSkbTemplate!!.getDefaultKey(keyId)
                        } else {
                            softKey = getSoftKey(xrp, attrKey)
                        }
                        if (null == softKey) return null

                        // Update the position for next key.
                        mKeyXPos = softKey.mRightF
                        if (mKeyXPos.toInt() * mSkbWidth > mSkbWidth) {
                            return null
                        }
                        // If the current xml event type becomes a starting tag,
                        // it indicates that we have parsed too much to get
                        // toggling states, and we started a new row. In this
                        // case, the row starting position information should
                        // be updated.
                        if (mXmlEventType == XmlResourceParser.START_TAG) {
                            attr = xrp.getName()
                            if (XMLTAG_ROW.compareTo(attr) == 0) {
                                mKeyYPos += attrRow.keyHeight
                                if (mKeyYPos.toInt() * mSkbHeight > mSkbHeight) {
                                    return null
                                }
                            }
                        }
                        softKeyboard.addSoftKey(softKey)
                    }
                } else if (mXmlEventType == XmlResourceParser.END_TAG) {
                    val attr: String = xrp.getName()
                    if (XMLTAG_ROW.compareTo(attr) == 0) {
                        mKeyYPos += attrRow.keyHeight
                        if (mKeyYPos.toInt() * mSkbHeight > mSkbHeight) {
                            return null
                        }
                    }
                }

                // Get the next tag.
                if (!mNextEventFetched) mXmlEventType = xrp.next()
            }
            xrp.close()
            softKeyboard!!.setSkbCoreSize(mSkbWidth, mSkbHeight)
            return softKeyboard
        } catch (e: XmlPullParserException) {
            // Log.e(TAG, "Ill-formatted keybaord resource file");
        } catch (e: IOException) {
            // Log.e(TAG, "Unable to read keyboard resource file");
        }
        return null
    }

    // Caller makes sure xrp and r are valid.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun getSoftKey(
        xrp: XmlResourceParser,
        attrKey: KeyCommonAttributes
    ): SoftKey? {
        val keyCode: Int = getInteger(xrp, XMLATTR_KEY_CODE, 0)
        val keyLabel: String? = getString(xrp, XMLATTR_KEY_LABEL, null)
        var keyIcon: Drawable? = getDrawable(xrp, XMLATTR_KEY_ICON, null)
        var keyIconPopup: Drawable? = getDrawable(xrp, XMLATTR_KEY_ICON_POPUP, null)
        val popupSkbId: Int = xrp.getAttributeResourceValue(
            null,
            XMLATTR_KEY_POPUP_SKBID, 0
        )

        if (null == keyLabel && null == keyIcon) {
            keyIcon = mSkbTemplate!!.getDefaultKeyIcon(keyCode)
            keyIconPopup = mSkbTemplate!!.getDefaultKeyIconPopup(keyCode)
            if (null == keyIcon || null == keyIconPopup) return null
        }

        // Dimension information must been initialized before
        // getting toggle state, because mKeyYPos may be changed
        // to next row when trying to get toggle state.
        val left: Float
        val right: Float
        val top: Float
        val bottom: Float
        left = mKeyXPos
        right = left + attrKey.keyWidth
        top = mKeyYPos
        bottom = top + attrKey.keyHeight

        if (right - left < 2 * mKeyXMargin) return null
        if (bottom - top < 2 * mKeyYMargin) return null

        // Try to find if the next tag is
        // {@link #XMLTAG_TOGGLE_STATE_OF_KEY}, if yes, try to
        // create a toggle key.
        var toggleKey: Boolean = false
        mXmlEventType = xrp.next()
        mNextEventFetched = true

        val softKey: SoftKey
        if (mXmlEventType == XmlResourceParser.START_TAG) {
            mAttrTmp = xrp.getName()
            if (mAttrTmp?.compareTo(XMLTAG_TOGGLE_STATE) == 0) {
                toggleKey = true
            }
        }
        if (toggleKey) {
            softKey = SoftKeyToggle()
            if (!softKey.setToggleStates(
                    getToggleStates(
                        attrKey, softKey, keyCode
                    )
                )
            ) {
                return null
            }
        } else {
            softKey = SoftKey()
        }

        // Set the normal state
        softKey.setKeyAttribute(
            keyCode, keyLabel, attrKey.repeat,
            attrKey.balloon
        )
        softKey.setPopupSkbId(popupSkbId)
        softKey.setKeyType(
            mSkbTemplate!!.getKeyType(attrKey.keyType), keyIcon,
            keyIconPopup
        )

        softKey.setKeyDimensions(left, top, right, bottom)
        return softKey
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun getToggleStates(
        attrKey: KeyCommonAttributes, softKey: SoftKeyToggle, defKeyCode: Int
    ): ToggleState? {
        val xrp: XmlResourceParser = attrKey.mXrp
        val stateId: Int = getInteger(xrp, XMLATTR_TOGGLE_STATE_ID, 0)
        if (0 == stateId) return null

        val keyLabel: String? = getString(xrp, XMLATTR_KEY_LABEL, null)
        val keyTypeId: Int = getInteger(xrp, XMLATTR_KEY_TYPE, KEYTYPE_ID_LAST)
        val keyCode: Int
        if (null == keyLabel) {
            keyCode = getInteger(xrp, XMLATTR_KEY_CODE, defKeyCode)
        } else {
            keyCode = getInteger(xrp, XMLATTR_KEY_CODE, 0)
        }
        val icon: Drawable? = getDrawable(xrp, XMLATTR_KEY_ICON, null)
        val iconPopup: Drawable? = getDrawable(xrp, XMLATTR_KEY_ICON_POPUP, null)
        if (null == icon && null == keyLabel) {
            return null
        }
        val rootState: ToggleState = softKey.createToggleState()
        rootState.setStateId(stateId)
        rootState.mKeyType = null
        if (KEYTYPE_ID_LAST != keyTypeId) {
            rootState.mKeyType = mSkbTemplate!!.getKeyType(keyTypeId)
        }
        rootState.mKeyCode = keyCode
        rootState.mKeyIcon = icon
        rootState.mKeyIconPopup = iconPopup
        rootState.mKeyLabel = keyLabel

        val repeat: Boolean = getBoolean(xrp, XMLATTR_KEY_REPEAT, attrKey.repeat)
        val balloon: Boolean = getBoolean(xrp, XMLATTR_KEY_BALLOON, attrKey.balloon)
        rootState.setStateFlags(repeat, balloon)

        rootState.mNextState = null

        // If there is another toggle state.
        mXmlEventType = xrp.next()
        while (mXmlEventType != XmlResourceParser.START_TAG
            && mXmlEventType != XmlResourceParser.END_DOCUMENT
        ) {
            mXmlEventType = xrp.next()
        }
        if (mXmlEventType == XmlResourceParser.START_TAG) {
            val attr: String = xrp.getName()
            if (attr.compareTo(XMLTAG_TOGGLE_STATE) == 0) {
                val nextState: ToggleState? = getToggleStates(
                    attrKey,
                    softKey, defKeyCode
                )
                if (null == nextState) return null
                rootState.mNextState = nextState
            }
        }

        return rootState
    }

    private fun getInteger(xrp: XmlResourceParser, name: String, defValue: Int): Int {
        val resId: Int = xrp.getAttributeResourceValue(null, name, 0)
        val s: String?
        if (resId == 0) {
            s = xrp.getAttributeValue(null, name)
            if (null == s) return defValue
            try {
                val ret: Int = s.toInt()
                return ret
            } catch (e: NumberFormatException) {
                return defValue
            }
        } else {
            return mContext!!.getResources().getString(resId).toInt()
        }
    }

    private fun getColor(xrp: XmlResourceParser, name: String, defValue: Int): Int {
        val resId: Int = xrp.getAttributeResourceValue(null, name, 0)
        val s: String?
        if (resId == 0) {
            s = xrp.getAttributeValue(null, name)
            if (null == s) return defValue
            try {
                val ret: Int = s.toInt()
                return ret
            } catch (e: NumberFormatException) {
                return defValue
            }
        } else {
            return mContext!!.getResources().getColor(resId)
        }
    }

    private fun getString(xrp: XmlResourceParser, name: String, defValue: String?): String? {
        val resId: Int = xrp.getAttributeResourceValue(null, name, 0)
        if (resId == 0) {
            return xrp.getAttributeValue(null, name)
        } else {
            return mContext!!.getResources().getString(resId)
        }
    }

    private fun getFloat(xrp: XmlResourceParser, name: String, defValue: Float): Float {
        val resId: Int = xrp.getAttributeResourceValue(null, name, 0)
        if (resId == 0) {
            val s: String? = xrp.getAttributeValue(null, name)
            if (null == s) return defValue
            try {
                val ret: Float
                if (s.endsWith("%p")) {
                    ret = s.substring(0, s.length - 2).toFloat() / 100
                } else {
                    ret = s.toFloat()
                }
                return ret
            } catch (e: NumberFormatException) {
                return defValue
            }
        } else {
            return mContext!!.getResources().getDimension(resId)
        }
    }

    private fun getBoolean(
        xrp: XmlResourceParser, name: String,
        defValue: Boolean
    ): Boolean {
        val s: String? = xrp.getAttributeValue(null, name)
        if (null == s) return defValue
        try {
            val ret: Boolean = s.toBoolean()
            return ret
        } catch (e: NumberFormatException) {
            return defValue
        }
    }

    private fun getDrawable(
        xrp: XmlResourceParser, name: String,
        defValue: Drawable?
    ): Drawable? {
        val resId: Int = xrp.getAttributeResourceValue(null, name, 0)
        if (0 == resId) return defValue
        return mResources.getDrawable(resId)
    }

    companion object {
        /**
         * The tag used to define an xml-based soft keyboard template.
         */
        private const val XMLTAG_SKB_TEMPLATE: String = "skb_template"

        /**
         * The tag used to indicate the soft key type which is defined inside the
         * [.XMLTAG_SKB_TEMPLATE] element in the xml file. file.
         */
        private const val XMLTAG_KEYTYPE: String = "key_type"

        /**
         * The tag used to define a default key icon for enter/delete/space keys. It
         * is defined inside the [.XMLTAG_SKB_TEMPLATE] element in the xml
         * file.
         */
        private const val XMLTAG_KEYICON: String = "key_icon"

        /**
         * Attribute tag of the left and right margin for a key. A key's width
         * should be larger than double of this value. Defined inside
         * [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYBOARD].
         */
        private const val XMLATTR_KEY_XMARGIN: String = "key_xmargin"

        /**
         * Attribute tag of the top and bottom margin for a key. A key's height
         * should be larger than double of this value. Defined inside
         * [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYBOARD].
         */
        private const val XMLATTR_KEY_YMARGIN: String = "key_ymargin"

        /**
         * Attribute tag of the keyboard background image. Defined inside
         * [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYBOARD].
         */
        private const val XMLATTR_SKB_BG: String = "skb_bg"

        /**
         * Attribute tag of the balloon background image for key press. Defined
         * inside [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYBOARD].
         */
        private const val XMLATTR_BALLOON_BG: String = "balloon_bg"

        /**
         * Attribute tag of the popup balloon background image for key press or
         * popup mini keyboard. Defined inside [.XMLTAG_SKB_TEMPLATE] and
         * [.XMLTAG_KEYBOARD].
         */
        private const val XMLATTR_POPUP_BG: String = "popup_bg"

        /**
         * Attribute tag of the color to draw key label. Defined inside
         * [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYTYPE].
         */
        private const val XMLATTR_COLOR: String = "color"

        /**
         * Attribute tag of the color to draw key's highlighted label. Defined
         * inside [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYTYPE].
         */
        private const val XMLATTR_COLOR_HIGHLIGHT: String = "color_highlight"

        /**
         * Attribute tag of the color to draw key's label in the popup balloon.
         * Defined inside [.XMLTAG_SKB_TEMPLATE] and [.XMLTAG_KEYTYPE].
         */
        private const val XMLATTR_COLOR_BALLOON: String = "color_balloon"

        /**
         * Attribute tag of the id of [.XMLTAG_KEYTYPE] and
         * [.XMLTAG_KEY]. Key types and keys defined in a soft keyboard
         * template should have id, because a soft keyboard needs the id to refer to
         * these default definitions. If a key defined in [.XMLTAG_KEYBOARD]
         * does not id, that means the key is newly defined; if it has id (and only
         * has id), the id is used to find the default definition from the soft
         * keyboard template.
         */
        private const val XMLATTR_ID: String = "id"

        /**
         * Attribute tag of the key background for a specified key type. Defined
         * inside [.XMLTAG_KEYTYPE].
         */
        private const val XMLATTR_KEYTYPE_BG: String = "bg"

        /**
         * Attribute tag of the key high-light background for a specified key type.
         * Defined inside [.XMLTAG_KEYTYPE].
         */
        private const val XMLATTR_KEYTYPE_HLBG: String = "hlbg"

        /**
         * Attribute tag of the starting x-position of an element. It can be defined
         * in [.XMLTAG_ROW] and [.XMLTAG_KEY] in {XMLTAG_SKB_TEMPLATE}.
         * If not defined, 0 will be used. For a key defined in
         * [.XMLTAG_KEYBOARD], it always use its previous keys information to
         * calculate its own position.
         */
        private const val XMLATTR_START_POS_X: String = "start_pos_x"

        /**
         * Attribute tag of the starting y-position of an element. It can be defined
         * in [.XMLTAG_ROW] and [.XMLTAG_KEY] in {XMLTAG_SKB_TEMPLATE}.
         * If not defined, 0 will be used. For a key defined in
         * [.XMLTAG_KEYBOARD], it always use its previous keys information to
         * calculate its own position.
         */
        private const val XMLATTR_START_POS_Y: String = "start_pos_y"

        /**
         * Attribute tag of a row's id. Defined [.XMLTAG_ROW]. If not defined,
         * -1 will be used. Rows with id -1 will be enabled always, rows with same
         * row id will be enabled when the id is the same to the activated id of the
         * soft keyboard.
         */
        private const val XMLATTR_ROW_ID: String = "row_id"

        /** The tag used to indicate the keyboard element in the xml file.  */
        private const val XMLTAG_KEYBOARD: String = "keyboard"

        /** The tag used to indicate the row element in the xml file.  */
        private const val XMLTAG_ROW: String = "row"

        /** The tag used to indicate key-array element in the xml file.  */
        private const val XMLTAG_KEYS: String = "keys"

        /**
         * The tag used to indicate a key element in the xml file. If the element is
         * defined in a soft keyboard template, it should have an id. If it is
         * defined in a soft keyboard, id is not required.
         */
        private const val XMLTAG_KEY: String = "key"

        /** The tag used to indicate a key's toggle element in the xml file.  */
        private const val XMLTAG_TOGGLE_STATE: String = "toggle_state"

        /**
         * Attribute tag of the toggle state id for toggle key. Defined inside
         * [.XMLTAG_TOGGLE_STATE]
         */
        private const val XMLATTR_TOGGLE_STATE_ID: String = "state_id"

        /** Attribute tag of key template for the soft keyboard.  */
        private const val XMLATTR_SKB_TEMPLATE: String = "skb_template"

        /**
         * Attribute tag used to indicate whether this soft keyboard needs to be
         * cached in memory for future use. [.DEFAULT_SKB_CACHE_FLAG]
         * specifies the default value.
         */
        private const val XMLATTR_SKB_CACHE_FLAG: String = "skb_cache_flag"

        /**
         * Attribute tag used to indicate whether this soft keyboard is sticky. A
         * sticky soft keyboard will keep the current layout unless user makes a
         * switch explicitly. A none sticky soft keyboard will automatically goes
         * back to the previous keyboard after click a none-function key.
         * [.DEFAULT_SKB_STICKY_FLAG] specifies the default value.
         */
        private const val XMLATTR_SKB_STICKY_FLAG: String = "skb_sticky_flag"

        /** Attribute tag to indicate whether it is a QWERTY soft keyboard.  */
        private const val XMLATTR_QWERTY: String = "qwerty"

        /**
         * When the soft keyboard is a QWERTY one, this attribute tag to get the
         * information that whether it is defined in upper case.
         */
        private const val XMLATTR_QWERTY_UPPERCASE: String = "qwerty_uppercase"

        /** Attribute tag of key type.  */
        private const val XMLATTR_KEY_TYPE: String = "key_type"

        /** Attribute tag of key width.  */
        private const val XMLATTR_KEY_WIDTH: String = "width"

        /** Attribute tag of key height.  */
        private const val XMLATTR_KEY_HEIGHT: String = "height"

        /** Attribute tag of the key's repeating ability.  */
        private const val XMLATTR_KEY_REPEAT: String = "repeat"

        /** Attribute tag of the key's behavior for balloon.  */
        private const val XMLATTR_KEY_BALLOON: String = "balloon"

        /** Attribute tag of the key splitter in a key array.  */
        private const val XMLATTR_KEY_SPLITTER: String = "splitter"

        /** Attribute tag of the key labels in a key array.  */
        private const val XMLATTR_KEY_LABELS: String = "labels"

        /** Attribute tag of the key codes in a key array.  */
        private const val XMLATTR_KEY_CODES: String = "codes"

        /** Attribute tag of the key label in a key.  */
        private const val XMLATTR_KEY_LABEL: String = "label"

        /** Attribute tag of the key code in a key.  */
        private const val XMLATTR_KEY_CODE: String = "code"

        /** Attribute tag of the key icon in a key.  */
        private const val XMLATTR_KEY_ICON: String = "icon"

        /** Attribute tag of the key's popup icon in a key.  */
        private const val XMLATTR_KEY_ICON_POPUP: String = "icon_popup"

        /** The id for a mini popup soft keyboard.  */
        private const val XMLATTR_KEY_POPUP_SKBID: String = "popup_skb"

        private val DEFAULT_SKB_CACHE_FLAG: Boolean = true

        private val DEFAULT_SKB_STICKY_FLAG: Boolean = true

        /**
         * The key type id for invalid key type. It is also used to generate next
         * valid key type id by adding 1.
         */
        private val KEYTYPE_ID_LAST: Int = -1
    }
}
