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

import android.graphics.Rect
import android.util.Log
import com.android.inputmethod.keyboard.internal.TouchPositionCorrection
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.utils.JniUtils
import java.util.Arrays
import java.util.Collections
import javax.annotation.Nonnull
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class ProximityInfo internal constructor(
    gridWidth: Int, gridHeight: Int, minWidth: Int, height: Int,
    mostCommonKeyWidth: Int, mostCommonKeyHeight: Int,
    sortedKeys: List<Key>,
    touchPositionCorrection: TouchPositionCorrection
) {
    private val mGridWidth: Int
    private val mGridHeight: Int
    private val mGridSize: Int
    private val mCellWidth: Int
    private val mCellHeight: Int

    // TODO: Find a proper name for mKeyboardMinWidth
    private val mKeyboardMinWidth: Int
    private val mKeyboardHeight: Int
    private val mMostCommonKeyWidth: Int
    private val mMostCommonKeyHeight: Int

    private val mSortedKeys: List<Key>

    private val mGridNeighbors: Array<List<Key>?>

    private var mNativeProximityInfo: Long = 0

    init {
        mGridWidth = gridWidth
        mGridHeight = gridHeight
        mGridSize = mGridWidth * mGridHeight
        mCellWidth = (minWidth + mGridWidth - 1) / mGridWidth
        mCellHeight = (height + mGridHeight - 1) / mGridHeight
        mKeyboardMinWidth = minWidth
        mKeyboardHeight = height
        mMostCommonKeyHeight = mostCommonKeyHeight
        mMostCommonKeyWidth = mostCommonKeyWidth
        mSortedKeys = sortedKeys
        mGridNeighbors = arrayOfNulls(mGridSize)
        if (minWidth == 0 || height == 0) {
            // No proximity required. Keyboard might be more keys keyboard.
        } else {
            computeNearestNeighbors()
            mNativeProximityInfo = createNativeProximityInfo(touchPositionCorrection)
        }
    }

    private fun createNativeProximityInfo(
        @Nonnull touchPositionCorrection: TouchPositionCorrection
    ): Long {
        val gridNeighborKeys: Array<List<Key>?> = mGridNeighbors
        val proximityCharsArray = IntArray(mGridSize * MAX_PROXIMITY_CHARS_SIZE)
        Arrays.fill(proximityCharsArray, Constants.NOT_A_CODE)
        for (i in 0 until mGridSize) {
            val neighborKeys: List<Key> = gridNeighborKeys[i]!!
            val proximityCharsLength: Int = neighborKeys.size
            var infoIndex: Int = i * MAX_PROXIMITY_CHARS_SIZE
            for (j in 0 until proximityCharsLength) {
                val neighborKey: Key = neighborKeys[j]
                // Excluding from proximityCharsArray
                if (!needsProximityInfo(neighborKey)) {
                    continue
                }
                proximityCharsArray[infoIndex] = neighborKey.code
                infoIndex++
            }
        }
        if (DEBUG) {
            val sb: StringBuilder = StringBuilder()
            for (i in 0 until mGridSize) {
                sb.setLength(0)
                for (j in 0 until MAX_PROXIMITY_CHARS_SIZE) {
                    val code: Int = proximityCharsArray.get(i * MAX_PROXIMITY_CHARS_SIZE + j)
                    if (code == Constants.NOT_A_CODE) {
                        break
                    }
                    if (sb.length > 0) sb.append(" ")
                    sb.append(Constants.printableCode(code))
                }
                Log.d(TAG, "proxmityChars[" + i + "]: " + sb)
            }
        }

        val sortedKeys: List<Key> = mSortedKeys
        val keyCount: Int = getProximityInfoKeysCount(sortedKeys)
        val keyXCoordinates = IntArray(keyCount)
        val keyYCoordinates = IntArray(keyCount)
        val keyWidths = IntArray(keyCount)
        val keyHeights = IntArray(keyCount)
        val keyCharCodes = IntArray(keyCount)
        val sweetSpotCenterXs: FloatArray?
        val sweetSpotCenterYs: FloatArray?
        val sweetSpotRadii: FloatArray?

        var infoIndex: Int = 0
        var keyIndex: Int = 0
        while (keyIndex < sortedKeys.size) {
            val key: Key = sortedKeys.get(keyIndex)
            // Excluding from key coordinate arrays
            if (!needsProximityInfo(key)) {
                keyIndex++
                continue
            }
            keyXCoordinates[infoIndex] = key.x
            keyYCoordinates[infoIndex] = key.y
            keyWidths[infoIndex] = key.width
            keyHeights[infoIndex] = key.height
            keyCharCodes[infoIndex] = key.code
            infoIndex++
            keyIndex++
        }

        if (touchPositionCorrection.isValid()) {
            if (DEBUG) {
                Log.d(TAG, "touchPositionCorrection: ON")
            }
            sweetSpotCenterXs = FloatArray(keyCount)
            sweetSpotCenterYs = FloatArray(keyCount)
            sweetSpotRadii = FloatArray(keyCount)
            val rows: Int = touchPositionCorrection.getRows()
            val defaultRadius: Float = (DEFAULT_TOUCH_POSITION_CORRECTION_RADIUS
                    * hypot(
                mMostCommonKeyWidth.toDouble(),
                mMostCommonKeyHeight.toDouble()
            ).toFloat())
            var infoIndex = 0
            var keyIndex = 0
            while (keyIndex < sortedKeys.size) {
                val key: Key = sortedKeys.get(keyIndex)
                // Excluding from touch position correction arrays
                if (!needsProximityInfo(key)) {
                    keyIndex++
                    continue
                }
                val hitBox: Rect = key.hitBox
                sweetSpotCenterXs[infoIndex] = hitBox.exactCenterX()
                sweetSpotCenterYs[infoIndex] = hitBox.exactCenterY()
                sweetSpotRadii[infoIndex] = defaultRadius
                val row: Int = hitBox.top / mMostCommonKeyHeight
                if (row < rows) {
                    val hitBoxWidth: Int = hitBox.width()
                    val hitBoxHeight: Int = hitBox.height()
                    val hitBoxDiagonal: Float =
                        hypot(hitBoxWidth.toDouble(), hitBoxHeight.toDouble()).toFloat()
                    sweetSpotCenterXs[infoIndex] +=
                        touchPositionCorrection.getX(row) * hitBoxWidth
                    sweetSpotCenterYs[infoIndex] +=
                        touchPositionCorrection.getY(row) * hitBoxHeight
                    sweetSpotRadii[infoIndex] =
                        touchPositionCorrection.getRadius(row) * hitBoxDiagonal
                }
                if (DEBUG) {
                    Log.d(
                        TAG, String.format(
                            "  [%2d] row=%d x/y/r=%7.2f/%7.2f/%5.2f %s code=%s",
                            infoIndex,
                            row,
                            sweetSpotCenterXs[infoIndex],
                            sweetSpotCenterYs[infoIndex],
                            sweetSpotRadii[infoIndex],
                            (if (row < rows) "correct" else "default"),
                            Constants.printableCode(key.code)
                        )
                    )
                }
                infoIndex++
                keyIndex++
            }
        } else {
            sweetSpotRadii = null
            sweetSpotCenterYs = sweetSpotRadii
            sweetSpotCenterXs = sweetSpotCenterYs
            if (DEBUG) {
                Log.d(TAG, "touchPositionCorrection: OFF")
            }
        }

        // TODO: Stop passing proximityCharsArray
        return setProximityInfoNative(
            mKeyboardMinWidth, mKeyboardHeight, mGridWidth, mGridHeight,
            mMostCommonKeyWidth, mMostCommonKeyHeight, proximityCharsArray, keyCount,
            keyXCoordinates, keyYCoordinates, keyWidths, keyHeights, keyCharCodes,
            sweetSpotCenterXs, sweetSpotCenterYs, sweetSpotRadii
        )
    }

    fun getNativeProximityInfo(): Long {
        return mNativeProximityInfo
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            if (mNativeProximityInfo != 0L) {
                releaseProximityInfoNative(mNativeProximityInfo)
                mNativeProximityInfo = 0
            }
        } finally {
//            super.finalize()
        }
    }

    private fun computeNearestNeighbors() {
        val defaultWidth: Int = mMostCommonKeyWidth
        val keyCount: Int = mSortedKeys.size
        val gridSize: Int = mGridNeighbors.size
        val threshold: Int = (defaultWidth * SEARCH_DISTANCE).toInt()
        val thresholdSquared: Int = threshold * threshold
        // Round-up so we don't have any pixels outside the grid
        val lastPixelXCoordinate: Int = mGridWidth * mCellWidth - 1
        val lastPixelYCoordinate: Int = mGridHeight * mCellHeight - 1

        // For large layouts, 'neighborsFlatBuffer' is about 80k of memory: gridSize is usually 512,
        // keycount is about 40 and a pointer to a Key is 4 bytes. This contains, for each cell,
        // enough space for as many keys as there are on the keyboard. Hence, every
        // keycount'th element is the start of a new cell, and each of these virtual subarrays
        // start empty with keycount spaces available. This fills up gradually in the loop below.
        // Since in the practice each cell does not have a lot of neighbors, most of this space is
        // actually just empty padding in this fixed-size buffer.
        val neighborsFlatBuffer: Array<Key?> = arrayOfNulls(gridSize * keyCount)
        val neighborCountPerCell = IntArray(gridSize)
        val halfCellWidth: Int = mCellWidth / 2
        val halfCellHeight: Int = mCellHeight / 2
        for (key: Key in mSortedKeys) {
            if (key.isSpacer) continue

            /* HOW WE PRE-SELECT THE CELLS (iterate over only the relevant cells, instead of all of them)

  We want to compute the distance for keys that are in the cells that are close enough to the
  key border, as this method is performance-critical. These keys are represented with 'star'
  background on the diagram below. Let's consider the Y case first.

  We want to select the cells which center falls between the top of the key minus the threshold,
  and the bottom of the key plus the threshold.
  topPixelWithinThreshold is key.mY - threshold, and bottomPixelWithinThreshold is
  key.mY + key.mHeight + threshold.

  Then we need to compute the center of the top row that we need to evaluate, as we'll iterate
  from there.

(0,0)----> x
| .-------------------------------------------.
| |   |   |   |   |   |   |   |   |   |   |   |
| |---+---+---+---+---+---+---+---+---+---+---|   .- top of top cell (aligned on the grid)
| |   |   |   |   |   |   |   |   |   |   |   |   |
| |-----------+---+---+---+---+---+---+---+---|---'                          v
| |   |   |   |***|***|*_________________________ topPixelWithinThreshold    | yDeltaToGrid
| |---+---+---+-----^-+-|-+---+---+---+---+---|                              ^
| |   |   |   |***|*|*|*|*|***|***|   |   |   |           ______________________________________
v |---+---+--threshold--|-+---+---+---+---+---|          |
  |   |   |   |***|*|*|*|*|***|***|   |   |   |          | Starting from key.mY, we substract
y |---+---+---+---+-v-+-|-+---+---+---+---+---|          | thresholdBase and get the top pixel
  |   |   |   |***|**########------------------- key.mY  | within the threshold. We align that on
  |---+---+---+---+--#+---+-#-+---+---+---+---|          | the grid by computing the delta to the
  |   |   |   |***|**#|***|*#*|***|   |   |   |          | grid, and get the top of the top cell.
  |---+---+---+---+--#+---+-#-+---+---+---+---|          |
  |   |   |   |***|**########*|***|   |   |   |          | Adding half the cell height to the top
  |---+---+---+---+---+-|-+---+---+---+---+---|          | of the top cell, we get the middle of
  |   |   |   |***|***|*|*|***|***|   |   |   |          | the top cell (yMiddleOfTopCell).
  |---+---+---+---+---+-|-+---+---+---+---+---|          |
  |   |   |   |***|***|*|*|***|***|   |   |   |          |
  |---+---+---+---+---+-|________________________ yEnd   | Since we only want to add the key to
  |   |   |   |   |   |   | (bottomPixelWithinThreshold) | the proximity if it's close enough to
  |---+---+---+---+---+---+---+---+---+---+---|          | the center of the cell, we only need
  |   |   |   |   |   |   |   |   |   |   |   |          | to compute for these cells where
  '---'---'---'---'---'---'---'---'---'---'---'          | topPixelWithinThreshold is above the
                                        (positive x,y)   | center of the cell. This is the case
                                                         | when yDeltaToGrid is less than half
  [Zoomed in diagram]                                    | the height of the cell.
  +-------+-------+-------+-------+-------+              |
  |       |       |       |       |       |              | On the zoomed in diagram, on the right
  |       |       |       |       |       |              | the topPixelWithinThreshold (represented
  |       |       |       |       |       |      top of  | with an = sign) is below and we can skip
  +-------+-------+-------+--v----+-------+ .. top cell  | this cell, while on the left it's above
  |       | = topPixelWT  |  |  yDeltaToGrid             | and we need to compute for this cell.
  |..yStart.|.....|.......|..|....|.......|... y middle  | Thus, if yDeltaToGrid is more than half
  |   (left)|     |       |  ^ =  |       | of top cell  | the height of the cell, we start the
  +-------+-|-----+-------+----|--+-------+              | iteration one cell below the top cell,
  |       | |     |       |    |  |       |              | else we start it on the top cell. This
  |.......|.|.....|.......|....|..|.....yStart (right)   | is stored in yStart.

  Since we only want to go up to bottomPixelWithinThreshold, and we only iterate on the center
  of the keys, we can stop as soon as the y value exceeds bottomPixelThreshold, so we don't
  have to align this on the center of the key. Hence, we don't need a separate value for
  bottomPixelWithinThreshold and call this yEnd right away.
*/
            val keyX: Int = key.x
            val keyY: Int = key.y
            val topPixelWithinThreshold: Int = keyY - threshold
            val yDeltaToGrid: Int = topPixelWithinThreshold % mCellHeight
            val yMiddleOfTopCell: Int = topPixelWithinThreshold - yDeltaToGrid + halfCellHeight
            val yStart: Int = max(
                halfCellHeight.toDouble(),
                (yMiddleOfTopCell + (if (yDeltaToGrid <= halfCellHeight) 0 else mCellHeight)).toDouble()
            ).toInt()
            val yEnd: Int = min(
                lastPixelYCoordinate.toDouble(),
                (keyY + key.height + threshold).toDouble()
            ).toInt()

            val leftPixelWithinThreshold: Int = keyX - threshold
            val xDeltaToGrid: Int = leftPixelWithinThreshold % mCellWidth
            val xMiddleOfLeftCell: Int = leftPixelWithinThreshold - xDeltaToGrid + halfCellWidth
            val xStart: Int = max(
                halfCellWidth.toDouble(),
                (xMiddleOfLeftCell + (if (xDeltaToGrid <= halfCellWidth) 0 else mCellWidth)).toDouble()
            ).toInt()
            val xEnd: Int = min(
                lastPixelXCoordinate.toDouble(),
                (keyX + key.width + threshold).toDouble()
            ).toInt()

            var baseIndexOfCurrentRow: Int =
                (yStart / mCellHeight) * mGridWidth + (xStart / mCellWidth)
            var centerY: Int = yStart
            while (centerY <= yEnd) {
                var index: Int = baseIndexOfCurrentRow
                var centerX: Int = xStart
                while (centerX <= xEnd) {
                    if (key.squaredDistanceToEdge(centerX, centerY) < thresholdSquared) {
                        neighborsFlatBuffer[index * keyCount + neighborCountPerCell[index]] =
                            key
                        ++neighborCountPerCell[index]
                    }
                    ++index
                    centerX += mCellWidth
                }
                baseIndexOfCurrentRow += mGridWidth
                centerY += mCellHeight
            }
        }

        for (i in 0 until gridSize) {
            val indexStart: Int = i * keyCount
            val indexEnd: Int = indexStart + neighborCountPerCell.get(i)
            val neighbors: ArrayList<Key> = ArrayList(indexEnd - indexStart)
            for (index in indexStart until indexEnd) {
                neighbors.add(neighborsFlatBuffer[index]!!)
            }
            mGridNeighbors[i] = Collections.unmodifiableList(neighbors)
        }
    }

    fun fillArrayWithNearestKeyCodes(
        x: Int, y: Int, primaryKeyCode: Int,
        dest: IntArray
    ) {
        val destLength: Int = dest.size
        if (destLength < 1) {
            return
        }
        var index: Int = 0
        if (primaryKeyCode > Constants.CODE_SPACE) {
            dest[index++] = primaryKeyCode
        }
        val nearestKeys: List<Key> = getNearestKeys(x, y)
        for (key: Key in nearestKeys) {
            if (index >= destLength) {
                break
            }
            val code: Int = key.code
            if (code <= Constants.CODE_SPACE) {
                break
            }
            dest[index++] = code
        }
        if (index < destLength) {
            dest[index] = Constants.NOT_A_CODE
        }
    }

    @Nonnull
    fun getNearestKeys(x: Int, y: Int): List<Key> {
        if (x in 0..<mKeyboardMinWidth && y in 0..<mKeyboardHeight) {
            val index: Int = (y / mCellHeight) * mGridWidth + (x / mCellWidth)
            if (index < mGridSize) {
                return mGridNeighbors[index]!!
            }
        }
        return EMPTY_KEY_LIST
    }

    companion object {
        private val TAG: String = ProximityInfo::class.java.getSimpleName()
        private const val DEBUG: Boolean = false

        // Must be equal to MAX_PROXIMITY_CHARS_SIZE in native/jni/src/defines.h
        const val MAX_PROXIMITY_CHARS_SIZE: Int = 16

        /** Number of key widths from current touch point to search for nearest keys.  */
        private const val SEARCH_DISTANCE: Float = 1.2f

        @Nonnull
        private val EMPTY_KEY_LIST: List<Key> = emptyList()
        private const val DEFAULT_TOUCH_POSITION_CORRECTION_RADIUS: Float = 0.15f

        init {
            JniUtils.loadNativeLibrary()
        }

        // TODO: Stop passing proximityCharsArray
        @JvmStatic
        private external fun setProximityInfoNative(
            displayWidth: Int,
            displayHeight: Int,
            gridWidth: Int,
            gridHeight: Int,
            mostCommonKeyWidth: Int,
            mostCommonKeyHeight: Int,
            proximityCharsArray: IntArray,
            keyCount: Int,
            keyXCoordinates: IntArray,
            keyYCoordinates: IntArray,
            keyWidths: IntArray,
            keyHeights: IntArray,
            keyCharCodes: IntArray,
            sweetSpotCenterXs: FloatArray?,
            sweetSpotCenterYs: FloatArray?,
            sweetSpotRadii: FloatArray?
        ): Long

        @JvmStatic
        private external fun releaseProximityInfoNative(nativeProximityInfo: Long)

        @JvmStatic
        fun needsProximityInfo(key: Key): Boolean {
            // Don't include special keys into ProximityInfo.
            return key.code >= Constants.CODE_SPACE
        }

        private fun getProximityInfoKeysCount(keys: List<Key>): Int {
            var count: Int = 0
            for (key: Key in keys) {
                if (needsProximityInfo(key)) {
                    count++
                }
            }
            return count
        }
    }
}
