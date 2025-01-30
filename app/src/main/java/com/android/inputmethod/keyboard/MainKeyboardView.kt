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
package com.android.inputmethod.keyboard

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.accessibility.MainKeyboardAccessibilityDelegate
import com.android.inputmethod.annotations.ExternallyReferenced
import com.android.inputmethod.keyboard.MainKeyboardView
import com.android.inputmethod.keyboard.internal.DrawingPreviewPlacerView
import com.android.inputmethod.keyboard.internal.DrawingProxy
import com.android.inputmethod.keyboard.internal.GestureFloatingTextDrawingPreview
import com.android.inputmethod.keyboard.internal.GestureTrailsDrawingPreview
import com.android.inputmethod.keyboard.internal.KeyDrawParams
import com.android.inputmethod.keyboard.internal.KeyPreviewChoreographer
import com.android.inputmethod.keyboard.internal.KeyPreviewDrawParams
import com.android.inputmethod.keyboard.internal.KeyPreviewView
import com.android.inputmethod.keyboard.internal.MoreKeySpec
import com.android.inputmethod.keyboard.internal.NonDistinctMultitouchHelper
import com.android.inputmethod.keyboard.internal.SlidingKeyInputDrawingPreview
import com.android.inputmethod.keyboard.internal.TimerHandler
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.RichInputMethodSubtype
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.CoordinateUtils
import com.android.inputmethod.latin.settings.DebugSettings
import com.android.inputmethod.latin.utils.LanguageOnSpacebarUtils
import com.android.inputmethod.latin.utils.TypefaceUtils
import java.util.WeakHashMap

/**
 * A view that is responsible for detecting key presses and touch movements.
 *
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarTextRatio
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarTextColor
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarTextShadowRadius
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarTextShadowColor
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarFinalAlpha
 * @attr ref android.R.styleable#MainKeyboardView_languageOnSpacebarFadeoutAnimator
 * @attr ref android.R.styleable#MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator
 * @attr ref android.R.styleable#MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator
 * @attr ref android.R.styleable#MainKeyboardView_keyHysteresisDistance
 * @attr ref android.R.styleable#MainKeyboardView_touchNoiseThresholdTime
 * @attr ref android.R.styleable#MainKeyboardView_touchNoiseThresholdDistance
 * @attr ref android.R.styleable#MainKeyboardView_keySelectionByDraggingFinger
 * @attr ref android.R.styleable#MainKeyboardView_keyRepeatStartTimeout
 * @attr ref android.R.styleable#MainKeyboardView_keyRepeatInterval
 * @attr ref android.R.styleable#MainKeyboardView_longPressKeyTimeout
 * @attr ref android.R.styleable#MainKeyboardView_longPressShiftKeyTimeout
 * @attr ref android.R.styleable#MainKeyboardView_ignoreAltCodeKeyTimeout
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewLayout
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewOffset
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewHeight
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewLingerTimeout
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewShowUpAnimator
 * @attr ref android.R.styleable#MainKeyboardView_keyPreviewDismissAnimator
 * @attr ref android.R.styleable#MainKeyboardView_moreKeysKeyboardLayout
 * @attr ref android.R.styleable#MainKeyboardView_moreKeysKeyboardForActionLayout
 * @attr ref android.R.styleable#MainKeyboardView_backgroundDimAlpha
 * @attr ref android.R.styleable#MainKeyboardView_showMoreKeysKeyboardAtTouchPoint
 * @attr ref android.R.styleable#MainKeyboardView_gestureFloatingPreviewTextLingerTimeout
 * @attr ref android.R.styleable#MainKeyboardView_gestureStaticTimeThresholdAfterFastTyping
 * @attr ref android.R.styleable#MainKeyboardView_gestureDetectFastMoveSpeedThreshold
 * @attr ref android.R.styleable#MainKeyboardView_gestureDynamicThresholdDecayDuration
 * @attr ref android.R.styleable#MainKeyboardView_gestureDynamicTimeThresholdFrom
 * @attr ref android.R.styleable#MainKeyboardView_gestureDynamicTimeThresholdTo
 * @attr ref android.R.styleable#MainKeyboardView_gestureDynamicDistanceThresholdFrom
 * @attr ref android.R.styleable#MainKeyboardView_gestureDynamicDistanceThresholdTo
 * @attr ref android.R.styleable#MainKeyboardView_gestureSamplingMinimumDistance
 * @attr ref android.R.styleable#MainKeyboardView_gestureRecognitionMinimumTime
 * @attr ref android.R.styleable#MainKeyboardView_gestureRecognitionSpeedThreshold
 * @attr ref android.R.styleable#MainKeyboardView_suppressKeyPreviewAfterBatchInputDuration
 */
class MainKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = R.attr.mainKeyboardViewStyle
) :
    KeyboardView(context, attrs, defStyle), DrawingProxy, MoreKeysPanel.Controller {
    /** Listener for [KeyboardActionListener].  */
    private var mKeyboardActionListener: KeyboardActionListener?

    /* Space key and its icon and background. */
    private var mSpaceKey: Key? = null

    // Stuff to draw language name on spacebar.
    private val mLanguageOnSpacebarFinalAlpha: Int
    private val mLanguageOnSpacebarFadeoutAnimator: ObjectAnimator?
    private var mLanguageOnSpacebarFormatType: Int = 0
    private var mHasMultipleEnabledIMEsOrSubtypes: Boolean = false
    private var mLanguageOnSpacebarAnimAlpha: Int = Constants.Color.ALPHA_OPAQUE
    private val mLanguageOnSpacebarTextRatio: Float
    private var mLanguageOnSpacebarTextSize: Float = 0f
    private val mLanguageOnSpacebarTextColor: Int
    private val mLanguageOnSpacebarTextShadowRadius: Float
    private val mLanguageOnSpacebarTextShadowColor: Int

    // Stuff to draw altCodeWhileTyping keys.
    private val mAltCodeKeyWhileTypingFadeoutAnimator: ObjectAnimator?
    private val mAltCodeKeyWhileTypingFadeinAnimator: ObjectAnimator?
    private var mAltCodeKeyWhileTypingAnimAlpha: Int = Constants.Color.ALPHA_OPAQUE

    // Drawing preview placer view
    private val mDrawingPreviewPlacerView: DrawingPreviewPlacerView
    private val mOriginCoords: IntArray = CoordinateUtils.newInstance()
    private val mGestureFloatingTextDrawingPreview: GestureFloatingTextDrawingPreview
    private val mGestureTrailsDrawingPreview: GestureTrailsDrawingPreview
    private val mSlidingKeyInputDrawingPreview: SlidingKeyInputDrawingPreview

    // Key preview
    private val mKeyPreviewDrawParams: KeyPreviewDrawParams
    private val mKeyPreviewChoreographer: KeyPreviewChoreographer

    // More keys keyboard
    private val mBackgroundDimAlphaPaint: Paint = Paint()
    private val mMoreKeysKeyboardContainer: View
    private val mMoreKeysKeyboardForActionContainer: View
    private val mMoreKeysKeyboardCache: WeakHashMap<Key, Keyboard?> = WeakHashMap()
    private val mConfigShowMoreKeysKeyboardAtTouchedPoint: Boolean

    // More keys panel (used by both more keys keyboard and more suggestions view)
    // TODO: Consider extending to support multiple more keys panels
    private var mMoreKeysPanel: MoreKeysPanel? = null

    // Gesture floating preview text
    // TODO: Make this parameter customizable by user via settings.
    private val mGestureFloatingPreviewTextLingerTimeout: Int

    private val mKeyDetector: KeyDetector
    private val mNonDistinctMultitouchHelper: NonDistinctMultitouchHelper?

    private val mTimerHandler: TimerHandler
    private val mLanguageOnSpacebarHorizontalMargin: Int

    private var mAccessibilityDelegate: MainKeyboardAccessibilityDelegate? = null

    override var keyboard: Keyboard?
        get() = super.keyboard
        set(keyboard) {
            // Remove any pending messages, except dismissing preview and key repeat.
            mTimerHandler.cancelLongPressTimers()
            super.keyboard = keyboard
            mKeyDetector.setKeyboard(
                keyboard!!, -getPaddingLeft().toFloat(), -getPaddingTop() + verticalCorrection
            )
            PointerTracker.setKeyDetector(mKeyDetector)
            mMoreKeysKeyboardCache.clear()

            mSpaceKey = keyboard.getKey(Constants.CODE_SPACE)
            val keyHeight: Int = keyboard.mMostCommonKeyHeight - keyboard.mVerticalGap
            mLanguageOnSpacebarTextSize = keyHeight * mLanguageOnSpacebarTextRatio

            if (AccessibilityUtils.instance.isAccessibilityEnabled()) {
                if (mAccessibilityDelegate == null) {
                    mAccessibilityDelegate = MainKeyboardAccessibilityDelegate(this, mKeyDetector)
                }
                mAccessibilityDelegate!!.setKeyboard(keyboard)
            } else {
                mAccessibilityDelegate = null
            }
        }

    init {
        val drawingPreviewPlacerView: DrawingPreviewPlacerView =
            DrawingPreviewPlacerView(context, attrs)

        val mainKeyboardViewAttr: TypedArray = context.obtainStyledAttributes(
            attrs, R.styleable.MainKeyboardView, defStyle, R.style.MainKeyboardView
        )
        val ignoreAltCodeKeyTimeout: Int = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_ignoreAltCodeKeyTimeout, 0
        )
        val gestureRecognitionUpdateTime: Int = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_gestureRecognitionUpdateTime, 0
        )
        mTimerHandler = TimerHandler(
            this, ignoreAltCodeKeyTimeout, gestureRecognitionUpdateTime
        )

        val keyHysteresisDistance: Float = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_keyHysteresisDistance, 0.0f
        )
        val keyHysteresisDistanceForSlidingModifier: Float = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_keyHysteresisDistanceForSlidingModifier, 0.0f
        )
        mKeyDetector = KeyDetector(
            keyHysteresisDistance, keyHysteresisDistanceForSlidingModifier
        )

        PointerTracker.init(mainKeyboardViewAttr, mTimerHandler, this /* DrawingProxy */)

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val forceNonDistinctMultitouch: Boolean = prefs.getBoolean(
            DebugSettings.PREF_FORCE_NON_DISTINCT_MULTITOUCH, false
        )
        val hasDistinctMultitouch: Boolean = context.getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
                && !forceNonDistinctMultitouch
        mNonDistinctMultitouchHelper = if (hasDistinctMultitouch)
            null
        else
            NonDistinctMultitouchHelper()

        val backgroundDimAlpha: Int = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_backgroundDimAlpha, 0
        )
        mBackgroundDimAlphaPaint.setColor(Color.BLACK)
        mBackgroundDimAlphaPaint.setAlpha(backgroundDimAlpha)
        mLanguageOnSpacebarTextRatio = mainKeyboardViewAttr.getFraction(
            R.styleable.MainKeyboardView_languageOnSpacebarTextRatio, 1, 1, 1.0f
        )
        mLanguageOnSpacebarTextColor = mainKeyboardViewAttr.getColor(
            R.styleable.MainKeyboardView_languageOnSpacebarTextColor, 0
        )
        mLanguageOnSpacebarTextShadowRadius = mainKeyboardViewAttr.getFloat(
            R.styleable.MainKeyboardView_languageOnSpacebarTextShadowRadius,
            LANGUAGE_ON_SPACEBAR_TEXT_SHADOW_RADIUS_DISABLED
        )
        mLanguageOnSpacebarTextShadowColor = mainKeyboardViewAttr.getColor(
            R.styleable.MainKeyboardView_languageOnSpacebarTextShadowColor, 0
        )
        mLanguageOnSpacebarFinalAlpha = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_languageOnSpacebarFinalAlpha,
            Constants.Color.ALPHA_OPAQUE
        )
        val languageOnSpacebarFadeoutAnimatorResId: Int = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_languageOnSpacebarFadeoutAnimator, 0
        )
        val altCodeKeyWhileTypingFadeoutAnimatorResId: Int = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator, 0
        )
        val altCodeKeyWhileTypingFadeinAnimatorResId: Int = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator, 0
        )

        mKeyPreviewDrawParams = KeyPreviewDrawParams(mainKeyboardViewAttr)
        mKeyPreviewChoreographer = KeyPreviewChoreographer(mKeyPreviewDrawParams)

        val moreKeysKeyboardLayoutId: Int = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_moreKeysKeyboardLayout, 0
        )
        val moreKeysKeyboardForActionLayoutId: Int = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_moreKeysKeyboardForActionLayout,
            moreKeysKeyboardLayoutId
        )
        mConfigShowMoreKeysKeyboardAtTouchedPoint = mainKeyboardViewAttr.getBoolean(
            R.styleable.MainKeyboardView_showMoreKeysKeyboardAtTouchedPoint, false
        )

        mGestureFloatingPreviewTextLingerTimeout = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_gestureFloatingPreviewTextLingerTimeout, 0
        )

        mGestureFloatingTextDrawingPreview = GestureFloatingTextDrawingPreview(
            mainKeyboardViewAttr
        )
        mGestureFloatingTextDrawingPreview.setDrawingView(drawingPreviewPlacerView)

        mGestureTrailsDrawingPreview = GestureTrailsDrawingPreview(mainKeyboardViewAttr)
        mGestureTrailsDrawingPreview.setDrawingView(drawingPreviewPlacerView)

        mSlidingKeyInputDrawingPreview = SlidingKeyInputDrawingPreview(mainKeyboardViewAttr)
        mSlidingKeyInputDrawingPreview.setDrawingView(drawingPreviewPlacerView)
        mainKeyboardViewAttr.recycle()

        mDrawingPreviewPlacerView = drawingPreviewPlacerView

        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        mMoreKeysKeyboardContainer = inflater.inflate(moreKeysKeyboardLayoutId, null)
        mMoreKeysKeyboardForActionContainer = inflater.inflate(
            moreKeysKeyboardForActionLayoutId, null
        )
        mLanguageOnSpacebarFadeoutAnimator = loadObjectAnimator(
            languageOnSpacebarFadeoutAnimatorResId, this
        )
        mAltCodeKeyWhileTypingFadeoutAnimator = loadObjectAnimator(
            altCodeKeyWhileTypingFadeoutAnimatorResId, this
        )
        mAltCodeKeyWhileTypingFadeinAnimator = loadObjectAnimator(
            altCodeKeyWhileTypingFadeinAnimatorResId, this
        )

        mKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER

        mLanguageOnSpacebarHorizontalMargin = getResources().getDimension(
            R.dimen.config_language_on_spacebar_horizontal_margin
        ).toInt()
    }

    override fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        super.setHardwareAcceleratedDrawingEnabled(enabled)
        mDrawingPreviewPlacerView.setHardwareAcceleratedDrawingEnabled(enabled)
    }

    private fun loadObjectAnimator(resId: Int, target: Any): ObjectAnimator? {
        if (resId == 0) {
            // TODO: Stop returning null.
            return null
        }
        val animator: ObjectAnimator? = AnimatorInflater.loadAnimator(
            getContext(), resId
        ) as ObjectAnimator?
        if (animator != null) {
            animator.setTarget(target)
        }
        return animator
    }

    // Implements {@link DrawingProxy#startWhileTypingAnimation(int)}.
    /**
     * Called when a while-typing-animation should be started.
     * @param fadeInOrOut [DrawingProxy.FADE_IN] starts while-typing-fade-in animation.
     * [DrawingProxy.FADE_OUT] starts while-typing-fade-out animation.
     */
    override fun startWhileTypingAnimation(fadeInOrOut: Int) {
        when (fadeInOrOut) {
            DrawingProxy.FADE_IN -> cancelAndStartAnimators(
                mAltCodeKeyWhileTypingFadeoutAnimator, mAltCodeKeyWhileTypingFadeinAnimator
            )

            DrawingProxy.FADE_OUT -> cancelAndStartAnimators(
                mAltCodeKeyWhileTypingFadeinAnimator, mAltCodeKeyWhileTypingFadeoutAnimator
            )
        }
    }

    @ExternallyReferenced
    fun getLanguageOnSpacebarAnimAlpha(): Int {
        return mLanguageOnSpacebarAnimAlpha
    }

    @ExternallyReferenced
    fun setLanguageOnSpacebarAnimAlpha(alpha: Int) {
        mLanguageOnSpacebarAnimAlpha = alpha
        invalidateKey(mSpaceKey)
    }

    @ExternallyReferenced
    fun getAltCodeKeyWhileTypingAnimAlpha(): Int {
        return mAltCodeKeyWhileTypingAnimAlpha
    }

    @ExternallyReferenced
    fun setAltCodeKeyWhileTypingAnimAlpha(alpha: Int) {
        if (mAltCodeKeyWhileTypingAnimAlpha == alpha) {
            return
        }
        // Update the visual of alt-code-key-while-typing.
        mAltCodeKeyWhileTypingAnimAlpha = alpha
        val keyboard: Keyboard = this.keyboard ?: return
        for (key: Key? in keyboard.mAltCodeKeysWhileTyping) {
            invalidateKey(key)
        }
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener?) {
        mKeyboardActionListener = listener
        PointerTracker.setKeyboardActionListener(listener)
    }

    // TODO: We should reconsider which coordinate system should be used to represent keyboard
    // event.
    fun getKeyX(x: Int): Int {
        return if (Constants.isValidCoordinate(x)) mKeyDetector.getTouchX(x) else x
    }

    // TODO: We should reconsider which coordinate system should be used to represent keyboard
    // event.
    fun getKeyY(y: Int): Int {
        return if (Constants.isValidCoordinate(y)) mKeyDetector.getTouchY(y) else y
    }

    /**
     * Enables or disables the key preview popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     * @param previewEnabled whether or not to enable the key feedback preview
     * @param delay the delay after which the preview is dismissed
     */
    fun setKeyPreviewPopupEnabled(previewEnabled: Boolean, delay: Int) {
        mKeyPreviewDrawParams.setPopupEnabled(previewEnabled, delay)
    }

    /**
     * Enables or disables the key preview popup animations and set animations' parameters.
     *
     * @param hasCustomAnimationParams false to use the default key preview popup animations
     * specified by keyPreviewShowUpAnimator and keyPreviewDismissAnimator attributes.
     * true to override the default animations with the specified parameters.
     * @param showUpStartXScale from this x-scale the show up animation will start.
     * @param showUpStartYScale from this y-scale the show up animation will start.
     * @param showUpDuration the duration of the show up animation in milliseconds.
     * @param dismissEndXScale to this x-scale the dismiss animation will end.
     * @param dismissEndYScale to this y-scale the dismiss animation will end.
     * @param dismissDuration the duration of the dismiss animation in milliseconds.
     */
    fun setKeyPreviewAnimationParams(
        hasCustomAnimationParams: Boolean,
        showUpStartXScale: Float, showUpStartYScale: Float, showUpDuration: Int,
        dismissEndXScale: Float, dismissEndYScale: Float, dismissDuration: Int
    ) {
        mKeyPreviewDrawParams.setAnimationParams(
            hasCustomAnimationParams,
            showUpStartXScale, showUpStartYScale, showUpDuration,
            dismissEndXScale, dismissEndYScale, dismissDuration
        )
    }

    private fun locatePreviewPlacerView() {
        getLocationInWindow(mOriginCoords)
        mDrawingPreviewPlacerView.setKeyboardViewGeometry(mOriginCoords, getWidth(), getHeight())
    }

    private fun installPreviewPlacerView() {
        val rootView: View? = getRootView()
        if (rootView == null) {
            Log.w(TAG, "Cannot find root view")
            return
        }
        val windowContentView: ViewGroup? =
            rootView.findViewById<View>(android.R.id.content) as ViewGroup?
        // Note: It'd be very weird if we get null by android.R.id.content.
        if (windowContentView == null) {
            Log.w(TAG, "Cannot find android.R.id.content view to add DrawingPreviewPlacerView")
            return
        }
        windowContentView.addView(mDrawingPreviewPlacerView)
    }

    // Implements {@link DrawingProxy#onKeyPressed(Key,boolean)}.
    override fun onKeyPressed(key: Key, withPreview: Boolean) {
        key.onPressed()
        invalidateKey(key)
        if (withPreview && !key.noKeyPreview()) {
            showKeyPreview(key)
        }
    }

    private fun showKeyPreview(key: Key) {
        val keyboard: Keyboard = this.keyboard ?: return
        val previewParams: KeyPreviewDrawParams = mKeyPreviewDrawParams
        if (!previewParams.isPopupEnabled()) {
            previewParams.setVisibleOffset(-keyboard.mVerticalGap)
            return
        }

        locatePreviewPlacerView()
        getLocationInWindow(mOriginCoords)
        mKeyPreviewChoreographer.placeAndShowKeyPreview(
            key, keyboard.mIconsSet, keyDrawParams,
            getWidth(), mOriginCoords, mDrawingPreviewPlacerView, isHardwareAccelerated()
        )
    }

    private fun dismissKeyPreviewWithoutDelay(key: Key) {
        mKeyPreviewChoreographer.dismissKeyPreview(key, false /* withAnimation */)
        invalidateKey(key)
    }

    // Implements {@link DrawingProxy#onKeyReleased(Key,boolean)}.
    override fun onKeyReleased(key: Key, withAnimation: Boolean) {
        key.onReleased()
        invalidateKey(key)
        if (!key.noKeyPreview()) {
            if (withAnimation) {
                dismissKeyPreview(key)
            } else {
                dismissKeyPreviewWithoutDelay(key)
            }
        }
    }

    private fun dismissKeyPreview(key: Key) {
        if (isHardwareAccelerated()) {
            mKeyPreviewChoreographer.dismissKeyPreview(key, true /* withAnimation */)
            return
        }
        // TODO: Implement preference option to control key preview method and duration.
        mTimerHandler.postDismissKeyPreview(key, mKeyPreviewDrawParams.getLingerTimeout().toLong())
    }

    fun setSlidingKeyInputPreviewEnabled(enabled: Boolean) {
        mSlidingKeyInputDrawingPreview.setPreviewEnabled(enabled)
    }

    override fun showSlidingKeyInputPreview(tracker: PointerTracker?) {
        locatePreviewPlacerView()
        if (tracker != null) {
            mSlidingKeyInputDrawingPreview.setPreviewPosition(tracker)
        } else {
            mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview()
        }
    }

    private fun setGesturePreviewMode(
        isGestureTrailEnabled: Boolean,
        isGestureFloatingPreviewTextEnabled: Boolean
    ) {
        mGestureFloatingTextDrawingPreview.setPreviewEnabled(isGestureFloatingPreviewTextEnabled)
        mGestureTrailsDrawingPreview.setPreviewEnabled(isGestureTrailEnabled)
    }

    fun showGestureFloatingPreviewText(
        suggestedWords: SuggestedWords,
        dismissDelayed: Boolean
    ) {
        locatePreviewPlacerView()
        val gestureFloatingTextDrawingPreview: GestureFloatingTextDrawingPreview =
            mGestureFloatingTextDrawingPreview
        gestureFloatingTextDrawingPreview.setSuggetedWords(suggestedWords)
        if (dismissDelayed) {
            mTimerHandler.postDismissGestureFloatingPreviewText(
                mGestureFloatingPreviewTextLingerTimeout.toLong()
            )
        }
    }

    // Implements {@link DrawingProxy#dismissGestureFloatingPreviewTextWithoutDelay()}.
    override fun dismissGestureFloatingPreviewTextWithoutDelay() {
        mGestureFloatingTextDrawingPreview.dismissGestureFloatingPreviewText()
    }

    override fun showGestureTrail(
        tracker: PointerTracker,
        showsFloatingPreviewText: Boolean
    ) {
        locatePreviewPlacerView()
        if (showsFloatingPreviewText) {
            mGestureFloatingTextDrawingPreview.setPreviewPosition(tracker)
        }
        mGestureTrailsDrawingPreview.setPreviewPosition(tracker)
    }

    // Note that this method is called from a non-UI thread.
    fun setMainDictionaryAvailability(mainDictionaryAvailable: Boolean) {
        PointerTracker.setMainDictionaryAvailability(mainDictionaryAvailable)
    }

    fun setGestureHandlingEnabledByUser(
        isGestureHandlingEnabledByUser: Boolean,
        isGestureTrailEnabled: Boolean,
        isGestureFloatingPreviewTextEnabled: Boolean
    ) {
        PointerTracker.setGestureHandlingEnabledByUser(isGestureHandlingEnabledByUser)
        setGesturePreviewMode(
            isGestureHandlingEnabledByUser && isGestureTrailEnabled,
            isGestureHandlingEnabledByUser && isGestureFloatingPreviewTextEnabled
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        installPreviewPlacerView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDrawingPreviewPlacerView.removeAllViews()
    }

    // Implements {@link DrawingProxy@showMoreKeysKeyboard(Key,PointerTracker)}.
    override fun showMoreKeysKeyboard(
        key: Key,
        tracker: PointerTracker
    ): MoreKeysPanel {
        val moreKeys: Array<MoreKeySpec> = key.moreKeys
        var moreKeysKeyboard: Keyboard? = mMoreKeysKeyboardCache.get(key)
        if (moreKeysKeyboard == null) {
            // {@link KeyPreviewDrawParams#mPreviewVisibleWidth} should have been set at
            // {@link KeyPreviewChoreographer#placeKeyPreview(Key,TextView,KeyboardIconsSet,KeyDrawParams,int,int[]},
            // though there may be some chances that the value is zero. <code>width == 0</code>
            // will cause zero-division error at
            // {@link MoreKeysKeyboardParams#setParameters(int,int,int,int,int,int,boolean,int)}.
            val isSingleMoreKeyWithPreview: Boolean = mKeyPreviewDrawParams.isPopupEnabled()
                    && !key.noKeyPreview() && moreKeys.size == 1 && mKeyPreviewDrawParams.getVisibleWidth() > 0
            val builder: MoreKeysKeyboard.Builder = MoreKeysKeyboard.Builder(
                context, key, keyboard!!, isSingleMoreKeyWithPreview,
                mKeyPreviewDrawParams.getVisibleWidth(),
                mKeyPreviewDrawParams.getVisibleHeight(), newLabelPaint(key)
            )
            moreKeysKeyboard = builder.build()
            mMoreKeysKeyboardCache.put(key, moreKeysKeyboard)
        }

        val container: View = if (key.isActionKey)
            mMoreKeysKeyboardForActionContainer
        else
            mMoreKeysKeyboardContainer
        val moreKeysKeyboardView: MoreKeysKeyboardView =
            container.findViewById<View>(R.id.more_keys_keyboard_view) as MoreKeysKeyboardView
        moreKeysKeyboardView.keyboard = moreKeysKeyboard
        container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val lastCoords: IntArray = CoordinateUtils.newInstance()
        tracker.getLastCoordinates(lastCoords)
        val keyPreviewEnabled: Boolean = mKeyPreviewDrawParams.isPopupEnabled()
                && !key.noKeyPreview()
        // The more keys keyboard is usually horizontally aligned with the center of the parent key.
        // If showMoreKeysKeyboardAtTouchedPoint is true and the key preview is disabled, the more
        // keys keyboard is placed at the touch point of the parent key.
        val pointX: Int = if ((mConfigShowMoreKeysKeyboardAtTouchedPoint && !keyPreviewEnabled))
            CoordinateUtils.x(lastCoords)
        else
            key.x + key.width / 2
        // The more keys keyboard is usually vertically aligned with the top edge of the parent key
        // (plus vertical gap). If the key preview is enabled, the more keys keyboard is vertically
        // aligned with the bottom edge of the visible part of the key preview.
        // {@code mPreviewVisibleOffset} has been set appropriately in
        // {@link KeyboardView#showKeyPreview(PointerTracker)}.
        val pointY: Int = key.y + mKeyPreviewDrawParams.getVisibleOffset()
        moreKeysKeyboardView.showMoreKeysPanel(this, this, pointX, pointY, mKeyboardActionListener)
        return moreKeysKeyboardView
    }

    fun isInDraggingFinger(): Boolean {
        if (isShowingMoreKeysPanel()) {
            return true
        }
        return PointerTracker.isAnyInDraggingFinger()
    }

    override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {
        locatePreviewPlacerView()
        // Dismiss another {@link MoreKeysPanel} that may be being showed.
        onDismissMoreKeysPanel()
        // Dismiss all key previews that may be being showed.
        PointerTracker.setReleasedKeyGraphicsToAllKeys()
        // Dismiss sliding key input preview that may be being showed.
        mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview()
        panel.showInParent(mDrawingPreviewPlacerView)
        mMoreKeysPanel = panel
    }

    fun isShowingMoreKeysPanel(): Boolean {
        return mMoreKeysPanel != null && mMoreKeysPanel!!.isShowingInParent
    }

    override fun onCancelMoreKeysPanel() {
        PointerTracker.dismissAllMoreKeysPanels()
    }

    override fun onDismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel!!.removeFromParent()
            mMoreKeysPanel = null
        }
    }

    fun startDoubleTapShiftKeyTimer() {
        mTimerHandler.startDoubleTapShiftKeyTimer()
    }

    fun cancelDoubleTapShiftKeyTimer() {
        mTimerHandler.cancelDoubleTapShiftKeyTimer()
    }

    fun isInDoubleTapShiftKeyTimeout(): Boolean {
        return mTimerHandler.isInDoubleTapShiftKeyTimeout()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (keyboard == null) {
            return false
        }
        if (mNonDistinctMultitouchHelper != null) {
            if (event.getPointerCount() > 1 && mTimerHandler.isInKeyRepeat()) {
                // Key repeating timer will be canceled if 2 or more keys are in action.
                mTimerHandler.cancelKeyRepeatTimers()
            }
            // Non distinct multitouch screen support
            mNonDistinctMultitouchHelper.processMotionEvent(event, mKeyDetector)
            return true
        }
        return processMotionEvent(event)
    }

    fun processMotionEvent(event: MotionEvent): Boolean {
        val index: Int = event.getActionIndex()
        val id: Int = event.getPointerId(index)
        val tracker: PointerTracker = PointerTracker.getPointerTracker(id)
        // When a more keys panel is showing, we should ignore other fingers' single touch events
        // other than the finger that is showing the more keys panel.
        if (isShowingMoreKeysPanel() && !tracker.isShowingMoreKeysPanel()
            && PointerTracker.getActivePointerTrackerCount() == 1
        ) {
            return true
        }
        tracker.processMotionEvent(event, mKeyDetector)
        return true
    }

    fun cancelAllOngoingEvents() {
        mTimerHandler.cancelAllMessages()
        PointerTracker.setReleasedKeyGraphicsToAllKeys()
        mGestureFloatingTextDrawingPreview.dismissGestureFloatingPreviewText()
        mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview()
        PointerTracker.dismissAllMoreKeysPanels()
        PointerTracker.cancelAllPointerTrackers()
    }

    fun closing() {
        cancelAllOngoingEvents()
        mMoreKeysKeyboardCache.clear()
    }

    fun onHideWindow() {
        onDismissMoreKeysPanel()
        val accessibilityDelegate: MainKeyboardAccessibilityDelegate? = mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isAccessibilityEnabled()
        ) {
            accessibilityDelegate.onHideWindow()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onHoverEvent(event: MotionEvent): Boolean {
        val accessibilityDelegate: MainKeyboardAccessibilityDelegate? = mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isTouchExplorationEnabled()
        ) {
            return accessibilityDelegate.onHoverEvent(event)
        }
        return super.onHoverEvent(event)
    }

    fun updateShortcutKey(available: Boolean) {
        val keyboard: Keyboard = this.keyboard ?: return
        val shortcutKey: Key = keyboard.getKey(Constants.CODE_SHORTCUT) ?: return
        shortcutKey.isEnabled = available
        invalidateKey(shortcutKey)
    }

    fun startDisplayLanguageOnSpacebar(
        subtypeChanged: Boolean,
        languageOnSpacebarFormatType: Int,
        hasMultipleEnabledIMEsOrSubtypes: Boolean
    ) {
        if (subtypeChanged) {
            KeyPreviewView.clearTextCache()
        }
        mLanguageOnSpacebarFormatType = languageOnSpacebarFormatType
        mHasMultipleEnabledIMEsOrSubtypes = hasMultipleEnabledIMEsOrSubtypes
        val animator: ObjectAnimator? = mLanguageOnSpacebarFadeoutAnimator
        if (animator == null) {
            mLanguageOnSpacebarFormatType = LanguageOnSpacebarUtils.FORMAT_TYPE_NONE
        } else {
            if (subtypeChanged
                && languageOnSpacebarFormatType != LanguageOnSpacebarUtils.FORMAT_TYPE_NONE
            ) {
                setLanguageOnSpacebarAnimAlpha(Constants.Color.ALPHA_OPAQUE)
                if (animator.isStarted()) {
                    animator.cancel()
                }
                animator.start()
            } else {
                if (!animator.isStarted()) {
                    mLanguageOnSpacebarAnimAlpha = mLanguageOnSpacebarFinalAlpha
                }
            }
        }
        invalidateKey(mSpaceKey)
    }

    override fun onDrawKeyTopVisuals(
        key: Key, canvas: Canvas, paint: Paint,
        params: KeyDrawParams
    ) {
        if (key.altCodeWhileTyping() && key.isEnabled) {
            params.mAnimAlpha = mAltCodeKeyWhileTypingAnimAlpha
        }
        super.onDrawKeyTopVisuals(key, canvas, paint, params)
        val code: Int = key.code
        if (code == Constants.CODE_SPACE) {
            // If input language are explicitly selected.
            if (mLanguageOnSpacebarFormatType != LanguageOnSpacebarUtils.FORMAT_TYPE_NONE) {
                drawLanguageOnSpacebar(key, canvas, paint)
            }
            // Whether space key needs to show the "..." popup hint for special purposes
            if (key.isLongPressEnabled && mHasMultipleEnabledIMEsOrSubtypes) {
                drawKeyPopupHint(key, canvas, paint, params)
            }
        } else if (code == Constants.CODE_LANGUAGE_SWITCH) {
            drawKeyPopupHint(key, canvas, paint, params)
        }
    }

    private fun fitsTextIntoWidth(width: Int, text: String, paint: Paint): Boolean {
        val maxTextWidth: Int = width - mLanguageOnSpacebarHorizontalMargin * 2
        paint.setTextScaleX(1.0f)
        val textWidth: Float = TypefaceUtils.getStringWidth(text, paint)
        if (textWidth < width) {
            return true
        }

        val scaleX: Float = maxTextWidth / textWidth
        if (scaleX < MINIMUM_XSCALE_OF_LANGUAGE_NAME) {
            return false
        }

        paint.setTextScaleX(scaleX)
        return TypefaceUtils.getStringWidth(text, paint) < maxTextWidth
    }

    // Layout language name on spacebar.
    private fun layoutLanguageOnSpacebar(
        paint: Paint,
        subtype: RichInputMethodSubtype, width: Int
    ): String {
        // Choose appropriate language name to fit into the width.
        if (mLanguageOnSpacebarFormatType == LanguageOnSpacebarUtils.FORMAT_TYPE_FULL_LOCALE) {
            val fullText: String = subtype.fullDisplayName
            if (fitsTextIntoWidth(width, fullText, paint)) {
                return fullText
            }
        }

        val middleText: String = subtype.middleDisplayName
        if (fitsTextIntoWidth(width, middleText, paint)) {
            return middleText
        }

        return ""
    }

    private fun drawLanguageOnSpacebar(key: Key, canvas: Canvas, paint: Paint) {
        val keyboard: Keyboard = this.keyboard ?: return
        val width: Int = key.width
        val height: Int = key.height
        paint.setTextAlign(Align.CENTER)
        paint.setTypeface(Typeface.DEFAULT)
        paint.setTextSize(mLanguageOnSpacebarTextSize)
        val language: String = layoutLanguageOnSpacebar(
            paint,
            keyboard.mId.mSubtype!!, width
        )
        // Draw language text with shadow
        val descent: Float = paint.descent()
        val textHeight: Float = -paint.ascent() + descent
        val baseline: Float = height / 2 + textHeight / 2
        if (mLanguageOnSpacebarTextShadowRadius > 0.0f) {
            paint.setShadowLayer(
                mLanguageOnSpacebarTextShadowRadius, 0f, 0f,
                mLanguageOnSpacebarTextShadowColor
            )
        } else {
            paint.clearShadowLayer()
        }
        paint.setColor(mLanguageOnSpacebarTextColor)
        paint.setAlpha(mLanguageOnSpacebarAnimAlpha)
        canvas.drawText(language, (width / 2).toFloat(), baseline - descent, paint)
        paint.clearShadowLayer()
        paint.setTextScaleX(1.0f)
    }

    override fun deallocateMemory() {
        super.deallocateMemory()
        mDrawingPreviewPlacerView.deallocateMemory()
    }

    companion object {
        private val TAG: String = MainKeyboardView::class.java.getSimpleName()

        private val LANGUAGE_ON_SPACEBAR_TEXT_SHADOW_RADIUS_DISABLED: Float = -1.0f

        // The minimum x-scale to fit the language name on spacebar.
        private const val MINIMUM_XSCALE_OF_LANGUAGE_NAME: Float = 0.8f

        private fun cancelAndStartAnimators(
            animatorToCancel: ObjectAnimator?,
            animatorToStart: ObjectAnimator?
        ) {
            if (animatorToCancel == null || animatorToStart == null) {
                // TODO: Stop using null as a no-operation animator.
                return
            }
            var startFraction: Float = 0.0f
            if (animatorToCancel.isStarted()) {
                animatorToCancel.cancel()
                startFraction = 1.0f - animatorToCancel.getAnimatedFraction()
            }
            val startTime: Long = (animatorToStart.getDuration() * startFraction).toLong()
            animatorToStart.start()
            animatorToStart.setCurrentPlayTime(startTime)
        }
    }
}
