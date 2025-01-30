/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Pair
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TabHost
import android.widget.TabHost.OnTabChangeListener
import android.widget.TabHost.TabSpec
import android.widget.TabWidget
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyboardActionListener
import com.android.inputmethod.keyboard.KeyboardLayoutSet
import com.android.inputmethod.keyboard.emoji.EmojiCategory.CategoryProperties
import com.android.inputmethod.keyboard.emoji.EmojiPageKeyboardView.OnKeyEventListener
import com.android.inputmethod.keyboard.internal.KeyDrawParams
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet
import com.android.inputmethod.latin.AudioAndHapticFeedbackManager
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.RichInputMethodSubtype
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.utils.ResourceUtils

/**
 * View class to implement Emoji palettes.
 * The Emoji keyboard consists of group of views layout/emoji_palettes_view.
 *
 *  1.  Emoji category tabs.
 *  1.  Delete button.
 *  1.  Emoji keyboard pages that can be scrolled by swiping horizontally or by selecting a tab.
 *  1.  Back to main keyboard button and enter button.
 *
 * Because of the above reasons, this class doesn't extend [KeyboardView].
 */
class EmojiPalettesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = R.attr.emojiPalettesViewStyle
) :
    LinearLayout(context, attrs, defStyle), OnTabChangeListener, OnPageChangeListener,
    View.OnClickListener,
    OnTouchListener, OnKeyEventListener {
    private val mFunctionalKeyBackgroundId: Int
    private val mSpacebarBackgroundId: Int
    private val mCategoryIndicatorEnabled: Boolean
    private val mCategoryIndicatorDrawableResId: Int
    private val mCategoryIndicatorBackgroundResId: Int
    private val mCategoryPageIndicatorColor: Int
    private val mCategoryPageIndicatorBackground: Int
    private var mEmojiPalettesAdapter: EmojiPalettesAdapter? = null
    private val mEmojiLayoutParams: EmojiLayoutParams
    private val mDeleteKeyOnTouchListener: DeleteKeyOnTouchListener

    private var mDeleteKey: ImageButton? = null
    private var mAlphabetKeyLeft: TextView? = null
    private var mAlphabetKeyRight: TextView? = null
    private var mSpacebar: View? = null

    // TODO: Remove this workaround.
    private var mSpacebarIcon: View? = null
    private var mTabHost: TabHost? = null
    private var mEmojiPager: ViewPager? = null
    private var mCurrentPagerPosition: Int = 0
    private var mEmojiCategoryPageIndicatorView: EmojiCategoryPageIndicatorView? = null

    private var mKeyboardActionListener: KeyboardActionListener? =
        KeyboardActionListener.EMPTY_LISTENER

    private val mEmojiCategory: EmojiCategory

    init {
        val keyboardViewAttr: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.KeyboardView, defStyle, R.style.KeyboardView
        )
        val keyBackgroundId: Int = keyboardViewAttr.getResourceId(
            R.styleable.KeyboardView_keyBackground, 0
        )
        mFunctionalKeyBackgroundId = keyboardViewAttr.getResourceId(
            R.styleable.KeyboardView_functionalKeyBackground, keyBackgroundId
        )
        mSpacebarBackgroundId = keyboardViewAttr.getResourceId(
            R.styleable.KeyboardView_spacebarBackground, keyBackgroundId
        )
        keyboardViewAttr.recycle()
        val builder: KeyboardLayoutSet.Builder = KeyboardLayoutSet.Builder(
            context, null /* editorInfo */
        )
        val res: Resources = context.getResources()
        mEmojiLayoutParams = EmojiLayoutParams(context)
        builder.setSubtype(RichInputMethodSubtype.emojiSubtype)
        builder.setKeyboardGeometry(
            ResourceUtils.getDefaultKeyboardWidth(context),
            mEmojiLayoutParams.mEmojiKeyboardHeight
        )
        val layoutSet: KeyboardLayoutSet = builder.build()
        val emojiPalettesViewAttr: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.EmojiPalettesView, defStyle, R.style.EmojiPalettesView
        )
        mEmojiCategory = EmojiCategory(
            PreferenceManager.getDefaultSharedPreferences(context),
            res, layoutSet, emojiPalettesViewAttr
        )
        mCategoryIndicatorEnabled = emojiPalettesViewAttr.getBoolean(
            R.styleable.EmojiPalettesView_categoryIndicatorEnabled, false
        )
        mCategoryIndicatorDrawableResId = emojiPalettesViewAttr.getResourceId(
            R.styleable.EmojiPalettesView_categoryIndicatorDrawable, 0
        )
        mCategoryIndicatorBackgroundResId = emojiPalettesViewAttr.getResourceId(
            R.styleable.EmojiPalettesView_categoryIndicatorBackground, 0
        )
        mCategoryPageIndicatorColor = emojiPalettesViewAttr.getColor(
            R.styleable.EmojiPalettesView_categoryPageIndicatorColor, 0
        )
        mCategoryPageIndicatorBackground = emojiPalettesViewAttr.getColor(
            R.styleable.EmojiPalettesView_categoryPageIndicatorBackground, 0
        )
        emojiPalettesViewAttr.recycle()
        mDeleteKeyOnTouchListener = DeleteKeyOnTouchListener()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val res: Resources = getContext().getResources()
        // The main keyboard expands to the entire this {@link KeyboardView}.
        val width: Int = (ResourceUtils.getDefaultKeyboardWidth(getContext())
                + getPaddingLeft() + getPaddingRight())
        val height: Int = (ResourceUtils.getDefaultKeyboardHeight(res)
                + res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
                + getPaddingTop() + getPaddingBottom())
        setMeasuredDimension(width, height)
    }

    private fun addTab(host: TabHost, categoryId: Int) {
        val tabId: String =
            EmojiCategory.getCategoryName(categoryId, 0 /* categoryPageId */)
        val tspec: TabSpec = host.newTabSpec(tabId)
        tspec.setContent(R.id.emoji_keyboard_dummy)
        val iconView: ImageView = LayoutInflater.from(getContext()).inflate(
            R.layout.emoji_keyboard_tab_icon, null
        ) as ImageView
        // TODO: Replace background color with its own setting rather than using the
        //       category page indicator background as a workaround.
        iconView.setBackgroundColor(mCategoryPageIndicatorBackground)
        iconView.setImageResource(mEmojiCategory.getCategoryTabIcon(categoryId))
        iconView.setContentDescription(mEmojiCategory.getAccessibilityDescription(categoryId))
        tspec.setIndicator(iconView)
        host.addTab(tspec)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mTabHost = findViewById<View>(R.id.emoji_category_tabhost) as TabHost?
        mTabHost!!.setup()
        for (properties: CategoryProperties
        in mEmojiCategory.getShownCategories()) {
            addTab(mTabHost!!, properties.mCategoryId)
        }
        mTabHost!!.setOnTabChangedListener(this)
        val tabWidget: TabWidget = mTabHost!!.getTabWidget()
        tabWidget.setStripEnabled(mCategoryIndicatorEnabled)
        if (mCategoryIndicatorEnabled) {
            // On TabWidget's strip, what looks like an indicator is actually a background.
            // And what looks like a background are actually left and right drawables.
            tabWidget.setBackgroundResource(mCategoryIndicatorDrawableResId)
            tabWidget.setLeftStripDrawable(mCategoryIndicatorBackgroundResId)
            tabWidget.setRightStripDrawable(mCategoryIndicatorBackgroundResId)
        }

        mEmojiPalettesAdapter = EmojiPalettesAdapter(mEmojiCategory, this)

        mEmojiPager = findViewById<View>(R.id.emoji_keyboard_pager) as ViewPager?
        mEmojiPager!!.setAdapter(mEmojiPalettesAdapter)
        mEmojiPager!!.setOnPageChangeListener(this)
        mEmojiPager!!.setOffscreenPageLimit(0)
        mEmojiPager!!.setPersistentDrawingCache(PERSISTENT_NO_CACHE)
        mEmojiLayoutParams.setPagerProperties(mEmojiPager!!)

        mEmojiCategoryPageIndicatorView =
            findViewById<View>(R.id.emoji_category_page_id_view) as EmojiCategoryPageIndicatorView?
        mEmojiCategoryPageIndicatorView!!.setColors(
            mCategoryPageIndicatorColor, mCategoryPageIndicatorBackground
        )
        mEmojiLayoutParams.setCategoryPageIdViewProperties(mEmojiCategoryPageIndicatorView!!)

        setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true /* force */)

        val actionBar: LinearLayout = findViewById<View>(R.id.emoji_action_bar) as LinearLayout
        mEmojiLayoutParams.setActionBarProperties(actionBar)

        // deleteKey depends only on OnTouchListener.
        mDeleteKey = findViewById<View>(R.id.emoji_keyboard_delete) as ImageButton?
        mDeleteKey!!.setBackgroundResource(mFunctionalKeyBackgroundId)
        mDeleteKey!!.setTag(Constants.CODE_DELETE)
        mDeleteKey!!.setOnTouchListener(mDeleteKeyOnTouchListener)

        // {@link #mAlphabetKeyLeft}, {@link #mAlphabetKeyRight, and spaceKey depend on
        // {@link View.OnClickListener} as well as {@link View.OnTouchListener}.
        // {@link View.OnTouchListener} is used as the trigger of key-press, while
        // {@link View.OnClickListener} is used as the trigger of key-release which does not occur
        // if the event is canceled by moving off the finger from the view.
        // The text on alphabet keys are set at
        // {@link #startEmojiPalettes(String,int,float,Typeface)}.
        mAlphabetKeyLeft = findViewById<View>(R.id.emoji_keyboard_alphabet_left) as TextView?
        mAlphabetKeyLeft!!.setBackgroundResource(mFunctionalKeyBackgroundId)
        mAlphabetKeyLeft!!.setTag(Constants.CODE_ALPHA_FROM_EMOJI)
        mAlphabetKeyLeft!!.setOnTouchListener(this)
        mAlphabetKeyLeft!!.setOnClickListener(this)
        mAlphabetKeyRight = findViewById<View>(R.id.emoji_keyboard_alphabet_right) as TextView?
        mAlphabetKeyRight!!.setBackgroundResource(mFunctionalKeyBackgroundId)
        mAlphabetKeyRight!!.setTag(Constants.CODE_ALPHA_FROM_EMOJI)
        mAlphabetKeyRight!!.setOnTouchListener(this)
        mAlphabetKeyRight!!.setOnClickListener(this)
        val spaceBar = findViewById<View>(R.id.emoji_keyboard_space)
        this.mSpacebar = spaceBar
        spaceBar.setBackgroundResource(mSpacebarBackgroundId)
        spaceBar.setTag(Constants.CODE_SPACE)
        spaceBar.setOnTouchListener(this)
        spaceBar.setOnClickListener(this)
        mEmojiLayoutParams.setKeyProperties(spaceBar)
        mSpacebarIcon = findViewById(R.id.emoji_keyboard_space_icon)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Add here to the stack trace to nail down the {@link IllegalArgumentException} exception
        // in MotionEvent that sporadically happens.
        // TODO: Remove this override method once the issue has been addressed.
        return super.dispatchTouchEvent(ev)
    }

    override fun onTabChanged(tabId: String) {
        AudioAndHapticFeedbackManager.instance.performHapticAndAudioFeedback(
            Constants.CODE_UNSPECIFIED, this
        )
        val categoryId: Int = mEmojiCategory.getCategoryId(tabId)
        setCurrentCategoryId(categoryId, false /* force */)
        updateEmojiCategoryPageIdView()
    }

    override fun onPageSelected(position: Int) {
        val newPos: Pair<Int, Int>? =
            mEmojiCategory.getCategoryIdAndPageIdFromPagePosition(position)
        setCurrentCategoryId(newPos!!.first!!,  /* categoryId */false /* force */)
        mEmojiCategory.setCurrentCategoryPageId(newPos.second!! /* categoryPageId */)
        updateEmojiCategoryPageIdView()
        mCurrentPagerPosition = position
    }

    override fun onPageScrollStateChanged(state: Int) {
        // Ignore this message. Only want the actual page selected.
    }

    override fun onPageScrolled(
        position: Int, positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        mEmojiPalettesAdapter!!.onPageScrolled()
        val newPos: Pair<Int, Int>? =
            mEmojiCategory.getCategoryIdAndPageIdFromPagePosition(position)
        val newCategoryId: Int = newPos!!.first
        val newCategorySize: Int = mEmojiCategory.getCategoryPageSize(newCategoryId)
        val currentCategoryId: Int = mEmojiCategory.getCurrentCategoryId()
        val currentCategoryPageId: Int = mEmojiCategory.getCurrentCategoryPageId()
        val currentCategorySize: Int = mEmojiCategory.getCurrentCategoryPageSize()
        if (newCategoryId == currentCategoryId) {
            mEmojiCategoryPageIndicatorView!!.setCategoryPageId(
                newCategorySize, newPos.second!!, positionOffset
            )
        } else if (newCategoryId > currentCategoryId) {
            mEmojiCategoryPageIndicatorView!!.setCategoryPageId(
                currentCategorySize, currentCategoryPageId, positionOffset
            )
        } else if (newCategoryId < currentCategoryId) {
            mEmojiCategoryPageIndicatorView!!.setCategoryPageId(
                currentCategorySize, currentCategoryPageId, positionOffset - 1
            )
        }
    }

    /**
     * Called from [EmojiPageKeyboardView] through [OnTouchListener]
     * interface to handle touch events from View-based elements such as the space bar.
     * Note that this method is used only for observing [MotionEvent.ACTION_DOWN] to trigger
     * [KeyboardActionListener.onPressKey]. [KeyboardActionListener.onReleaseKey] will
     * be covered by [.onClick] as long as the event is not canceled.
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false
        }
        val tag: Any = v.getTag()
        if (tag !is Int) {
            return false
        }
        mKeyboardActionListener!!.onPressKey(
            tag, 0,  /* repeatCount */true /* isSinglePointer */
        )
        // It's important to return false here. Otherwise, {@link #onClick} and touch-down visual
        // feedback stop working.
        return false
    }

    /**
     * Called from [EmojiPageKeyboardView] through [OnClickListener]
     * interface to handle non-canceled touch-up events from View-based elements such as the space
     * bar.
     */
    override fun onClick(v: View) {
        val tag: Any = v.getTag()
        if (tag !is Int) {
            return
        }
        val code: Int = tag
        mKeyboardActionListener!!.onCodeInput(
            code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
            false /* isKeyRepeat */
        )
        mKeyboardActionListener!!.onReleaseKey(code, false /* withSliding */)
    }

    /**
     * Called from [EmojiPageKeyboardView] through
     * [EmojiPageKeyboardView.OnKeyEventListener]
     * interface to handle touch events from non-View-based elements such as Emoji buttons.
     */
    override fun onPressKey(key: Key) {
        val code: Int = key.code
        mKeyboardActionListener!!.onPressKey(code, 0,  /* repeatCount */true /* isSinglePointer */)
    }

    /**
     * Called from [EmojiPageKeyboardView] through
     * [EmojiPageKeyboardView.OnKeyEventListener]
     * interface to handle touch events from non-View-based elements such as Emoji buttons.
     */
    override fun onReleaseKey(key: Key) {
        mEmojiPalettesAdapter!!.addRecentKey(key)
        mEmojiCategory.saveLastTypedCategoryPage()
        val code: Int = key.code
        if (code == Constants.CODE_OUTPUT_TEXT) {
            mKeyboardActionListener!!.onTextInput(key.outputText)
        } else {
            mKeyboardActionListener!!.onCodeInput(
                code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                false /* isKeyRepeat */
            )
        }
        mKeyboardActionListener!!.onReleaseKey(code, false /* withSliding */)
    }

    fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        if (!enabled) return
        // TODO: Should use LAYER_TYPE_SOFTWARE when hardware acceleration is off?
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun startEmojiPalettes(
        switchToAlphaLabel: String?,
        keyVisualAttr: KeyVisualAttributes?,
        iconSet: KeyboardIconsSet
    ) {
        val deleteIconResId: Int =
            iconSet.getIconResourceId(KeyboardIconsSet.NAME_DELETE_KEY)
        if (deleteIconResId != 0) {
            mDeleteKey!!.setImageResource(deleteIconResId)
        }
        val spacebarResId: Int =
            iconSet.getIconResourceId(KeyboardIconsSet.NAME_SPACE_KEY)
        if (spacebarResId != 0) {
            // TODO: Remove this workaround to place the spacebar icon.
            mSpacebarIcon!!.setBackgroundResource(spacebarResId)
        }
        val params: KeyDrawParams = KeyDrawParams()
        params.updateParams(mEmojiLayoutParams.getActionBarHeight(), keyVisualAttr)
        setupAlphabetKey(mAlphabetKeyLeft!!, switchToAlphaLabel, params)
        setupAlphabetKey(mAlphabetKeyRight!!, switchToAlphaLabel, params)
        mEmojiPager!!.setAdapter(mEmojiPalettesAdapter)
        mEmojiPager!!.setCurrentItem(mCurrentPagerPosition)
    }

    fun stopEmojiPalettes() {
        mEmojiPalettesAdapter!!.releaseCurrentKey(true /* withKeyRegistering */)
        mEmojiPalettesAdapter!!.flushPendingRecentKeys()
        mEmojiPager!!.setAdapter(null)
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener?) {
        mKeyboardActionListener = listener
        mDeleteKeyOnTouchListener.setKeyboardActionListener(listener)
    }

    private fun updateEmojiCategoryPageIdView() {
        if (mEmojiCategoryPageIndicatorView == null) {
            return
        }
        mEmojiCategoryPageIndicatorView!!.setCategoryPageId(
            mEmojiCategory.getCurrentCategoryPageSize(),
            mEmojiCategory.getCurrentCategoryPageId(), 0.0f /* offset */
        )
    }

    private fun setCurrentCategoryId(categoryId: Int, force: Boolean) {
        val oldCategoryId: Int = mEmojiCategory.getCurrentCategoryId()
        if (oldCategoryId == categoryId && !force) {
            return
        }

        if (oldCategoryId == EmojiCategory.ID_RECENTS) {
            // Needs to save pending updates for recent keys when we get out of the recents
            // category because we don't want to move the recent emojis around while the user
            // is in the recents category.
            mEmojiPalettesAdapter!!.flushPendingRecentKeys()
        }

        mEmojiCategory.setCurrentCategoryId(categoryId)
        val newTabId: Int = mEmojiCategory.getTabIdFromCategoryId(categoryId)
        val newCategoryPageId: Int = mEmojiCategory.getPageIdFromCategoryId(categoryId)
        if (force || mEmojiCategory.getCategoryIdAndPageIdFromPagePosition(
                mEmojiPager!!.getCurrentItem()
            )!!.first != categoryId
        ) {
            mEmojiPager!!.setCurrentItem(newCategoryPageId, false /* smoothScroll */)
        }
        if (force || mTabHost!!.getCurrentTab() != newTabId) {
            mTabHost!!.setCurrentTab(newTabId)
        }
    }

    private class DeleteKeyOnTouchListener : OnTouchListener {
        private var mKeyboardActionListener: KeyboardActionListener? =
            KeyboardActionListener.EMPTY_LISTENER

        fun setKeyboardActionListener(listener: KeyboardActionListener?) {
            mKeyboardActionListener = listener
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.getActionMasked()) {
                MotionEvent.ACTION_DOWN -> {
                    onTouchDown(v)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val x: Float = event.getX()
                    val y: Float = event.getY()
                    if (x < 0.0f || v.getWidth() < x || y < 0.0f || v.getHeight() < y) {
                        // Stop generating key events once the finger moves away from the view area.
                        onTouchCanceled(v)
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    onTouchUp(v)
                    return true
                }
            }
            return false
        }

        fun onTouchDown(v: View) {
            mKeyboardActionListener!!.onPressKey(
                Constants.CODE_DELETE,
                0,  /* repeatCount */true /* isSinglePointer */
            )
            v.setPressed(true /* pressed */)
        }

        fun onTouchUp(v: View) {
            mKeyboardActionListener!!.onCodeInput(
                Constants.CODE_DELETE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false /* isKeyRepeat */
            )
            mKeyboardActionListener!!.onReleaseKey(Constants.CODE_DELETE, false /* withSliding */)
            v.setPressed(false /* pressed */)
        }

        fun onTouchCanceled(v: View) {
            v.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    companion object {
        private fun setupAlphabetKey(
            alphabetKey: TextView, label: String?,
            params: KeyDrawParams
        ) {
            alphabetKey.setText(label)
            alphabetKey.setTextColor(params.mFunctionalTextColor)
            alphabetKey.setTextSize(TypedValue.COMPLEX_UNIT_PX, params.mLabelSize.toFloat())
            alphabetKey.setTypeface(params.mTypeface)
        }
    }
}