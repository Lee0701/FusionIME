/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.latin.utils

import android.annotation.TargetApi
import android.graphics.Matrix
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build.VERSION_CODES
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.widget.TextView
import com.android.inputmethod.compat.BuildCompatUtils
import com.android.inputmethod.compat.CursorAnchorInfoCompatWrapper
import javax.annotation.Nonnull
import kotlin.math.max
import kotlin.math.min

/**
 * This class allows input methods to extract [CursorAnchorInfo] directly from the given
 * [TextView]. This is useful and even necessary to support full-screen mode where the default
 * [InputMethodService.onUpdateCursorAnchorInfo] event callback must be
 * ignored because it reports the character locations of the target application rather than
 * characters on [ExtractEditText].
 */
object CursorAnchorInfoUtils {
    private fun isPositionVisible(
        view: View, positionX: Float,
        positionY: Float
    ): Boolean {
        val position = floatArrayOf(positionX, positionY)
        var currentView: View? = view

        while (currentView != null) {
            if (currentView !== view) {
                // Local scroll is already taken into account in positionX/Y
                position[0] -= currentView.scrollX.toFloat()
                position[1] -= currentView.scrollY.toFloat()
            }

            if (position[0] < 0 || position[1] < 0 || position[0] > currentView.width || position[1] > currentView.height) {
                return false
            }

            if (!currentView.matrix.isIdentity) {
                currentView.matrix.mapPoints(position)
            }

            position[0] += currentView.left.toFloat()
            position[1] += currentView.top.toFloat()

            val parent = currentView.parent
            currentView = if (parent is View) {
                parent
            } else {
                // We've reached the ViewRoot, stop iterating
                null
            }
        }

        // We've been able to walk up the view hierarchy and the position was never clipped
        return true
    }

    /**
     * Extracts [CursorAnchorInfoCompatWrapper] from the given [TextView].
     * @param textView the target text view from which [CursorAnchorInfoCompatWrapper] is to
     * be extracted.
     * @return the [CursorAnchorInfoCompatWrapper] object based on the current layout.
     * `null` if `Build.VERSION.SDK_INT` is 20 or prior or [TextView] is not
     * ready to provide layout information.
     */
    fun extractFromTextView(
        @Nonnull textView: TextView
    ): CursorAnchorInfoCompatWrapper? {
        if (BuildCompatUtils.EFFECTIVE_SDK_INT < VERSION_CODES.LOLLIPOP) {
            return null
        }
        return CursorAnchorInfoCompatWrapper.Companion.wrap(extractFromTextViewInternal(textView))
    }

