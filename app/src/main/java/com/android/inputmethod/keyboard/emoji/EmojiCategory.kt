/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.inputmethod.keyboard.emoji

import android.content.SharedPreferences
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build.VERSION_CODES
import android.util.Log
import android.util.Pair
import com.android.inputmethod.compat.BuildCompatUtils
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardId
import com.android.inputmethod.keyboard.KeyboardLayoutSet
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.settings.Settings
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

internal class EmojiCategory(
    prefs: SharedPreferences, res: Resources,
    layoutSet: KeyboardLayoutSet, emojiPaletteViewAttr: TypedArray
) {
    private val TAG: String = EmojiCategory::class.java.getSimpleName()

    inner class CategoryProperties(categoryId: Int, pageCount: Int) {
        val mCategoryId: Int
        val mPageCount: Int

        init {
            mCategoryId = categoryId
            mPageCount = pageCount
        }
    }

    private val mPrefs: SharedPreferences
    private val mRes: Resources
    private val mMaxPageKeyCount: Int
    private val mLayoutSet: KeyboardLayoutSet
    private val mCategoryNameToIdMap: HashMap<String, Int> = HashMap()
    private val mCategoryTabIconId: IntArray = IntArray(sCategoryName.size)
    private val mShownCategories: ArrayList<CategoryProperties> = ArrayList()
    private val mCategoryKeyboardMap: ConcurrentHashMap<Long, DynamicGridKeyboard> =
        ConcurrentHashMap()

    private var mCurrentCategoryId: Int = ID_UNSPECIFIED
    private var mCurrentCategoryPageId: Int = 0

    private fun addShownCategoryId(categoryId: Int) {
        // Load a keyboard of categoryId
        getKeyboard(categoryId, 0 /* categoryPageId */)
        val properties: CategoryProperties =
            CategoryProperties(categoryId, getCategoryPageCount(categoryId))
        mShownCategories.add(properties)
    }

    private fun isShownCategoryId(categoryId: Int): Boolean {
        for (prop: CategoryProperties in mShownCategories) {
            if (prop.mCategoryId == categoryId) {
                return true
            }
        }
        return false
    }

    fun getCategoryId(name: String): Int {
        val strings: Array<String> =
            name.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return mCategoryNameToIdMap.get(strings.get(0))!!
    }

    fun getCategoryTabIcon(categoryId: Int): Int {
        return mCategoryTabIconId.get(categoryId)
    }

    fun getAccessibilityDescription(categoryId: Int): String {
        return mRes.getString(sAccessibilityDescriptionResourceIdsForCategories.get(categoryId))
    }

    fun getShownCategories(): ArrayList<CategoryProperties> {
        return mShownCategories
    }

    fun getCurrentCategoryId(): Int {
        return mCurrentCategoryId
    }

    fun getCurrentCategoryPageSize(): Int {
        return getCategoryPageSize(mCurrentCategoryId)
    }

    fun getCategoryPageSize(categoryId: Int): Int {
        for (prop: CategoryProperties in mShownCategories) {
            if (prop.mCategoryId == categoryId) {
                return prop.mPageCount
            }
        }
        Log.w(TAG, "Invalid category id: " + categoryId)
        // Should not reach here.
        return 0
    }

    fun setCurrentCategoryId(categoryId: Int) {
        mCurrentCategoryId = categoryId
        Settings.Companion.writeLastShownEmojiCategoryId(mPrefs, categoryId)
    }

    fun setCurrentCategoryPageId(id: Int) {
        mCurrentCategoryPageId = id
    }

    fun getCurrentCategoryPageId(): Int {
        return mCurrentCategoryPageId
    }

    fun saveLastTypedCategoryPage() {
        Settings.Companion.writeLastTypedEmojiCategoryPageId(
            mPrefs, mCurrentCategoryId, mCurrentCategoryPageId
        )
    }

    fun isInRecentTab(): Boolean {
        return mCurrentCategoryId == ID_RECENTS
    }

    fun getTabIdFromCategoryId(categoryId: Int): Int {
        for (i in mShownCategories.indices) {
            if (mShownCategories.get(i).mCategoryId == categoryId) {
                return i
            }
        }
        Log.w(TAG, "categoryId not found: " + categoryId)
        return 0
    }

    // Returns the view pager's page position for the categoryId
    fun getPageIdFromCategoryId(categoryId: Int): Int {
        val lastSavedCategoryPageId: Int =
            Settings.Companion.readLastTypedEmojiCategoryPageId(mPrefs, categoryId)
        var sum: Int = 0
        for (i in mShownCategories.indices) {
            val props: CategoryProperties = mShownCategories.get(i)
            if (props.mCategoryId == categoryId) {
                return sum + lastSavedCategoryPageId
            }
            sum += props.mPageCount
        }
        Log.w(TAG, "categoryId not found: " + categoryId)
        return 0
    }

    fun getRecentTabId(): Int {
        return getTabIdFromCategoryId(ID_RECENTS)
    }

    private fun getCategoryPageCount(categoryId: Int): Int {
        val keyboard: Keyboard = mLayoutSet.getKeyboard(
            sCategoryElementId.get(categoryId)
        )
        return (keyboard.getSortedKeys().size - 1) / mMaxPageKeyCount + 1
    }

    // Returns a pair of the category id and the category page id from the view pager's page
    // position. The category page id is numbered in each category. And the view page position
    // is the position of the current shown page in the view pager which contains all pages of
    // all categories.
    fun getCategoryIdAndPageIdFromPagePosition(position: Int): Pair<Int, Int>? {
        var sum: Int = 0
        for (properties: CategoryProperties in mShownCategories) {
            val temp: Int = sum
            sum += properties.mPageCount
            if (sum > position) {
                return Pair(properties.mCategoryId, position - temp)
            }
        }
        return null
    }

    // Returns a keyboard from the view pager's page position.
    fun getKeyboardFromPagePosition(position: Int): DynamicGridKeyboard? {
        val categoryAndId: Pair<Int, Int>? =
            getCategoryIdAndPageIdFromPagePosition(position)
        if (categoryAndId != null) {
            return getKeyboard(categoryAndId.first, categoryAndId.second)
        }
        return null
    }

    fun getKeyboard(categoryId: Int, id: Int): DynamicGridKeyboard? {
        synchronized(mCategoryKeyboardMap) {
            val categoryKeyboardMapKey: Long = getCategoryKeyboardMapKey(categoryId, id)
            if (mCategoryKeyboardMap.containsKey(categoryKeyboardMapKey)) {
                return mCategoryKeyboardMap.get(categoryKeyboardMapKey)
            }

            if (categoryId == ID_RECENTS) {
                val kbd: DynamicGridKeyboard = DynamicGridKeyboard(
                    mPrefs,
                    mLayoutSet.getKeyboard(KeyboardId.Companion.ELEMENT_EMOJI_RECENTS),
                    mMaxPageKeyCount, categoryId
                )
                mCategoryKeyboardMap.put(categoryKeyboardMapKey, kbd)
                return kbd
            }

            val keyboard: Keyboard = mLayoutSet.getKeyboard(
                sCategoryElementId.get(categoryId)
            )
            val sortedKeys: Array<Array<Key?>> = sortKeysIntoPages(
                keyboard.getSortedKeys(), mMaxPageKeyCount
            )
            for (pageId in sortedKeys.indices) {
                val tempKeyboard: DynamicGridKeyboard = DynamicGridKeyboard(
                    mPrefs,
                    mLayoutSet.getKeyboard(KeyboardId.Companion.ELEMENT_EMOJI_RECENTS),
                    mMaxPageKeyCount, categoryId
                )
                for (emojiKey: Key? in sortedKeys.get(pageId)) {
                    if (emojiKey == null) {
                        break
                    }
                    tempKeyboard.addKeyLast(emojiKey)
                }
                mCategoryKeyboardMap.put(
                    getCategoryKeyboardMapKey(categoryId, pageId), tempKeyboard
                )
            }
            return mCategoryKeyboardMap.get(categoryKeyboardMapKey)
        }
    }

    fun getTotalPageCountOfAllCategories(): Int {
        var sum: Int = 0
        for (properties: CategoryProperties in mShownCategories) {
            sum += properties.mPageCount
        }
        return sum
    }

    init {
        mPrefs = prefs
        mRes = res
        mMaxPageKeyCount = res.getInteger(R.integer.config_emoji_keyboard_max_page_key_count)
        mLayoutSet = layoutSet
        for (i in sCategoryName.indices) {
            mCategoryNameToIdMap.put(sCategoryName.get(i), i)
            mCategoryTabIconId.get(i) = emojiPaletteViewAttr.getResourceId(
                sCategoryTabIconAttr.get(i), 0
            )
        }

        var defaultCategoryId: Int = ID_SYMBOLS
        addShownCategoryId(ID_RECENTS)
        if (BuildCompatUtils.EFFECTIVE_SDK_INT >= VERSION_CODES.KITKAT) {
            if (canShowUnicodeEightEmoji()) {
                defaultCategoryId = ID_EIGHT_SMILEY_PEOPLE
                addShownCategoryId(ID_EIGHT_SMILEY_PEOPLE)
                addShownCategoryId(ID_EIGHT_ANIMALS_NATURE)
                addShownCategoryId(ID_EIGHT_FOOD_DRINK)
                addShownCategoryId(ID_EIGHT_TRAVEL_PLACES)
                addShownCategoryId(ID_EIGHT_ACTIVITY)
                addShownCategoryId(ID_EIGHT_OBJECTS)
                addShownCategoryId(ID_EIGHT_SYMBOLS)
                addShownCategoryId(ID_FLAGS) // Exclude combinations without glyphs.
            } else {
                defaultCategoryId = ID_PEOPLE
                addShownCategoryId(ID_PEOPLE)
                addShownCategoryId(ID_OBJECTS)
                addShownCategoryId(ID_NATURE)
                addShownCategoryId(ID_PLACES)
                addShownCategoryId(ID_SYMBOLS)
                if (canShowFlagEmoji()) {
                    addShownCategoryId(ID_FLAGS)
                }
            }
        } else {
            addShownCategoryId(ID_SYMBOLS)
        }
        addShownCategoryId(ID_EMOTICONS)

        val recentsKbd: DynamicGridKeyboard? =
            getKeyboard(ID_RECENTS, 0 /* categoryPageId */)
        recentsKbd!!.loadRecentKeys(mCategoryKeyboardMap.values)

        mCurrentCategoryId =
            Settings.Companion.readLastShownEmojiCategoryId(mPrefs, defaultCategoryId)
        Log.i(TAG, "Last Emoji category id is " + mCurrentCategoryId)
        if (!isShownCategoryId(mCurrentCategoryId)) {
            Log.i(
                TAG, "Last emoji category " + mCurrentCategoryId +
                        " is invalid, starting in " + defaultCategoryId
            )
            mCurrentCategoryId = defaultCategoryId
        } else if (mCurrentCategoryId == ID_RECENTS &&
            recentsKbd.getSortedKeys().isEmpty()
        ) {
            Log.i(TAG, "No recent emojis found, starting in category " + defaultCategoryId)
            mCurrentCategoryId = defaultCategoryId
        }
    }

    companion object {
        private val ID_UNSPECIFIED: Int = -1
        const val ID_RECENTS: Int = 0
        private const val ID_PEOPLE: Int = 1
        private const val ID_OBJECTS: Int = 2
        private const val ID_NATURE: Int = 3
        private const val ID_PLACES: Int = 4
        private const val ID_SYMBOLS: Int = 5
        private const val ID_EMOTICONS: Int = 6
        private const val ID_FLAGS: Int = 7
        private const val ID_EIGHT_SMILEY_PEOPLE: Int = 8
        private const val ID_EIGHT_ANIMALS_NATURE: Int = 9
        private const val ID_EIGHT_FOOD_DRINK: Int = 10
        private const val ID_EIGHT_TRAVEL_PLACES: Int = 11
        private const val ID_EIGHT_ACTIVITY: Int = 12
        private const val ID_EIGHT_OBJECTS: Int = 13
        private const val ID_EIGHT_SYMBOLS: Int = 14
        private const val ID_EIGHT_FLAGS: Int = 15
        private const val ID_EIGHT_SMILEY_PEOPLE_BORING: Int = 16

        private val sCategoryName: Array<String> = arrayOf(
            "recents",
            "people",
            "objects",
            "nature",
            "places",
            "symbols",
            "emoticons",
            "flags",
            "smiley & people",
            "animals & nature",
            "food & drink",
            "travel & places",
            "activity",
            "objects2",
            "symbols2",
            "flags2",
            "smiley & people2"
        )

        private val sCategoryTabIconAttr: IntArray = intArrayOf(
            R.styleable.EmojiPalettesView_iconEmojiRecentsTab,
            R.styleable.EmojiPalettesView_iconEmojiCategory1Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory2Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory3Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory4Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory5Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory6Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory7Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory8Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory9Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory10Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory11Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory12Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory13Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory14Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory15Tab,
            R.styleable.EmojiPalettesView_iconEmojiCategory16Tab
        )

        private val sAccessibilityDescriptionResourceIdsForCategories: IntArray = intArrayOf(
            R.string.spoken_descrption_emoji_category_recents,
            R.string.spoken_descrption_emoji_category_people,
            R.string.spoken_descrption_emoji_category_objects,
            R.string.spoken_descrption_emoji_category_nature,
            R.string.spoken_descrption_emoji_category_places,
            R.string.spoken_descrption_emoji_category_symbols,
            R.string.spoken_descrption_emoji_category_emoticons,
            R.string.spoken_descrption_emoji_category_flags,
            R.string.spoken_descrption_emoji_category_eight_smiley_people,
            R.string.spoken_descrption_emoji_category_eight_animals_nature,
            R.string.spoken_descrption_emoji_category_eight_food_drink,
            R.string.spoken_descrption_emoji_category_eight_travel_places,
            R.string.spoken_descrption_emoji_category_eight_activity,
            R.string.spoken_descrption_emoji_category_objects,
            R.string.spoken_descrption_emoji_category_symbols,
            R.string.spoken_descrption_emoji_category_flags,
            R.string.spoken_descrption_emoji_category_eight_smiley_people
        )

        private val sCategoryElementId: IntArray = intArrayOf(
            KeyboardId.Companion.ELEMENT_EMOJI_RECENTS,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY1,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY2,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY3,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY4,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY5,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY6,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY7,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY8,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY9,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY10,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY11,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY12,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY13,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY14,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY15,
            KeyboardId.Companion.ELEMENT_EMOJI_CATEGORY16
        )

        fun getCategoryName(categoryId: Int, categoryPageId: Int): String {
            return sCategoryName.get(categoryId) + "-" + categoryPageId
        }

        private fun getCategoryKeyboardMapKey(categoryId: Int, id: Int): Long {
            return ((categoryId.toLong()) shl Integer.SIZE) or id.toLong()
        }

        private val EMOJI_KEY_COMPARATOR: Comparator<Key?> = object : Comparator<Key> {
            override fun compare(lhs: Key, rhs: Key): Int {
                val lHitBox: Rect? = lhs.getHitBox()
                val rHitBox: Rect? = rhs.getHitBox()
                if (lHitBox!!.top < rHitBox!!.top) {
                    return -1
                } else if (lHitBox.top > rHitBox.top) {
                    return 1
                }
                if (lHitBox.left < rHitBox.left) {
                    return -1
                } else if (lHitBox.left > rHitBox.left) {
                    return 1
                }
                if (lhs.getCode() == rhs.getCode()) {
                    return 0
                }
                return if (lhs.getCode() < rhs.getCode()) -1 else 1
            }
        }

        private fun sortKeysIntoPages(inKeys: List<Key?>, maxPageCount: Int): Array<Array<Key?>> {
            val keys: ArrayList<Key?> = ArrayList(inKeys)
            Collections.sort(keys, EMOJI_KEY_COMPARATOR)
            val pageCount: Int = (keys.size - 1) / maxPageCount + 1
            val retval: Array<Array<Key?>> = Array(pageCount) { arrayOfNulls(maxPageCount) }
            for (i in keys.indices) {
                retval.get(i / maxPageCount).get(i % maxPageCount) = keys.get(i)
            }
            return retval
        }

        private fun canShowFlagEmoji(): Boolean {
            val paint: Paint = Paint()
            val switzerland: String =
                "\uD83C\uDDE8\uD83C\uDDED" //  U+1F1E8 U+1F1ED Flag for Switzerland
            try {
                return paint.hasGlyph(switzerland)
            } catch (e: NoSuchMethodError) {
                // Compare display width of single-codepoint emoji to width of flag emoji to determine
                // whether flag is rendered as single glyph or two adjacent regional indicator symbols.
                val flagWidth: Float = paint.measureText(switzerland)
                val standardWidth: Float = paint.measureText("\uD83D\uDC27") //  U+1F427 Penguin
                return flagWidth < standardWidth * 1.25
                // This assumes that a valid glyph for the flag emoji must be less than 1.25 times
                // the width of the penguin.
            }
        }

        private fun canShowUnicodeEightEmoji(): Boolean {
            val paint: Paint = Paint()
            val cheese: String = "\uD83E\uDDC0" //  U+1F9C0 Cheese wedge
            try {
                return paint.hasGlyph(cheese)
            } catch (e: NoSuchMethodError) {
                val cheeseWidth: Float = paint.measureText(cheese)
                val tofuWidth: Float = paint.measureText("\uFFFE")
                return cheeseWidth > tofuWidth
                // This assumes that a valid glyph for the cheese wedge must be greater than the width
                // of the noncharacter.
            }
        }
    }
}
