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

import android.content.res.Resources
import android.content.res.TypedArray
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import com.android.inputmethod.keyboard.internal.BatchInputArbiter
import com.android.inputmethod.keyboard.internal.BatchInputArbiter.BatchInputArbiterListener
import com.android.inputmethod.keyboard.internal.BogusMoveEventDetector
import com.android.inputmethod.keyboard.internal.DrawingProxy
import com.android.inputmethod.keyboard.internal.GestureEnabler
import com.android.inputmethod.keyboard.internal.GestureStrokeDrawingParams
import com.android.inputmethod.keyboard.internal.GestureStrokeDrawingPoints
import com.android.inputmethod.keyboard.internal.GestureStrokeRecognitionParams
import com.android.inputmethod.keyboard.internal.PointerTrackerQueue
import com.android.inputmethod.keyboard.internal.TimerProxy
import com.android.inputmethod.keyboard.internal.TypingTimeRecorder
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.CoordinateUtils
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.define.DebugFlags
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.utils.ResourceUtils
import kotlin.math.hypot
import kotlin.math.sqrt

class PointerTracker private constructor(id: Int) : PointerTrackerQueue.Element,
    BatchInputArbiterListener {
    internal class PointerTrackerParams(mainKeyboardViewAttr: TypedArray) {
        val mKeySelectionByDraggingFinger: Boolean
        val mTouchNoiseThresholdTime: Int
        val mTouchNoiseThresholdDistance: Int
        val mSuppressKeyPreviewAfterBatchInputDuration: Int
        val mKeyRepeatStartTimeout: Int
        val mKeyRepeatInterval: Int
        val mLongPressShiftLockTimeout: Int

        init {
            mKeySelectionByDraggingFinger = mainKeyboardViewAttr.getBoolean(
                R.styleable.MainKeyboardView_keySelectionByDraggingFinger, false
            )
            mTouchNoiseThresholdTime = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_touchNoiseThresholdTime, 0
            )
            mTouchNoiseThresholdDistance = mainKeyboardViewAttr.getDimensionPixelSize(
                R.styleable.MainKeyboardView_touchNoiseThresholdDistance, 0
            )
            mSuppressKeyPreviewAfterBatchInputDuration = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_suppressKeyPreviewAfterBatchInputDuration, 0
            )
            mKeyRepeatStartTimeout = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_keyRepeatStartTimeout, 0
            )
            mKeyRepeatInterval = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_keyRepeatInterval, 0
            )
            mLongPressShiftLockTimeout = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_longPressShiftLockTimeout, 0
            )
        }
    }

    val mPointerId: Int

    // The {@link KeyDetector} is set whenever the down event is processed. Also this is updated
    // when new {@link Keyboard} is set by {@link #setKeyDetector(KeyDetector)}.
    private var mKeyDetector: KeyDetector = KeyDetector()
    private var mKeyboard: Keyboard? = null
    private var mPhantomSuddenMoveThreshold: Int = 0
    private val mBogusMoveEventDetector: BogusMoveEventDetector = BogusMoveEventDetector()

    private var mIsDetectingGesture: Boolean = false // per PointerTracker.

    // The position and time at which first down event occurred.
    private var mDownTime: Long = 0

    private val mDownCoordinates: IntArray = CoordinateUtils.newInstance()
    private val mUpTime: Long = 0

    // The current key where this pointer is.
    private var mCurrentKey: Key? = null

    // The position where the current key was recognized for the first time.
    private var mKeyX: Int = 0
    private var mKeyY: Int = 0

    // Last pointer position.
    private var mLastX: Int = 0
    private var mLastY: Int = 0

    // true if keyboard layout has been changed.
    private var mKeyboardLayoutHasBeenChanged: Boolean = false

    // true if this pointer is no longer triggering any action because it has been canceled.
    private var mIsTrackingForActionDisabled: Boolean = false

    // the more keys panel currently being shown. equals null if no panel is active.
    private var mMoreKeysPanel: MoreKeysPanel? = null

    // true if this pointer is in the dragging finger mode.
    var mIsInDraggingFinger: Boolean = false

    // true if this pointer is sliding from a modifier key and in the sliding key input mode,
    // so that further modifier keys should be ignored.
    var mIsInSlidingKeyInput: Boolean = false

    // if not a NOT_A_CODE, the key of this code is repeating
    private var mCurrentRepeatingKeyCode: Int = Constants.NOT_A_CODE

    // true if dragging finger is allowed.
    private var mIsAllowedDraggingFinger: Boolean = false

    private val mBatchInputArbiter: BatchInputArbiter
    private val mGestureStrokeDrawingPoints: GestureStrokeDrawingPoints

    init {
        mPointerId = id
        mBatchInputArbiter = BatchInputArbiter(id, sGestureStrokeRecognitionParams)
        mGestureStrokeDrawingPoints = GestureStrokeDrawingPoints(sGestureStrokeDrawingParams)
    }

    // Returns true if keyboard has been changed by this callback.
    private fun callListenerOnPressAndCheckKeyboardLayoutChange(
        key: Key?,
        repeatCount: Int
    ): Boolean {
        // While gesture input is going on, this method should be a no-operation. But when gesture
        // input has been canceled, <code>sInGesture</code> and <code>mIsDetectingGesture</code>
        // are set to false. To keep this method is a no-operation,
        // <code>mIsTrackingForActionDisabled</code> should also be taken account of.
        if (sInGesture || mIsDetectingGesture || mIsTrackingForActionDisabled) {
            return false
        }
        val ignoreModifierKey: Boolean = mIsInDraggingFinger && key!!.isModifier
        if (DEBUG_LISTENER) {
            Log.d(
                TAG, String.format(
                    "[%d] onPress    : %s%s%s%s", mPointerId,
                    (if (key == null) "none" else Constants.printableCode(key.code)),
                    if (ignoreModifierKey) " ignoreModifier" else "",
                    if (key!!.isEnabled) "" else " disabled",
                    if (repeatCount > 0) " repeatCount=" + repeatCount else ""
                )
            )
        }
        if (ignoreModifierKey) {
            return false
        }
        if (key!!.isEnabled) {
            sListener!!.onPressKey(key.code, repeatCount, getActivePointerTrackerCount() == 1)
            val keyboardLayoutHasBeenChanged: Boolean = mKeyboardLayoutHasBeenChanged
            mKeyboardLayoutHasBeenChanged = false
            sTimerProxy!!.startTypingStateTimer(key)
            return keyboardLayoutHasBeenChanged
        }
        return false
    }

    // Note that we need primaryCode argument because the keyboard may in shifted state and the
    // primaryCode is different from {@link Key#mKeyCode}.
    private fun callListenerOnCodeInput(
        key: Key, primaryCode: Int, x: Int,
        y: Int, eventTime: Long, isKeyRepeat: Boolean
    ) {
        val ignoreModifierKey: Boolean = mIsInDraggingFinger && key.isModifier
        val altersCode: Boolean = key.altCodeWhileTyping() && sTimerProxy!!.isTypingState()
        val code: Int = if (altersCode) key.altCode else primaryCode
        if (DEBUG_LISTENER) {
            val output: String? = if (code == Constants.CODE_OUTPUT_TEXT)
                key.outputText
            else
                Constants.printableCode(code)
            Log.d(
                TAG, String.format(
                    "[%d] onCodeInput: %4d %4d %s%s%s%s", mPointerId, x, y,
                    output, if (ignoreModifierKey) " ignoreModifier" else "",
                    if (altersCode) " altersCode" else "", if (key.isEnabled) "" else " disabled"
                )
            )
        }
        if (ignoreModifierKey) {
            return
        }
        // Even if the key is disabled, it should respond if it is in the altCodeWhileTyping state.
        if (key.isEnabled || altersCode) {
            sTypingTimeRecorder!!.onCodeInput(code, eventTime)
            if (code == Constants.CODE_OUTPUT_TEXT) {
                sListener!!.onTextInput(key.outputText)
            } else if (code != Constants.CODE_UNSPECIFIED) {
                if (mKeyboard!!.hasProximityCharsCorrection(code)) {
                    sListener!!.onCodeInput(code, x, y, isKeyRepeat)
                } else {
                    sListener!!.onCodeInput(
                        code,
                        Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, isKeyRepeat
                    )
                }
            }
        }
    }

    // Note that we need primaryCode argument because the keyboard may be in shifted state and the
    // primaryCode is different from {@link Key#mKeyCode}.
    private fun callListenerOnRelease(
        key: Key, primaryCode: Int,
        withSliding: Boolean
    ) {
        // See the comment at {@link #callListenerOnPressAndCheckKeyboardLayoutChange(Key}}.
        if (sInGesture || mIsDetectingGesture || mIsTrackingForActionDisabled) {
            return
        }
        val ignoreModifierKey: Boolean = mIsInDraggingFinger && key.isModifier
        if (DEBUG_LISTENER) {
            Log.d(
                TAG, String.format(
                    "[%d] onRelease  : %s%s%s%s",
                    mPointerId,
                    Constants.printableCode(primaryCode),
                    if (withSliding) " sliding" else "",
                    if (ignoreModifierKey) " ignoreModifier" else "",
                    if (key.isEnabled) "" else " disabled"
                )
            )
        }
        if (ignoreModifierKey) {
            return
        }
        if (key.isEnabled) {
            sListener!!.onReleaseKey(primaryCode, withSliding)
        }
    }

    private fun callListenerOnFinishSlidingInput() {
        if (DEBUG_LISTENER) {
            Log.d(TAG, String.format("[%d] onFinishSlidingInput", mPointerId))
        }
        sListener!!.onFinishSlidingInput()
    }

    private fun callListenerOnCancelInput() {
        if (DEBUG_LISTENER) {
            Log.d(TAG, String.format("[%d] onCancelInput", mPointerId))
        }
        sListener!!.onCancelInput()
    }

    private fun setKeyDetectorInner(keyDetector: KeyDetector) {
        val keyboard: Keyboard? = keyDetector.getKeyboard()
        if (keyboard == null) {
            return
        }
        if (keyDetector === mKeyDetector && keyboard === mKeyboard) {
            return
        }
        mKeyDetector = keyDetector
        mKeyboard = keyboard
        // Mark that keyboard layout has been changed.
        mKeyboardLayoutHasBeenChanged = true
        val keyWidth: Int = mKeyboard!!.mMostCommonKeyWidth
        val keyHeight: Int = mKeyboard!!.mMostCommonKeyHeight
        mBatchInputArbiter.setKeyboardGeometry(keyWidth, mKeyboard!!.mOccupiedHeight)
        // Keep {@link #mCurrentKey} that comes from previous keyboard. The key preview of
        // {@link #mCurrentKey} will be dismissed by {@setReleasedKeyGraphics(Key)} via
        // {@link onMoveEventInternal(int,int,long)} or {@link #onUpEventInternal(int,int,long)}.
        mPhantomSuddenMoveThreshold = (keyWidth * PHANTOM_SUDDEN_MOVE_THRESHOLD).toInt()
        mBogusMoveEventDetector.setKeyboardGeometry(keyWidth, keyHeight)
    }

    override fun isInDraggingFinger(): Boolean {
        return mIsInDraggingFinger
    }

    fun getKey(): Key? {
        return mCurrentKey
    }

    override fun isModifier(): Boolean {
        return mCurrentKey != null && mCurrentKey!!.isModifier
    }

    fun getKeyOn(x: Int, y: Int): Key? {
        return mKeyDetector.detectHitKey(x, y)
    }

    private fun setReleasedKeyGraphics(key: Key?, withAnimation: Boolean) {
        if (key == null) {
            return
        }

        sDrawingProxy!!.onKeyReleased(key, withAnimation)

        if (key.isShift) {
            for (shiftKey: Key in mKeyboard!!.mShiftKeys) {
                if (shiftKey !== key) {
                    sDrawingProxy!!.onKeyReleased(shiftKey, false /* withAnimation */)
                }
            }
        }

        if (key.altCodeWhileTyping()) {
            val altCode: Int = key.altCode
            val altKey: Key? = mKeyboard!!.getKey(altCode)
            if (altKey != null) {
                sDrawingProxy!!.onKeyReleased(altKey, false /* withAnimation */)
            }
            for (k: Key in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k !== key && k.altCode == altCode) {
                    sDrawingProxy!!.onKeyReleased(k, false /* withAnimation */)
                }
            }
        }
    }

    private fun setPressedKeyGraphics(key: Key?, eventTime: Long) {
        if (key == null) {
            return
        }

        // Even if the key is disabled, it should respond if it is in the altCodeWhileTyping state.
        val altersCode: Boolean = key.altCodeWhileTyping() && sTimerProxy!!.isTypingState()
        val needsToUpdateGraphics: Boolean = key.isEnabled || altersCode
        if (!needsToUpdateGraphics) {
            return
        }

        val noKeyPreview: Boolean = sInGesture || needsToSuppressKeyPreviewPopup(eventTime)
        sDrawingProxy!!.onKeyPressed(key, !noKeyPreview)

        if (key.isShift) {
            for (shiftKey: Key in mKeyboard!!.mShiftKeys) {
                if (shiftKey !== key) {
                    sDrawingProxy!!.onKeyPressed(shiftKey, false /* withPreview */)
                }
            }
        }

        if (altersCode) {
            val altCode: Int = key.altCode
            val altKey: Key? = mKeyboard!!.getKey(altCode)
            if (altKey != null) {
                sDrawingProxy!!.onKeyPressed(altKey, false /* withPreview */)
            }
            for (k: Key in mKeyboard!!.mAltCodeKeysWhileTyping) {
                if (k !== key && k.altCode == altCode) {
                    sDrawingProxy!!.onKeyPressed(k, false /* withPreview */)
                }
            }
        }
    }

    fun getGestureStrokeDrawingPoints(): GestureStrokeDrawingPoints {
        return mGestureStrokeDrawingPoints
    }

    fun getLastCoordinates(outCoords: IntArray) {
        CoordinateUtils.set(outCoords, mLastX, mLastY)
    }

    fun getDownTime(): Long {
        return mDownTime
    }

    fun getDownCoordinates(outCoords: IntArray) {
        CoordinateUtils.copy(outCoords, mDownCoordinates)
    }

    private fun onDownKey(x: Int, y: Int, eventTime: Long): Key {
        mDownTime = eventTime
        CoordinateUtils.set(mDownCoordinates, x, y)
        mBogusMoveEventDetector.onDownKey()
        return onMoveToNewKey(onMoveKeyInternal(x, y)!!, x, y)!!
    }

    private fun onMoveKeyInternal(x: Int, y: Int): Key? {
        mBogusMoveEventDetector.onMoveKey(getDistance(x, y, mLastX, mLastY))
        mLastX = x
        mLastY = y
        return mKeyDetector.detectHitKey(x, y)
    }

    private fun onMoveKey(x: Int, y: Int): Key? {
        return onMoveKeyInternal(x, y)
    }

    private fun onMoveToNewKey(newKey: Key?, x: Int, y: Int): Key? {
        mCurrentKey = newKey
        mKeyX = x
        mKeyY = y
        return newKey
    }

    private fun isOldestTrackerInQueue(): Boolean {
        return sPointerTrackerQueue.getOldestElement() === this
    }

    // Implements {@link BatchInputArbiterListener}.
    override fun onStartBatchInput() {
        if (DEBUG_LISTENER) {
            Log.d(TAG, String.format("[%d] onStartBatchInput", mPointerId))
        }
        sListener!!.onStartBatchInput()
        dismissAllMoreKeysPanels()
        sTimerProxy!!.cancelLongPressTimersOf(this)
    }

    private fun showGestureTrail() {
        if (mIsTrackingForActionDisabled) {
            return
        }
        // A gesture floating preview text will be shown at the oldest pointer/finger on the screen.
        sDrawingProxy!!.showGestureTrail(
            this, isOldestTrackerInQueue() /* showsFloatingPreviewText */
        )
    }

    fun updateBatchInputByTimer(syntheticMoveEventTime: Long) {
        mBatchInputArbiter.updateBatchInputByTimer(syntheticMoveEventTime, this)
    }

    // Implements {@link BatchInputArbiterListener}.
    override fun onUpdateBatchInput(aggregatedPointers: InputPointers, moveEventTime: Long) {
        if (DEBUG_LISTENER) {
            Log.d(
                TAG, String.format(
                    "[%d] onUpdateBatchInput: batchPoints=%d", mPointerId,
                    aggregatedPointers.pointerSize
                )
            )
        }
        sListener!!.onUpdateBatchInput(aggregatedPointers)
    }

    // Implements {@link BatchInputArbiterListener}.
    override fun onStartUpdateBatchInputTimer() {
        sTimerProxy!!.startUpdateBatchInputTimer(this)
    }

    // Implements {@link BatchInputArbiterListener}.
    override fun onEndBatchInput(aggregatedPointers: InputPointers, upEventTime: Long) {
        sTypingTimeRecorder!!.onEndBatchInput(upEventTime)
        sTimerProxy!!.cancelAllUpdateBatchInputTimers()
        if (mIsTrackingForActionDisabled) {
            return
        }
        if (DEBUG_LISTENER) {
            Log.d(
                TAG, String.format(
                    "[%d] onEndBatchInput   : batchPoints=%d",
                    mPointerId, aggregatedPointers.pointerSize
                )
            )
        }
        sListener!!.onEndBatchInput(aggregatedPointers)
    }

    private fun cancelBatchInput() {
        cancelAllPointerTrackers()
        mIsDetectingGesture = false
        if (!sInGesture) {
            return
        }
        sInGesture = false
        if (DEBUG_LISTENER) {
            Log.d(TAG, String.format("[%d] onCancelBatchInput", mPointerId))
        }
        sListener!!.onCancelBatchInput()
    }

    fun processMotionEvent(me: MotionEvent, keyDetector: KeyDetector) {
        val action: Int = me.getActionMasked()
        val eventTime: Long = me.getEventTime()
        if (action == MotionEvent.ACTION_MOVE) {
            // When this pointer is the only active pointer and is showing a more keys panel,
            // we should ignore other pointers' motion event.
            val shouldIgnoreOtherPointers: Boolean =
                isShowingMoreKeysPanel() && getActivePointerTrackerCount() == 1
            val pointerCount: Int = me.getPointerCount()
            for (index in 0 until pointerCount) {
                val id: Int = me.getPointerId(index)
                if (shouldIgnoreOtherPointers && id != mPointerId) {
                    continue
                }
                val x: Int = me.getX(index).toInt()
                val y: Int = me.getY(index).toInt()
                val tracker: PointerTracker = getPointerTracker(id)
                tracker.onMoveEvent(x, y, eventTime, me)
            }
            return
        }
        val index: Int = me.getActionIndex()
        val x: Int = me.getX(index).toInt()
        val y: Int = me.getY(index).toInt()
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(
                x,
                y,
                eventTime,
                keyDetector
            )

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, eventTime)
            MotionEvent.ACTION_CANCEL -> onCancelEvent(x, y, eventTime)
        }
    }

    private fun onDownEvent(
        x: Int, y: Int, eventTime: Long,
        keyDetector: KeyDetector
    ) {
        setKeyDetectorInner(keyDetector)
        if (DEBUG_EVENT) {
            printTouchEvent("onDownEvent:", x, y, eventTime)
        }
        // Naive up-to-down noise filter.
        val deltaT: Long = eventTime - mUpTime
        if (deltaT < sParams!!.mTouchNoiseThresholdTime) {
            val distance: Int = getDistance(x, y, mLastX, mLastY)
            if (distance < sParams!!.mTouchNoiseThresholdDistance) {
                if (DEBUG_MODE) Log.w(
                    TAG, String.format(
                        "[%d] onDownEvent:"
                                + " ignore potential noise: time=%d distance=%d",
                        mPointerId, deltaT, distance
                    )
                )
                cancelTrackingForAction()
                return
            }
        }

        val key: Key? = getKeyOn(x, y)
        mBogusMoveEventDetector.onActualDownEvent(x, y)
        if (key != null && key.isModifier) {
            // Before processing a down event of modifier key, all pointers already being
            // tracked should be released.
            sPointerTrackerQueue.releaseAllPointers(eventTime)
        }
        sPointerTrackerQueue.add(this)
        onDownEventInternal(x, y, eventTime)
        if (!sGestureEnabler.shouldHandleGesture()) {
            return
        }
        // A gesture should start only from a non-modifier key. Note that the gesture detection is
        // disabled when the key is repeating.
        val keyboard = mKeyboard
        mIsDetectingGesture = keyboard != null && keyboard.mId.isAlphabetKeyboard()
                && key != null && !key.isModifier
        if (mIsDetectingGesture) {
            mBatchInputArbiter.addDownEventPoint(
                x, y, eventTime,
                sTypingTimeRecorder!!.getLastLetterTypingTime(), getActivePointerTrackerCount()
            )
            mGestureStrokeDrawingPoints.onDownEvent(
                x, y, mBatchInputArbiter.getElapsedTimeSinceFirstDown(eventTime)
            )
        }
    }

    /* package */
    fun isShowingMoreKeysPanel(): Boolean {
        return (mMoreKeysPanel != null)
    }

    private fun dismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel!!.dismissMoreKeysPanel()
            mMoreKeysPanel = null
        }
    }

    private fun onDownEventInternal(x: Int, y: Int, eventTime: Long) {
        var key: Key? = onDownKey(x, y, eventTime)
        // Key selection by dragging finger is allowed when 1) key selection by dragging finger is
        // enabled by configuration, 2) this pointer starts dragging from modifier key, or 3) this
        // pointer's KeyDetector always allows key selection by dragging finger, such as
        // {@link MoreKeysKeyboard}.
        mIsAllowedDraggingFinger = sParams!!.mKeySelectionByDraggingFinger
                || (key != null && key.isModifier)
                || mKeyDetector.alwaysAllowsKeySelectionByDraggingFinger()
        mKeyboardLayoutHasBeenChanged = false
        mIsTrackingForActionDisabled = false
        resetKeySelectionByDraggingFinger()
        if (key != null) {
            // This onPress call may have changed keyboard layout. Those cases are detected at
            // {@link #setKeyboard}. In those cases, we should update key according to the new
            // keyboard layout.
            if (callListenerOnPressAndCheckKeyboardLayoutChange(key, 0 /* repeatCount */)) {
                key = onDownKey(x, y, eventTime)
            }

            startRepeatKey(key)
            startLongPressTimer(key)
            setPressedKeyGraphics(key, eventTime)
        }
    }

    private fun startKeySelectionByDraggingFinger(key: Key) {
        if (!mIsInDraggingFinger) {
            mIsInSlidingKeyInput = key.isModifier
        }
        mIsInDraggingFinger = true
    }

    private fun resetKeySelectionByDraggingFinger() {
        mIsInDraggingFinger = false
        mIsInSlidingKeyInput = false
        sDrawingProxy!!.showSlidingKeyInputPreview(null /* tracker */)
    }

    private fun onGestureMoveEvent(
        x: Int, y: Int, eventTime: Long,
        isMajorEvent: Boolean, key: Key?
    ) {
        if (!mIsDetectingGesture) {
            return
        }
        val onValidArea: Boolean = mBatchInputArbiter.addMoveEventPoint(
            x, y, eventTime, isMajorEvent, this
        )
        // If the move event goes out from valid batch input area, cancel batch input.
        if (!onValidArea) {
            cancelBatchInput()
            return
        }
        mGestureStrokeDrawingPoints.onMoveEvent(
            x, y, mBatchInputArbiter.getElapsedTimeSinceFirstDown(eventTime)
        )
        // If the MoreKeysPanel is showing then do not attempt to enter gesture mode. However,
        // the gestured touch points are still being recorded in case the panel is dismissed.
        if (isShowingMoreKeysPanel()) {
            return
        }
        if (!sInGesture && key != null && Character.isLetter(key.code)
            && mBatchInputArbiter.mayStartBatchInput(this)
        ) {
            sInGesture = true
        }
        if (sInGesture) {
            if (key != null) {
                mBatchInputArbiter.updateBatchInput(eventTime, this)
            }
            showGestureTrail()
        }
    }

    private fun onMoveEvent(x: Int, y: Int, eventTime: Long, me: MotionEvent?) {
        if (DEBUG_MOVE_EVENT) {
            printTouchEvent("onMoveEvent:", x, y, eventTime)
        }
        if (mIsTrackingForActionDisabled) {
            return
        }

        if (sGestureEnabler.shouldHandleGesture() && me != null) {
            // Add historical points to gesture path.
            val pointerIndex: Int = me.findPointerIndex(mPointerId)
            val historicalSize: Int = me.getHistorySize()
            for (h in 0 until historicalSize) {
                val historicalX: Int = me.getHistoricalX(pointerIndex, h).toInt()
                val historicalY: Int = me.getHistoricalY(pointerIndex, h).toInt()
                val historicalTime: Long = me.getHistoricalEventTime(h)
                onGestureMoveEvent(
                    historicalX, historicalY, historicalTime,
                    false,  /* isMajorEvent */null
                )
            }
        }

        if (isShowingMoreKeysPanel()) {
            val translatedX: Int = mMoreKeysPanel!!.translateX(x)
            val translatedY: Int = mMoreKeysPanel!!.translateY(y)
            mMoreKeysPanel!!.onMoveEvent(translatedX, translatedY, mPointerId, eventTime)
            onMoveKey(x, y)
            if (mIsInSlidingKeyInput) {
                sDrawingProxy!!.showSlidingKeyInputPreview(this)
            }
            return
        }
        onMoveEventInternal(x, y, eventTime)
    }

    private fun processDraggingFingerInToNewKey(
        newKey: Key, x: Int, y: Int,
        eventTime: Long
    ) {
        // This onPress call may have changed keyboard layout. Those cases are detected
        // at {@link #setKeyboard}. In those cases, we should update key according
        // to the new keyboard layout.
        var key: Key? = newKey
        if (callListenerOnPressAndCheckKeyboardLayoutChange(key, 0 /* repeatCount */)) {
            key = onMoveKey(x, y)
        }
        onMoveToNewKey(key!!, x, y)
        if (mIsTrackingForActionDisabled) {
            return
        }
        startLongPressTimer(key)
        setPressedKeyGraphics(key, eventTime)
    }

    private fun processPhantomSuddenMoveHack(
        key: Key, x: Int, y: Int,
        eventTime: Long, oldKey: Key, lastX: Int, lastY: Int
    ) {
        if (DEBUG_MODE) {
            Log.w(
                TAG, String.format(
                    ("[%d] onMoveEvent:"
                            + " phantom sudden move event (distance=%d) is translated to "
                            + "up[%d,%d,%s]/down[%d,%d,%s] events"), mPointerId,
                    getDistance(x, y, lastX, lastY),
                    lastX, lastY, Constants.printableCode(oldKey.code),
                    x, y, Constants.printableCode(key.code)
                )
            )
        }
        onUpEventInternal(x, y, eventTime)
        onDownEventInternal(x, y, eventTime)
    }

    private fun processProximateBogusDownMoveUpEventHack(
        key: Key, x: Int, y: Int,
        eventTime: Long, oldKey: Key, lastX: Int, lastY: Int
    ) {
        if (DEBUG_MODE) {
            val keyDiagonal: Float = hypot(
                mKeyboard!!.mMostCommonKeyWidth.toDouble(),
                mKeyboard!!.mMostCommonKeyHeight.toDouble()
            ).toFloat()
            val radiusRatio: Float =
                (mBogusMoveEventDetector.getDistanceFromDownEvent(x, y)
                        / keyDiagonal)
            Log.w(
                TAG, String.format(
                    ("[%d] onMoveEvent:"
                            + " bogus down-move-up event (raidus=%.2f key diagonal) is "
                            + " translated to up[%d,%d,%s]/down[%d,%d,%s] events"),
                    mPointerId, radiusRatio,
                    lastX, lastY, Constants.printableCode(oldKey.code),
                    x, y, Constants.printableCode(key.code)
                )
            )
        }
        onUpEventInternal(x, y, eventTime)
        onDownEventInternal(x, y, eventTime)
    }

    private fun processDraggingFingerOutFromOldKey(oldKey: Key) {
        setReleasedKeyGraphics(oldKey, true /* withAnimation */)
        callListenerOnRelease(oldKey, oldKey.code, true /* withSliding */)
        startKeySelectionByDraggingFinger(oldKey)
        sTimerProxy!!.cancelKeyTimersOf(this)
    }

    private fun dragFingerFromOldKeyToNewKey(
        key: Key, x: Int, y: Int,
        eventTime: Long, oldKey: Key, lastX: Int, lastY: Int
    ) {
        // The pointer has been slid in to the new key from the previous key, we must call
        // onRelease() first to notify that the previous key has been released, then call
        // onPress() to notify that the new key is being pressed.
        processDraggingFingerOutFromOldKey(oldKey)
        startRepeatKey(key)
        if (mIsAllowedDraggingFinger) {
            processDraggingFingerInToNewKey(key, x, y, eventTime)
        } else if (sNeedsPhantomSuddenMoveEventHack
            && getDistance(x, y, lastX, lastY) >= mPhantomSuddenMoveThreshold
        ) {
            processPhantomSuddenMoveHack(key, x, y, eventTime, oldKey, lastX, lastY)
        } else if (sTypingTimeRecorder!!.isInFastTyping(eventTime)
            && mBogusMoveEventDetector.isCloseToActualDownEvent(x, y)
        ) {
            processProximateBogusDownMoveUpEventHack(key, x, y, eventTime, oldKey, lastX, lastY)
        } else if (getActivePointerTrackerCount() > 1
            && !sPointerTrackerQueue.hasModifierKeyOlderThan(this)
        ) {
            if (DEBUG_MODE) {
                Log.w(
                    TAG, String.format(
                        "[%d] onMoveEvent:"
                                + " detected sliding finger while multi touching", mPointerId
                    )
                )
            }
            onUpEvent(x, y, eventTime)
            cancelTrackingForAction()
            setReleasedKeyGraphics(oldKey, true /* withAnimation */)
        } else {
            if (!mIsDetectingGesture) {
                cancelTrackingForAction()
            }
            setReleasedKeyGraphics(oldKey, true /* withAnimation */)
        }
    }

    private fun dragFingerOutFromOldKey(oldKey: Key, x: Int, y: Int) {
        // The pointer has been slid out from the previous key, we must call onRelease() to
        // notify that the previous key has been released.
        processDraggingFingerOutFromOldKey(oldKey)
        if (mIsAllowedDraggingFinger) {
            onMoveToNewKey(null, x, y)
        } else {
            if (!mIsDetectingGesture) {
                cancelTrackingForAction()
            }
        }
    }

    private fun onMoveEventInternal(x: Int, y: Int, eventTime: Long) {
        val lastX: Int = mLastX
        val lastY: Int = mLastY
        val oldKey: Key? = mCurrentKey
        val newKey: Key? = onMoveKey(x, y)

        if (sGestureEnabler.shouldHandleGesture()) {
            // Register move event on gesture tracker.
            onGestureMoveEvent(x, y, eventTime, true,  /* isMajorEvent */newKey)
            if (sInGesture) {
                mCurrentKey = null
                setReleasedKeyGraphics(oldKey, true /* withAnimation */)
                return
            }
        }

        if (newKey != null) {
            if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, eventTime, newKey)) {
                dragFingerFromOldKeyToNewKey(newKey, x, y, eventTime, oldKey, lastX, lastY)
            } else if (oldKey == null) {
                // The pointer has been slid in to the new key, but the finger was not on any keys.
                // In this case, we must call onPress() to notify that the new key is being pressed.
                processDraggingFingerInToNewKey(newKey, x, y, eventTime)
            }
        } else { // newKey == null
            if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, eventTime, newKey)) {
                dragFingerOutFromOldKey(oldKey, x, y)
            }
        }
        if (mIsInSlidingKeyInput) {
            sDrawingProxy!!.showSlidingKeyInputPreview(this)
        }
    }

    private fun onUpEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG_EVENT) {
            printTouchEvent("onUpEvent  :", x, y, eventTime)
        }

        sTimerProxy!!.cancelUpdateBatchInputTimer(this)
        if (!sInGesture) {
            if (mCurrentKey != null && mCurrentKey!!.isModifier) {
                // Before processing an up event of modifier key, all pointers already being
                // tracked should be released.
                sPointerTrackerQueue.releaseAllPointersExcept(this, eventTime)
            } else {
                sPointerTrackerQueue.releaseAllPointersOlderThan(this, eventTime)
            }
        }
        onUpEventInternal(x, y, eventTime)
        sPointerTrackerQueue.remove(this)
    }

    // Let this pointer tracker know that one of newer-than-this pointer trackers got an up event.
    // This pointer tracker needs to keep the key top graphics "pressed", but needs to get a
    // "virtual" up event.
    override fun onPhantomUpEvent(eventTime: Long) {
        if (DEBUG_EVENT) {
            printTouchEvent("onPhntEvent:", mLastX, mLastY, eventTime)
        }
        onUpEventInternal(mLastX, mLastY, eventTime)
        cancelTrackingForAction()
    }

    private fun onUpEventInternal(x: Int, y: Int, eventTime: Long) {
        sTimerProxy!!.cancelKeyTimersOf(this)
        val isInDraggingFinger: Boolean = mIsInDraggingFinger
        val isInSlidingKeyInput: Boolean = mIsInSlidingKeyInput
        resetKeySelectionByDraggingFinger()
        mIsDetectingGesture = false
        val currentKey: Key? = mCurrentKey
        mCurrentKey = null
        val currentRepeatingKeyCode: Int = mCurrentRepeatingKeyCode
        mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
        // Release the last pressed key.
        setReleasedKeyGraphics(currentKey, true /* withAnimation */)

        if (isShowingMoreKeysPanel()) {
            if (!mIsTrackingForActionDisabled) {
                val translatedX: Int = mMoreKeysPanel!!.translateX(x)
                val translatedY: Int = mMoreKeysPanel!!.translateY(y)
                mMoreKeysPanel!!.onUpEvent(translatedX, translatedY, mPointerId, eventTime)
            }
            dismissMoreKeysPanel()
            return
        }

        if (sInGesture) {
            if (currentKey != null) {
                callListenerOnRelease(currentKey, currentKey.code, true /* withSliding */)
            }
            if (mBatchInputArbiter.mayEndBatchInput(
                    eventTime, getActivePointerTrackerCount(), this
                )
            ) {
                sInGesture = false
            }
            showGestureTrail()
            return
        }

        if (mIsTrackingForActionDisabled) {
            return
        }
        if (currentKey != null && currentKey.isRepeatable
            && (currentKey.code == currentRepeatingKeyCode) && !isInDraggingFinger
        ) {
            return
        }
        detectAndSendKey(currentKey, mKeyX, mKeyY, eventTime)
        if (isInSlidingKeyInput) {
            callListenerOnFinishSlidingInput()
        }
    }

    override fun cancelTrackingForAction() {
        if (isShowingMoreKeysPanel()) {
            return
        }
        mIsTrackingForActionDisabled = true
    }

    fun isInOperation(): Boolean {
        return !mIsTrackingForActionDisabled
    }

    fun onLongPressed() {
        sTimerProxy!!.cancelLongPressTimersOf(this)
        if (isShowingMoreKeysPanel()) {
            return
        }
        val key: Key? = getKey()
        if (key == null) {
            return
        }
        if (key.hasNoPanelAutoMoreKey()) {
            cancelKeyTracking()
            val moreKeyCode: Int = key.moreKeys[0].mCode
            sListener!!.onPressKey(moreKeyCode, 0,  /* repeatCont */true /* isSinglePointer */)
            sListener!!.onCodeInput(
                moreKeyCode, Constants.NOT_A_COORDINATE,
                Constants.NOT_A_COORDINATE, false /* isKeyRepeat */
            )
            sListener!!.onReleaseKey(moreKeyCode, false /* withSliding */)
            return
        }
        val code: Int = key.code
        if (code == Constants.CODE_SPACE || code == Constants.CODE_LANGUAGE_SWITCH) {
            // Long pressing the space key invokes IME switcher dialog.
            if (sListener!!.onCustomRequest(Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER)) {
                cancelKeyTracking()
                sListener!!.onReleaseKey(code, false /* withSliding */)
                return
            }
        }

        setReleasedKeyGraphics(key, false /* withAnimation */)
        val moreKeysPanel: MoreKeysPanel? = sDrawingProxy!!.showMoreKeysKeyboard(
            key,
            this
        )
        if (moreKeysPanel == null) {
            return
        }
        val translatedX: Int = moreKeysPanel.translateX(mLastX)
        val translatedY: Int = moreKeysPanel.translateY(mLastY)
        moreKeysPanel.onDownEvent(translatedX, translatedY, mPointerId, SystemClock.uptimeMillis())
        mMoreKeysPanel = moreKeysPanel
    }

    private fun cancelKeyTracking() {
        resetKeySelectionByDraggingFinger()
        cancelTrackingForAction()
        setReleasedKeyGraphics(mCurrentKey, true /* withAnimation */)
        sPointerTrackerQueue.remove(this)
    }

    private fun onCancelEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG_EVENT) {
            printTouchEvent("onCancelEvt:", x, y, eventTime)
        }

        cancelBatchInput()
        cancelAllPointerTrackers()
        sPointerTrackerQueue.releaseAllPointers(eventTime)
        onCancelEventInternal()
    }

    private fun onCancelEventInternal() {
        sTimerProxy!!.cancelKeyTimersOf(this)
        setReleasedKeyGraphics(mCurrentKey, true /* withAnimation */)
        resetKeySelectionByDraggingFinger()
        dismissMoreKeysPanel()
    }

    private fun isMajorEnoughMoveToBeOnNewKey(
        x: Int, y: Int, eventTime: Long,
        newKey: Key?
    ): Boolean {
        val curKey: Key? = mCurrentKey
        if (newKey === curKey) {
            return false
        }
        if (curKey == null /* && newKey != null */) {
            return true
        }
        // Here curKey points to the different key from newKey.
        val keyHysteresisDistanceSquared: Int = mKeyDetector.getKeyHysteresisDistanceSquared(
            mIsInSlidingKeyInput
        )
        val distanceFromKeyEdgeSquared: Int = curKey.squaredDistanceToEdge(x, y)
        if (distanceFromKeyEdgeSquared >= keyHysteresisDistanceSquared) {
            if (DEBUG_MODE) {
                val distanceToEdgeRatio: Float =
                    (sqrt(distanceFromKeyEdgeSquared.toDouble()).toFloat()
                            / mKeyboard!!.mMostCommonKeyWidth)
                Log.d(
                    TAG, String.format(
                        "[%d] isMajorEnoughMoveToBeOnNewKey:"
                                + " %.2f key width from key edge", mPointerId, distanceToEdgeRatio
                    )
                )
            }
            return true
        }
        if (!mIsAllowedDraggingFinger && sTypingTimeRecorder!!.isInFastTyping(eventTime)
            && mBogusMoveEventDetector.hasTraveledLongDistance(x, y)
        ) {
            if (DEBUG_MODE) {
                val keyDiagonal: Float = hypot(
                    mKeyboard!!.mMostCommonKeyWidth.toDouble(),
                    mKeyboard!!.mMostCommonKeyHeight.toDouble()
                ).toFloat()
                val lengthFromDownRatio: Float =
                    mBogusMoveEventDetector.getAccumulatedDistanceFromDownKey() / keyDiagonal
                Log.d(
                    TAG, String.format(
                        "[%d] isMajorEnoughMoveToBeOnNewKey:"
                                + " %.2f key diagonal from virtual down point",
                        mPointerId, lengthFromDownRatio
                    )
                )
            }
            return true
        }
        return false
    }

    private fun startLongPressTimer(key: Key?) {
        // Note that we need to cancel all active long press shift key timers if any whenever we
        // start a new long press timer for both non-shift and shift keys.
        sTimerProxy!!.cancelLongPressShiftKeyTimer()
        if (sInGesture) return
        if (key == null) return
        if (!key.isLongPressEnabled) return
        // Caveat: Please note that isLongPressEnabled() can be true even if the current key
        // doesn't have its more keys. (e.g. spacebar, globe key) If we are in the dragging finger
        // mode, we will disable long press timer of such key.
        // We always need to start the long press timer if the key has its more keys regardless of
        // whether or not we are in the dragging finger mode.
        if (mIsInDraggingFinger && key.moreKeys == null) return

        val delay: Int = getLongPressTimeout(key.code)
        if (delay <= 0) return
        sTimerProxy!!.startLongPressTimerOf(this, delay)
    }

    private fun getLongPressTimeout(code: Int): Int {
        if (code == Constants.CODE_SHIFT) {
            return sParams!!.mLongPressShiftLockTimeout
        }
        val longpressTimeout: Int =
            Settings.instance.current!!.mKeyLongpressTimeout
        if (mIsInSlidingKeyInput) {
            // We use longer timeout for sliding finger input started from the modifier key.
            return longpressTimeout * MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT
        }
        return longpressTimeout
    }

    private fun detectAndSendKey(key: Key?, x: Int, y: Int, eventTime: Long) {
        if (key == null) {
            callListenerOnCancelInput()
            return
        }

        val code: Int = key.code
        callListenerOnCodeInput(key, code, x, y, eventTime, false /* isKeyRepeat */)
        callListenerOnRelease(key, code, false /* withSliding */)
    }

    private fun startRepeatKey(key: Key?) {
        if (sInGesture) return
        if (key == null) return
        if (!key.isRepeatable) return
        // Don't start key repeat when we are in the dragging finger mode.
        if (mIsInDraggingFinger) return
        val startRepeatCount: Int = 1
        startKeyRepeatTimer(startRepeatCount)
    }

    fun onKeyRepeat(code: Int, repeatCount: Int) {
        val key: Key? = getKey()
        if (key == null || key.code != code) {
            mCurrentRepeatingKeyCode = Constants.NOT_A_CODE
            return
        }
        mCurrentRepeatingKeyCode = code
        mIsDetectingGesture = false
        val nextRepeatCount: Int = repeatCount + 1
        startKeyRepeatTimer(nextRepeatCount)
        callListenerOnPressAndCheckKeyboardLayoutChange(key, repeatCount)
        callListenerOnCodeInput(
            key, code, mKeyX, mKeyY, SystemClock.uptimeMillis(),
            true /* isKeyRepeat */
        )
    }

    private fun startKeyRepeatTimer(repeatCount: Int) {
        val delay: Int =
            if ((repeatCount == 1)) sParams!!.mKeyRepeatStartTimeout else sParams!!.mKeyRepeatInterval
        sTimerProxy!!.startKeyRepeatTimerOf(this, repeatCount, delay)
    }

    private fun printTouchEvent(
        title: String, x: Int, y: Int,
        eventTime: Long
    ) {
        val key: Key? = mKeyDetector.detectHitKey(x, y)
        val code: String = (if (key == null) "none" else Constants.printableCode(key.code))
        Log.d(
            TAG, String.format(
                "[%d]%s%s %4d %4d %5d %s", mPointerId,
                (if (mIsTrackingForActionDisabled) "-" else " "), title, x, y, eventTime, code
            )
        )
    }

    companion object {
        private val TAG: String = PointerTracker::class.java.getSimpleName()
        private const val DEBUG_EVENT: Boolean = false
        private const val DEBUG_MOVE_EVENT: Boolean = false
        private const val DEBUG_LISTENER: Boolean = false
        private val DEBUG_MODE: Boolean = DebugFlags.DEBUG_ENABLED || DEBUG_EVENT

        private val sGestureEnabler: GestureEnabler = GestureEnabler()

        // Parameters for pointer handling.
        private var sParams: PointerTrackerParams? = null
        private var sGestureStrokeRecognitionParams: GestureStrokeRecognitionParams? = null
        private var sGestureStrokeDrawingParams: GestureStrokeDrawingParams? = null
        private var sNeedsPhantomSuddenMoveEventHack: Boolean = false

        // Move this threshold to resource.
        // TODO: Device specific parameter would be better for device specific hack?
        private const val PHANTOM_SUDDEN_MOVE_THRESHOLD: Float = 0.25f // in keyWidth

        private val sTrackers: ArrayList<PointerTracker> = ArrayList()
        private val sPointerTrackerQueue: PointerTrackerQueue = PointerTrackerQueue()

        private var sDrawingProxy: DrawingProxy? = null
        private var sTimerProxy: TimerProxy? = null
        private var sListener: KeyboardActionListener? =
            KeyboardActionListener.EMPTY_LISTENER

        private var sInGesture: Boolean = false
        private var sTypingTimeRecorder: TypingTimeRecorder? = null

        private const val MULTIPLIER_FOR_LONG_PRESS_TIMEOUT_IN_SLIDING_INPUT: Int = 3

        // TODO: Add PointerTrackerFactory singleton and move some class static methods into it.
        fun init(
            mainKeyboardViewAttr: TypedArray, timerProxy: TimerProxy?,
            drawingProxy: DrawingProxy?
        ) {
            sParams = PointerTrackerParams(mainKeyboardViewAttr)
            sGestureStrokeRecognitionParams = GestureStrokeRecognitionParams(mainKeyboardViewAttr)
            sGestureStrokeDrawingParams = GestureStrokeDrawingParams(mainKeyboardViewAttr)
            sTypingTimeRecorder = TypingTimeRecorder(
                sGestureStrokeRecognitionParams!!.mStaticTimeThresholdAfterFastTyping,
                sParams!!.mSuppressKeyPreviewAfterBatchInputDuration
            )

            val res: Resources = mainKeyboardViewAttr.getResources()
            sNeedsPhantomSuddenMoveEventHack = ResourceUtils.getDeviceOverrideValue(
                res,
                R.array.phantom_sudden_move_event_device_list, java.lang.Boolean.FALSE.toString()
            ).toBoolean()
            BogusMoveEventDetector.init(res)

            sTimerProxy = timerProxy
            sDrawingProxy = drawingProxy
        }

        // Note that this method is called from a non-UI thread.
        fun setMainDictionaryAvailability(mainDictionaryAvailable: Boolean) {
            sGestureEnabler.setMainDictionaryAvailability(mainDictionaryAvailable)
        }

        fun setGestureHandlingEnabledByUser(gestureHandlingEnabledByUser: Boolean) {
            sGestureEnabler.setGestureHandlingEnabledByUser(gestureHandlingEnabledByUser)
        }

        fun getPointerTracker(id: Int): PointerTracker {
            val trackers: ArrayList<PointerTracker> = sTrackers

            // Create pointer trackers until we can get 'id+1'-th tracker, if needed.
            for (i in trackers.size..id) {
                val tracker: PointerTracker = PointerTracker(i)
                trackers.add(tracker)
            }

            return trackers.get(id)
        }

        fun isAnyInDraggingFinger(): Boolean {
            return sPointerTrackerQueue.isAnyInDraggingFinger()
        }

        fun cancelAllPointerTrackers() {
            sPointerTrackerQueue.cancelAllPointerTrackers()
        }

        fun setKeyboardActionListener(listener: KeyboardActionListener?) {
            sListener = listener
        }

        fun setKeyDetector(keyDetector: KeyDetector) {
            val keyboard: Keyboard? = keyDetector.getKeyboard()
            if (keyboard == null) {
                return
            }
            val trackersSize: Int = sTrackers.size
            for (i in 0 until trackersSize) {
                val tracker: PointerTracker = sTrackers.get(i)
                tracker.setKeyDetectorInner(keyDetector)
            }
            sGestureEnabler.setPasswordMode(keyboard.mId.passwordInput())
        }

        fun setReleasedKeyGraphicsToAllKeys() {
            val trackersSize: Int = sTrackers.size
            for (i in 0 until trackersSize) {
                val tracker: PointerTracker = sTrackers.get(i)
                tracker.setReleasedKeyGraphics(tracker.getKey(), true /* withAnimation */)
            }
        }

        fun dismissAllMoreKeysPanels() {
            val trackersSize: Int = sTrackers.size
            for (i in 0 until trackersSize) {
                val tracker: PointerTracker = sTrackers.get(i)
                tracker.dismissMoreKeysPanel()
            }
        }

        private fun needsToSuppressKeyPreviewPopup(eventTime: Long): Boolean {
            if (!sGestureEnabler.shouldHandleGesture()) return false
            return sTypingTimeRecorder!!.needsToSuppressKeyPreviewPopup(eventTime)
        }

        private fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
            return hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toInt()
        }

        /* package */
        fun getActivePointerTrackerCount(): Int {
            return sPointerTrackerQueue.size()
        }
    }
}
