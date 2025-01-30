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

import android.graphics.drawable.Drawable
import java.util.Vector

/**
 * Key icon definition. It is defined in soft keyboard template. A soft keyboard
 * can refer to such an icon in its xml file directly to improve performance.
 */
internal class KeyIconRecord {
    var keyCode: Int = 0
    var icon: Drawable? = null
    var iconPopup: Drawable? = null
}


/**
 * Default definition for a certain key. It is defined in soft keyboard
 * template. A soft keyboard can refer to a default key in its xml file. Nothing
 * of the key can be overwritten, including the size.
 */
internal class KeyRecord {
    var keyId: Int = 0
    var softKey: SoftKey? = null
}


/**
 * Soft keyboard template used by soft keyboards to share common resources. In
 * this way, memory cost is reduced.
 */
class SkbTemplate(val skbTemplateId: Int) {
    var skbBackground: Drawable? = null
        private set
    var balloonBackground: Drawable? = null
        private set
    var popupBackground: Drawable? = null
        private set
    var xMargin: Float = 0f
        private set
    var yMargin: Float = 0f
        private set

    /** Key type list.  */
    private val mKeyTypeList = Vector<SoftKeyType>()

    /**
     * Default key icon list. It is only for keys which do not have popup icons.
     */
    private val mKeyIconRecords = Vector<KeyIconRecord>()

    /**
     * Default key list.
     */
    private val mKeyRecords = Vector<KeyRecord>()

    fun setBackgrounds(
        skbBg: Drawable?, balloonBg: Drawable?,
        popupBg: Drawable?
    ) {
        skbBackground = skbBg
        balloonBackground = balloonBg
        popupBackground = popupBg
    }

    fun setMargins(xMargin: Float, yMargin: Float) {
        this.xMargin = xMargin
        this.yMargin = yMargin
    }

    fun createKeyType(id: Int, bg: Drawable?, hlBg: Drawable?): SoftKeyType {
        return SoftKeyType(id, bg, hlBg)
    }

    fun addKeyType(keyType: SoftKeyType): Boolean {
        // The newly added item should have the right id.
        if (mKeyTypeList.size != keyType.mKeyTypeId) return false
        mKeyTypeList.add(keyType)
        return true
    }

    fun getKeyType(typeId: Int): SoftKeyType? {
        if (typeId < 0 || typeId > mKeyTypeList.size) return null
        return mKeyTypeList.elementAt(typeId)
    }

    fun addDefaultKeyIcons(
        keyCode: Int, icon: Drawable?,
        iconPopup: Drawable?
    ) {
        if (null == icon || null == iconPopup) return

        val iconRecord = KeyIconRecord()
        iconRecord.icon = icon
        iconRecord.iconPopup = iconPopup
        iconRecord.keyCode = keyCode

        val size = mKeyIconRecords.size
        var pos = 0
        while (pos < size) {
            if (mKeyIconRecords[pos].keyCode >= keyCode) break
            pos++
        }
        mKeyIconRecords.add(pos, iconRecord)
    }

    fun getDefaultKeyIcon(keyCode: Int): Drawable? {
        val size = mKeyIconRecords.size
        var pos = 0
        while (pos < size) {
            val iconRecord = mKeyIconRecords[pos]
            if (iconRecord.keyCode < keyCode) {
                pos++
                continue
            }
            if (iconRecord.keyCode == keyCode) {
                return iconRecord.icon
            }
            return null
        }
        return null
    }

    fun getDefaultKeyIconPopup(keyCode: Int): Drawable? {
        val size = mKeyIconRecords.size
        var pos = 0
        while (pos < size) {
            val iconRecord = mKeyIconRecords[pos]
            if (iconRecord.keyCode < keyCode) {
                pos++
                continue
            }
            if (iconRecord.keyCode == keyCode) {
                return iconRecord.iconPopup
            }
            return null
        }
        return null
    }

    fun addDefaultKey(keyId: Int, softKey: SoftKey?) {
        if (null == softKey) return

        val keyRecord = KeyRecord()
        keyRecord.keyId = keyId
        keyRecord.softKey = softKey

        val size = mKeyRecords.size
        var pos = 0
        while (pos < size) {
            if (mKeyRecords[pos].keyId >= keyId) break
            pos++
        }
        mKeyRecords.add(pos, keyRecord)
    }

    fun getDefaultKey(keyId: Int): SoftKey? {
        val size = mKeyRecords.size
        var pos = 0
        while (pos < size) {
            val keyRecord = mKeyRecords[pos]
            if (keyRecord.keyId < keyId) {
                pos++
                continue
            }
            if (keyRecord.keyId == keyId) {
                return keyRecord.softKey
            }
            return null
        }
        return null
    }
}


class SoftKeyType(var mKeyTypeId: Int, var mKeyBg: Drawable?, var mKeyHlBg: Drawable?) {
    var mColor: Int = 0
    var mColorHl: Int = 0
    var mColorBalloon: Int = 0

    fun setColors(color: Int, colorHl: Int, colorBalloon: Int) {
        mColor = color
        mColorHl = colorHl
        mColorBalloon = colorBalloon
    }

    companion object {
        const val KEYTYPE_ID_NORMAL_KEY: Int = 0
    }
}
