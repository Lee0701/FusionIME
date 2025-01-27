/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.inputmethod.keyboard.internal

import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class RoundedLine {
    private val mArc1: RectF = RectF()
    private val mArc2: RectF = RectF()
    private val mPath: Path = Path()

    /**
     * Make a rounded line path
     *
     * @param p1x the x-coordinate of the start point.
     * @param p1y the y-coordinate of the start point.
     * @param r1 the radius at the start point
     * @param p2x the x-coordinate of the end point.
     * @param p2y the y-coordinate of the end point.
     * @param r2 the radius at the end point
     * @return an instance of [Path] that holds the result rounded line, or an instance of
     * [Path] that holds an empty path if the start and end points are equal.
     */
    fun makePath(
        p1x: Float, p1y: Float, r1: Float,
        p2x: Float, p2y: Float, r2: Float
    ): Path {
        mPath.rewind()
        val dx: Double = (p2x - p1x).toDouble()
        val dy: Double = (p2y - p1y).toDouble()
        // Distance of the points.
        val l: Double = hypot(dx, dy)
        if (java.lang.Double.compare(0.0, l) == 0) {
            return mPath // Return an empty path
        }
        // Angle of the line p1-p2
        val a: Double = atan2(dy, dx)
        // Difference of trail cap radius.
        val dr: Double = (r2 - r1).toDouble()
        // Variation of angle at trail cap.
        val ar: Double = asin(dr / l)
        // The start angle of trail cap arc at P1.
        val aa: Double = a - (RIGHT_ANGLE + ar)
        // The end angle of trail cap arc at P2.
        val ab: Double = a + (RIGHT_ANGLE + ar)
        val cosa: Float = cos(aa) as Float
        val sina: Float = sin(aa) as Float
        val cosb: Float = cos(ab) as Float
        val sinb: Float = sin(ab) as Float
        // Closing point of arc at P1.
        val p1ax: Float = p1x + r1 * cosa
        val p1ay: Float = p1y + r1 * sina
        // Opening point of arc at P1.
        val p1bx: Float = p1x + r1 * cosb
        val p1by: Float = p1y + r1 * sinb
        // Opening point of arc at P2.
        val p2ax: Float = p2x + r2 * cosa
        val p2ay: Float = p2y + r2 * sina
        // Closing point of arc at P2.
        val p2bx: Float = p2x + r2 * cosb
        val p2by: Float = p2y + r2 * sinb
        // Start angle of the trail arcs.
        val angle: Float = (aa * RADIAN_TO_DEGREE).toFloat()
        val ar2degree: Float = (ar * 2.0 * RADIAN_TO_DEGREE).toFloat()
        // Sweep angle of the trail arc at P1.
        val a1: Float = -180.0f + ar2degree
        // Sweep angle of the trail arc at P2.
        val a2: Float = 180.0f + ar2degree
        mArc1.set(p1x, p1y, p1x, p1y)
        mArc1.inset(-r1, -r1)
        mArc2.set(p2x, p2y, p2x, p2y)
        mArc2.inset(-r2, -r2)

        // Trail cap at P1.
        mPath.moveTo(p1x, p1y)
        mPath.arcTo(mArc1, angle, a1)
        // Trail cap at P2.
        mPath.moveTo(p2x, p2y)
        mPath.arcTo(mArc2, angle, a2)
        // Two trapezoids connecting P1 and P2.
        mPath.moveTo(p1ax, p1ay)
        mPath.lineTo(p1x, p1y)
        mPath.lineTo(p1bx, p1by)
        mPath.lineTo(p2bx, p2by)
        mPath.lineTo(p2x, p2y)
        mPath.lineTo(p2ax, p2ay)
        mPath.close()
        return mPath
    }

    fun getBounds(outBounds: Rect) {
        // Reuse mArc1 as working variable
        mPath.computeBounds(mArc1, true /* unused */)
        mArc1.roundOut(outBounds)
    }

    companion object {
        private val RADIAN_TO_DEGREE: Double = 180.0 / Math.PI
        private val RIGHT_ANGLE: Double = Math.PI / 2.0
    }
}