    /**
     * Returns [CursorAnchorInfo] from the given [TextView].
     * @param textView the target text view from which [CursorAnchorInfo] is to be extracted.
     * @return the [CursorAnchorInfo] object based on the current layout. `null` if it
     * is not feasible.
     */
    @TargetApi(VERSION_CODES.LOLLIPOP)
    private fun extractFromTextViewInternal(@Nonnull textView: TextView): CursorAnchorInfo? {
        val layout = textView.layout ?: return null

        val builder = CursorAnchorInfo.Builder()

        val selectionStart = textView.selectionStart
        builder.setSelectionRange(selectionStart, textView.selectionEnd)

        // Construct transformation matrix from view local coordinates to screen coordinates.
        val viewToScreenMatrix = Matrix(textView.matrix)
        val viewOriginInScreen = IntArray(2)
        textView.getLocationOnScreen(viewOriginInScreen)
        viewToScreenMatrix.postTranslate(
            viewOriginInScreen[0].toFloat(),
            viewOriginInScreen[1].toFloat()
        )
        builder.setMatrix(viewToScreenMatrix)

        if (layout.lineCount == 0) {
            return null
        }
        val lineBoundsWithoutOffset = Rect()
        val lineBoundsWithOffset = Rect()
        layout.getLineBounds(0, lineBoundsWithoutOffset)
        textView.getLineBounds(0, lineBoundsWithOffset)
        val viewportToContentHorizontalOffset = (lineBoundsWithOffset.left
                - lineBoundsWithoutOffset.left - textView.scrollX).toFloat()
        val viewportToContentVerticalOffset = (lineBoundsWithOffset.top
                - lineBoundsWithoutOffset.top - textView.scrollY).toFloat()

        val text = textView.text
        if (text is Spannable) {
            // Here we assume that the composing text is marked as SPAN_COMPOSING flag. This is not
            // necessarily true, but basically works.
            var composingTextStart = text.length
            var composingTextEnd = 0
            val spannable = text
            val spans = spannable.getSpans(
                0, text.length,
                Any::class.java
            )
            for (span in spans) {
                val spanFlag = spannable.getSpanFlags(span)
                if ((spanFlag and Spanned.SPAN_COMPOSING) != 0) {
                    composingTextStart = min(
                        composingTextStart.toDouble(),
                        spannable.getSpanStart(span).toDouble()
                    ).toInt()
                    composingTextEnd =
                        max(composingTextEnd.toDouble(), spannable.getSpanEnd(span).toDouble())
                            .toInt()
                }
            }

            val hasComposingText =
                (0 <= composingTextStart) && (composingTextStart < composingTextEnd)
            if (hasComposingText) {
                val composingText = text.subSequence(
                    composingTextStart,
                    composingTextEnd
                )
                builder.setComposingText(composingTextStart, composingText)

                val minLine = layout.getLineForOffset(composingTextStart)
                val maxLine = layout.getLineForOffset(composingTextEnd - 1)
                for (line in minLine..maxLine) {
                    val lineStart = layout.getLineStart(line)
                    val lineEnd = layout.getLineEnd(line)
                    val offsetStart =
                        max(lineStart.toDouble(), composingTextStart.toDouble()).toInt()
                    val offsetEnd =
                        min(lineEnd.toDouble(), composingTextEnd.toDouble()).toInt()
                    val ltrLine =
                        layout.getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT
                    val widths = FloatArray(offsetEnd - offsetStart)
                    layout.paint.getTextWidths(text, offsetStart, offsetEnd, widths)
                    val top = layout.getLineTop(line).toFloat()
                    val bottom = layout.getLineBottom(line).toFloat()
                    for (offset in offsetStart until offsetEnd) {
                        val charWidth = widths[offset - offsetStart]
                        val isRtl = layout.isRtlCharAt(offset)
                        val primary = layout.getPrimaryHorizontal(offset)
                        val secondary = layout.getSecondaryHorizontal(offset)
                        // TODO: This doesn't work perfectly for text with custom styles and TAB
                        // chars.
                        val left: Float
                        val right: Float
                        if (ltrLine) {
                            if (isRtl) {
                                left = secondary - charWidth
                                right = secondary
                            } else {
                                left = primary
                                right = primary + charWidth
                            }
                        } else {
                            if (!isRtl) {
                                left = secondary
                                right = secondary + charWidth
                            } else {
                                left = primary - charWidth
                                right = primary
                            }
                        }
                        // TODO: Check top-right and bottom-left as well.
                        val localLeft = left + viewportToContentHorizontalOffset
                        val localRight = right + viewportToContentHorizontalOffset
                        val localTop = top + viewportToContentVerticalOffset
                        val localBottom = bottom + viewportToContentVerticalOffset
                        val isTopLeftVisible = isPositionVisible(
                            textView,
                            localLeft, localTop
                        )
                        val isBottomRightVisible =
                            isPositionVisible(textView, localRight, localBottom)
                        var characterBoundsFlags = 0
                        if (isTopLeftVisible || isBottomRightVisible) {
                            characterBoundsFlags =
                                characterBoundsFlags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
                        }
                        if (!isTopLeftVisible || !isBottomRightVisible) {
                            characterBoundsFlags =
                                characterBoundsFlags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
                        }
                        if (isRtl) {
                            characterBoundsFlags =
                                characterBoundsFlags or CursorAnchorInfo.FLAG_IS_RTL
                        }
                        // Here offset is the index in Java chars.
                        builder.addCharacterBounds(
                            offset, localLeft, localTop, localRight,
                            localBottom, characterBoundsFlags
                        )
                    }
                }
            }
        }

        // Treat selectionStart as the insertion point.
        if (0 <= selectionStart) {
            val offset = selectionStart
            val line = layout.getLineForOffset(offset)
            val insertionMarkerX = (layout.getPrimaryHorizontal(offset)
                    + viewportToContentHorizontalOffset)
            val insertionMarkerTop = (layout.getLineTop(line)
                    + viewportToContentVerticalOffset)
            val insertionMarkerBaseline = (layout.getLineBaseline(line)
                    + viewportToContentVerticalOffset)
            val insertionMarkerBottom = (layout.getLineBottom(line)
                    + viewportToContentVerticalOffset)
            val isTopVisible =
                isPositionVisible(textView, insertionMarkerX, insertionMarkerTop)
            val isBottomVisible =
                isPositionVisible(textView, insertionMarkerX, insertionMarkerBottom)
            var insertionMarkerFlags = 0
            if (isTopVisible || isBottomVisible) {
                insertionMarkerFlags =
                    insertionMarkerFlags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
            }
            if (!isTopVisible || !isBottomVisible) {
                insertionMarkerFlags =
                    insertionMarkerFlags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
            }
            if (layout.isRtlCharAt(offset)) {
                insertionMarkerFlags = insertionMarkerFlags or CursorAnchorInfo.FLAG_IS_RTL
            }
            builder.setInsertionMarkerLocation(
                insertionMarkerX, insertionMarkerTop,
                insertionMarkerBaseline, insertionMarkerBottom, insertionMarkerFlags
            )
        }
        return builder.build()
    }
}
