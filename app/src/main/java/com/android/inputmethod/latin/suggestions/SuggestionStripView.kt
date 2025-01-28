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
package com.android.inputmethod.latin.suggestions

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.MainKeyboardView
import com.android.inputmethod.keyboard.MoreKeysPanel
import com.android.inputmethod.latin.AudioAndHapticFeedbackManager
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.define.DebugFlags
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.settings.SettingsValues
import com.android.inputmethod.latin.suggestions.MoreSuggestionsView.MoreSuggestionsListener
import com.android.inputmethod.latin.suggestions.SuggestionStripView
import com.android.inputmethod.latin.utils.ImportantNoticeUtils
import kotlin.math.abs

class SuggestionStripView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?,
    defStyle: Int = R.attr.suggestionStripViewStyle
) :
    RelativeLayout(context, attrs, defStyle), View.OnClickListener, OnLongClickListener {
    interface Listener {
        fun showImportantNoticeContents()
        fun pickSuggestionManually(word: SuggestedWordInfo)
        fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean)
    }

    private val mSuggestionsStrip: ViewGroup
    private val mVoiceKey: ImageButton
    private val mImportantNoticeStrip: View
    var mMainKeyboardView: MainKeyboardView? = null

    private val mMoreSuggestionsContainer: View
    private val mMoreSuggestionsView: MoreSuggestionsView
    private val mMoreSuggestionsBuilder: MoreSuggestions.Builder

    private val mWordViews: ArrayList<TextView> = ArrayList()
    private val mDebugInfoViews: ArrayList<TextView> = ArrayList()
    private val mDividerViews: ArrayList<View> = ArrayList()

    var mListener: Listener? = null
    private var mSuggestedWords: SuggestedWords = SuggestedWords.emptyInstance
    private var mStartIndexOfMoreSuggestions: Int = 0

    private val mLayoutHelper: SuggestionStripLayoutHelper
    private val mStripVisibilityGroup: StripVisibilityGroup

    private class StripVisibilityGroup(
        suggestionStripView: View,
        suggestionsStrip: ViewGroup, importantNoticeStrip: View
    ) {
        private val mSuggestionStripView: View
        private val mSuggestionsStrip: View
        private val mImportantNoticeStrip: View

        init {
            mSuggestionStripView = suggestionStripView
            mSuggestionsStrip = suggestionsStrip
            mImportantNoticeStrip = importantNoticeStrip
            showSuggestionsStrip()
        }

        fun setLayoutDirection(isRtlLanguage: Boolean) {
            val layoutDirection: Int = if (isRtlLanguage)
                ViewCompat.LAYOUT_DIRECTION_RTL
            else
                ViewCompat.LAYOUT_DIRECTION_LTR
            ViewCompat.setLayoutDirection(mSuggestionStripView, layoutDirection)
            ViewCompat.setLayoutDirection(mSuggestionsStrip, layoutDirection)
            ViewCompat.setLayoutDirection(mImportantNoticeStrip, layoutDirection)
        }

        fun showSuggestionsStrip() {
            mSuggestionsStrip.setVisibility(VISIBLE)
            mImportantNoticeStrip.setVisibility(INVISIBLE)
        }

        fun showImportantNoticeStrip() {
            mSuggestionsStrip.setVisibility(INVISIBLE)
            mImportantNoticeStrip.setVisibility(VISIBLE)
        }

        val isShowingImportantNoticeStrip: Boolean
            get() {
                return mImportantNoticeStrip.getVisibility() == VISIBLE
            }
    }

    /**
     * A connection back to the input method.
     * @param listener
     */
    fun setListener(listener: Listener?, inputView: View) {
        mListener = listener
        mMainKeyboardView = inputView.findViewById<View>(R.id.keyboard_view) as MainKeyboardView?
    }

    fun updateVisibility(shouldBeVisible: Boolean, isFullscreenMode: Boolean) {
        val visibility: Int =
            if (shouldBeVisible) VISIBLE else (if (isFullscreenMode) GONE else INVISIBLE)
        setVisibility(visibility)
        val currentSettingsValues: SettingsValues? = Settings.instance.current
        mVoiceKey.setVisibility(if (currentSettingsValues!!.mShowsVoiceInputKey) VISIBLE else INVISIBLE)
    }

    fun setSuggestions(suggestedWords: SuggestedWords, isRtlLanguage: Boolean) {
        clear()
        mStripVisibilityGroup.setLayoutDirection(isRtlLanguage)
        mSuggestedWords = suggestedWords
        mStartIndexOfMoreSuggestions = mLayoutHelper.layoutAndReturnStartIndexOfMoreSuggestions(
            getContext(), mSuggestedWords, mSuggestionsStrip, this
        )
        mStripVisibilityGroup.showSuggestionsStrip()
    }

    fun setMoreSuggestionsHeight(remainingHeight: Int) {
        mLayoutHelper.mMoreSuggestionsRowHeight = remainingHeight
    }

    // This method checks if we should show the important notice (checks on permanent storage if
    // it has been shown once already or not, and if in the setup wizard). If applicable, it shows
    // the notice. In all cases, it returns true if it was shown, false otherwise.
    fun maybeShowImportantNoticeTitle(): Boolean {
        val currentSettingsValues: SettingsValues? = Settings.instance.current
        if (!ImportantNoticeUtils.shouldShowImportantNotice(
                getContext(),
                currentSettingsValues!!
            )
        ) {
            return false
        }
        if (getWidth() <= 0) {
            return false
        }
        val importantNoticeTitle: String = ImportantNoticeUtils.getSuggestContactsNoticeTitle(
            getContext()
        )
        if (TextUtils.isEmpty(importantNoticeTitle)) {
            return false
        }
        if (isShowingMoreSuggestionPanel) {
            dismissMoreSuggestionsPanel()
        }
        mLayoutHelper.layoutImportantNotice(mImportantNoticeStrip, importantNoticeTitle)
        mStripVisibilityGroup.showImportantNoticeStrip()
        mImportantNoticeStrip.setOnClickListener(this)
        return true
    }

    fun clear() {
        mSuggestionsStrip.removeAllViews()
        removeAllDebugInfoViews()
        mStripVisibilityGroup.showSuggestionsStrip()
        dismissMoreSuggestionsPanel()
    }

    private fun removeAllDebugInfoViews() {
        // The debug info views may be placed as children views of this {@link SuggestionStripView}.
        for (debugInfoView: View in mDebugInfoViews) {
            val parent: ViewParent? = debugInfoView.parent
            if (parent is ViewGroup) {
                parent.removeView(debugInfoView)
            }
        }
    }

    private val mMoreSuggestionsListener: MoreSuggestionsListener =
        object : MoreSuggestionsListener() {
            override fun onSuggestionSelected(wordInfo: SuggestedWordInfo) {
                mListener!!.pickSuggestionManually(wordInfo)
                dismissMoreSuggestionsPanel()
            }

            override fun onCancelInput() {
                dismissMoreSuggestionsPanel()
            }
        }

    private val mMoreSuggestionsController: MoreKeysPanel.Controller =
        object : MoreKeysPanel.Controller {
            override fun onDismissMoreKeysPanel() {
                mMainKeyboardView!!.onDismissMoreKeysPanel()
            }

            override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {
                mMainKeyboardView!!.onShowMoreKeysPanel(panel)
            }

            override fun onCancelMoreKeysPanel() {
                dismissMoreSuggestionsPanel()
            }
        }

    val isShowingMoreSuggestionPanel: Boolean
        get() {
            return mMoreSuggestionsView.isShowingInParent
        }

    fun dismissMoreSuggestionsPanel() {
        mMoreSuggestionsView.dismissMoreKeysPanel()
    }

    override fun onLongClick(view: View): Boolean {
        AudioAndHapticFeedbackManager.instance.performHapticAndAudioFeedback(
            Constants.NOT_A_CODE, this
        )
        return showMoreSuggestions()
    }

    fun showMoreSuggestions(): Boolean {
        val parentKeyboard: Keyboard? = mMainKeyboardView?.keyboard
        if (parentKeyboard == null) {
            return false
        }
        val layoutHelper: SuggestionStripLayoutHelper = mLayoutHelper
        if (mSuggestedWords.size() <= mStartIndexOfMoreSuggestions) {
            return false
        }
        val stripWidth: Int = getWidth()
        val container: View = mMoreSuggestionsContainer
        val maxWidth: Int = stripWidth - container.getPaddingLeft() - container.getPaddingRight()
        val builder: MoreSuggestions.Builder = mMoreSuggestionsBuilder
        builder.layout(
            mSuggestedWords, mStartIndexOfMoreSuggestions, maxWidth,
            (maxWidth * layoutHelper.mMinMoreSuggestionsWidth).toInt(),
            layoutHelper.maxMoreSuggestionsRow, parentKeyboard
        )
        mMoreSuggestionsView.keyboard = builder.build()
        container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val moreKeysPanel: MoreKeysPanel = mMoreSuggestionsView
        val pointX: Int = stripWidth / 2
        val pointY: Int = -layoutHelper.mMoreSuggestionsBottomGap
        moreKeysPanel.showMoreKeysPanel(
            this, mMoreSuggestionsController, pointX, pointY,
            mMoreSuggestionsListener
        )
        mOriginX = mLastX
        mOriginY = mLastY
        for (i in 0 until mStartIndexOfMoreSuggestions) {
            mWordViews.get(i).setPressed(false)
        }
        return true
    }

    // Working variables for {@link onInterceptTouchEvent(MotionEvent)} and
    // {@link onTouchEvent(MotionEvent)}.
    private var mLastX: Int = 0
    private var mLastY: Int = 0
    private var mOriginX: Int = 0
    private var mOriginY: Int = 0
    private val mMoreSuggestionsModalTolerance: Int
    private var mNeedsToTransformTouchEventToHoverEvent: Boolean = false
    private var mIsDispatchingHoverEventToMoreSuggestions: Boolean = false
    private val mMoreSuggestionsSlidingDetector: GestureDetector
    private val mMoreSuggestionsSlidingListener: GestureDetector.OnGestureListener =
        object : SimpleOnGestureListener() {
            override fun onScroll(
                down: MotionEvent?,
                me: MotionEvent,
                deltaX: Float,
                deltaY: Float
            ): Boolean {
                if (down == null) {
                    return false
                }
                val dy: Float = me.getY() - down.getY()
                if (deltaY > 0 && dy < 0) {
                    return showMoreSuggestions()
                }
                return false
            }
        }

    /**
     * Construct a [SuggestionStripView] for showing suggestions to be picked by the user.
     * @param context
     * @param attrs
     */
    init {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.suggestions_strip, this)

        mSuggestionsStrip = findViewById<View>(R.id.suggestions_strip) as ViewGroup
        mVoiceKey = findViewById<View>(R.id.suggestions_strip_voice_key) as ImageButton
        mImportantNoticeStrip = findViewById(R.id.important_notice_strip)
        mStripVisibilityGroup = StripVisibilityGroup(
            this, mSuggestionsStrip,
            mImportantNoticeStrip
        )

        for (pos in 0 until SuggestedWords.MAX_SUGGESTIONS) {
            val word: TextView = TextView(context, null, R.attr.suggestionWordStyle)
            word.setContentDescription(getResources().getString(R.string.spoken_empty_suggestion))
            word.setOnClickListener(this)
            word.setOnLongClickListener(this)
            mWordViews.add(word)
            val divider: View = inflater.inflate(R.layout.suggestion_divider, null)
            mDividerViews.add(divider)
            val info: TextView = TextView(context, null, R.attr.suggestionWordStyle)
            info.setTextColor(Color.WHITE)
            info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEBUG_INFO_TEXT_SIZE_IN_DIP)
            mDebugInfoViews.add(info)
        }

        mLayoutHelper = SuggestionStripLayoutHelper(
            context, attrs, defStyle, mWordViews, mDividerViews, mDebugInfoViews
        )

        mMoreSuggestionsContainer = inflater.inflate(R.layout.more_suggestions, null)
        mMoreSuggestionsView = mMoreSuggestionsContainer
            .findViewById<View>(R.id.more_suggestions_view) as MoreSuggestionsView
        mMoreSuggestionsBuilder = MoreSuggestions.Builder(context, mMoreSuggestionsView)

        val res: Resources = context.getResources()
        mMoreSuggestionsModalTolerance = res.getDimensionPixelOffset(
            R.dimen.config_more_suggestions_modal_tolerance
        )
        mMoreSuggestionsSlidingDetector = GestureDetector(
            context, mMoreSuggestionsSlidingListener
        )

        val keyboardAttr: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.Keyboard, defStyle, R.style.SuggestionStripView
        )
        val iconVoice: Drawable? = keyboardAttr.getDrawable(R.styleable.Keyboard_iconShortcutKey)
        keyboardAttr.recycle()
        mVoiceKey.setImageDrawable(iconVoice)
        mVoiceKey.setOnClickListener(this)
    }

    override fun onInterceptTouchEvent(me: MotionEvent): Boolean {
        if (mStripVisibilityGroup.isShowingImportantNoticeStrip) {
            return false
        }
        // Detecting sliding up finger to show {@link MoreSuggestionsView}.
        if (!mMoreSuggestionsView.isShowingInParent) {
            mLastX = me.getX().toInt()
            mLastY = me.getY().toInt()
            return mMoreSuggestionsSlidingDetector.onTouchEvent(me)
        }
        if (mMoreSuggestionsView.isInModalMode) {
            return false
        }

        val action: Int = me.getAction()
        val index: Int = me.getActionIndex()
        val x: Int = me.getX(index).toInt()
        val y: Int = me.getY(index).toInt()
        if (abs((x - mOriginX).toDouble()) >= mMoreSuggestionsModalTolerance
            || mOriginY - y >= mMoreSuggestionsModalTolerance
        ) {
            // Decided to be in the sliding suggestion mode only when the touch point has been moved
            // upward. Further {@link MotionEvent}s will be delivered to
            // {@link #onTouchEvent(MotionEvent)}.
            mNeedsToTransformTouchEventToHoverEvent =
                AccessibilityUtils.instance.isTouchExplorationEnabled()
            mIsDispatchingHoverEventToMoreSuggestions = false
            return true
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            // Decided to be in the modal input mode.
            mMoreSuggestionsView.setModalMode()
        }
        return false
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        // Don't populate accessibility event with suggested words and voice key.
        return true
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (!mMoreSuggestionsView.isShowingInParent) {
            // Ignore any touch event while more suggestions panel hasn't been shown.
            // Detecting sliding up is done at {@link #onInterceptTouchEvent}.
            return true
        }
        // In the sliding input mode. {@link MotionEvent} should be forwarded to
        // {@link MoreSuggestionsView}.
        val index: Int = me.getActionIndex()
        val x: Int = mMoreSuggestionsView.translateX(me.getX(index).toInt())
        val y: Int = mMoreSuggestionsView.translateY(me.getY(index).toInt())
        me.setLocation(x.toFloat(), y.toFloat())
        if (!mNeedsToTransformTouchEventToHoverEvent) {
            mMoreSuggestionsView.onTouchEvent(me)
            return true
        }
        // In sliding suggestion mode with accessibility mode on, a touch event should be
        // transformed to a hover event.
        val width: Int = mMoreSuggestionsView.getWidth()
        val height: Int = mMoreSuggestionsView.getHeight()
        val onMoreSuggestions: Boolean = (x >= 0 && x < width && y >= 0 && y < height)
        if (!onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Just drop this touch event because dispatching hover event isn't started yet and
            // the touch event isn't on {@link MoreSuggestionsView}.
            return true
        }
        val hoverAction: Int
        if (onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Transform this touch event to a hover enter event and start dispatching a hover
            // event to {@link MoreSuggestionsView}.
            mIsDispatchingHoverEventToMoreSuggestions = true
            hoverAction = MotionEvent.ACTION_HOVER_ENTER
        } else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
            // Transform this touch event to a hover exit event and stop dispatching a hover event
            // after this.
            mIsDispatchingHoverEventToMoreSuggestions = false
            mNeedsToTransformTouchEventToHoverEvent = false
            hoverAction = MotionEvent.ACTION_HOVER_EXIT
        } else {
            // Transform this touch event to a hover move event.
            hoverAction = MotionEvent.ACTION_HOVER_MOVE
        }
        me.setAction(hoverAction)
        mMoreSuggestionsView.onHoverEvent(me)
        return true
    }

    override fun onClick(view: View) {
        AudioAndHapticFeedbackManager.instance.performHapticAndAudioFeedback(
            Constants.CODE_UNSPECIFIED, this
        )
        if (view === mImportantNoticeStrip) {
            mListener!!.showImportantNoticeContents()
            return
        }
        if (view === mVoiceKey) {
            mListener!!.onCodeInput(
                Constants.CODE_SHORTCUT,
                Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                false /* isKeyRepeat */
            )
            return
        }

        val tag: Any = view.getTag()
        // {@link Integer} tag is set at
        // {@link SuggestionStripLayoutHelper#setupWordViewsTextAndColor(SuggestedWords,int)} and
        // {@link SuggestionStripLayoutHelper#layoutPunctuationSuggestions(SuggestedWords,ViewGroup}
        if (tag is Int) {
            val index: Int = tag
            if (index >= mSuggestedWords.size()) {
                return
            }
            val wordInfo: SuggestedWordInfo? = mSuggestedWords.getInfo(index)
            mListener!!.pickSuggestionManually(wordInfo!!)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismissMoreSuggestionsPanel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Called by the framework when the size is known. Show the important notice if applicable.
        // This may be overriden by showing suggestions later, if applicable.
        if (oldw <= 0 && w > 0) {
            maybeShowImportantNoticeTitle()
        }
    }

    companion object {
        val DBG: Boolean = DebugFlags.DEBUG_ENABLED
        private const val DEBUG_INFO_TEXT_SIZE_IN_DIP: Float = 6.0f
    }
}
