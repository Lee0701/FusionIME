/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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
package jp.co.omronsoft.openwnn

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.WnnWord

/**
 * The default candidates view manager using [TextView].
 *
 * @author Copyright (C) 2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class CandidateTextView : TextView {
    /** Maximum width of candidate view  */
    private var mMaxWidth = 0

    /** Width of fontsize change beginning  */
    private var mChangeFontSize = 0

    /** Minimum width of candidate view  */
    private var mCandidateMinimumWidth = 0

    /** Alert dialog  */
    private var mCandidateDialog: Dialog? = null

    /**
     * Constructor
     * @param context    context
     */
    constructor(context: Context?) : super(context) {
        isSoundEffectsEnabled = false
    }

    /**
     * Constructor
     * @param context    context
     * @param candidateMinimumHeight Minimum height of candidate view
     * @param candidateMinimumWidth  Minimum width of candidate view
     * @param maxWidth  Maximum width of candidate view
     */
    constructor(
        context: Context?,
        candidateMinimumHeight: Int,
        candidateMinimumWidth: Int,
        maxWidth: Int
    ) : super(context) {
        isSoundEffectsEnabled = false
        setTextColor(resources.getColor(R.color.candidate_text_1line))
        setBackgroundResource(R.drawable.cand_back_1line)
        gravity = Gravity.CENTER
        setSingleLine()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1.0f
        )
        minHeight = candidateMinimumHeight
        minimumWidth = candidateMinimumWidth
        mCandidateMinimumWidth = candidateMinimumWidth
        mMaxWidth = maxWidth
        mChangeFontSize = maxWidth - CHANGE_FONTSIZE_WIDTH
    }

    /**
     * Textview is set to the best content for the display of candidate.
     * @param WnnWord    candidate
     * @param wordCount  candidate id
     * @param OnClickListener Operation when clicking
     * @param OnClickListener Operation when longclicking
     * @return Set completion textview
     */
    fun setCandidateTextView(
        word: WnnWord, wordCount: Int,
        candidateOnClick: OnClickListener?,
        candidateOnLongClick: OnLongClickListener?
    ): CandidateTextView {
        textSize = CUSTOM_FONTSIZE[0]
        text = word.candidate
        id = wordCount
        visibility = VISIBLE
        isPressed = false
        width = 0
        ellipsize = null
        setOnClickListener(candidateOnClick)
        setOnLongClickListener(candidateOnLongClick)
        setCustomCandidate(word)
        return this
    }

    /**
     * If the text view is set to the best width for the display,
     * and it is necessary, the character is shortened.
     * @param WnnWord candidate word
     * @return int    textview width
     */
    fun setCustomCandidate(word: WnnWord): Int {
        val paint = paint
        var width = paint.measureText(word.candidate, 0, word.candidate!!.length).toInt()
        width += paddingLeft + paddingRight

        if (width > mCandidateMinimumWidth) {
            var i = 0
            while (i < WIDTH_SIZE.size) {
                if (width > mChangeFontSize + WIDTH_SIZE[i]) {
                    textSize = CUSTOM_FONTSIZE[i]
                }
                i++
            }

            width = paint.measureText(word.candidate, 0, word.candidate!!.length).toInt()
            width += paddingLeft + paddingRight

            if (width >= mMaxWidth) {
                setWidth(mMaxWidth)
                width = mMaxWidth
                ellipsize = TextUtils.TruncateAt.START
            } else {
                setWidth(width)
            }
        } else {
            setWidth(mCandidateMinimumWidth)
            width = mCandidateMinimumWidth
        }
        return width
    }

    /** @see View.setBackgroundDrawable
     */
    override fun setBackgroundDrawable(d: Drawable) {
        super.setBackgroundDrawable(d)
        setPadding(20, 0, 20, 0)
    }

    /**
     * Display Dialog.
     *
     * @param builder  The Dialog builder,
     */
    fun displayCandidateDialog(builder: Dialog?) {
        if (mCandidateDialog != null) {
            mCandidateDialog!!.dismiss()
            mCandidateDialog = null
        }
        mCandidateDialog = builder
        val window = mCandidateDialog!!.window
        val lp = window!!.attributes
        lp.token = windowToken
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window.attributes = lp
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        mCandidateDialog!!.show()
    }

    /** @see android.view.View.onWindowVisibilityChanged
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if ((visibility != VISIBLE) && (mCandidateDialog != null)) {
            mCandidateDialog!!.dismiss()
        }
    }

    companion object {
        /** Width of fontsize change  */
        private val WIDTH_SIZE = intArrayOf(0, 50, 100)

        /** Fontsize corresponding to width  */
        private val CUSTOM_FONTSIZE = floatArrayOf(20.0f, 17.0f, 15.0f)

        /** Width of fontsize change beginning  */
        private const val CHANGE_FONTSIZE_WIDTH = 120
    }
}
