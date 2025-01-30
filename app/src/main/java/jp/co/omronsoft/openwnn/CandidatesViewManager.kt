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
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ee.oyatl.ime.fusion.R

/**
 * The interface of candidates view manager used by [OpenWnn].
 *
 * @author Copyright (C) 2008-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
abstract class CandidatesViewManager {
    /** The view of the LongPressDialog  */
    protected var mViewLongPressDialog: View? = null

    /** Whether candidates long click enable  */
    protected var mDialog: Dialog? = null

    /** The word pressed  */
    protected var mWord: WnnWord? = null

    /**
     * Initialize the candidates view.
     *
     * @param parent    The OpenWnn object
     * @param width     The width of the display
     * @param height    The height of the display
     *
     * @return The candidates view created in the initialize process; `null` if cannot create a candidates view.
     */
    abstract fun initView(parent: OpenWnn, width: Int, height: Int): View?

    /**
     * Get the candidates view being used currently.
     *
     * @return The candidates view; `null` if no candidates view is used currently.
     */
    abstract val currentView: View?

    /**
     * Get the candidates view type.
     *
     * @return      The view type,
     * from [CandidatesViewManager.VIEW_TYPE_NORMAL] to
     * [CandidatesViewManager.VIEW_TYPE_CLOSE]
     */
    /**
     * Set the candidates view type.
     *
     * @param type  The candidate view type,
     * from [CandidatesViewManager.VIEW_TYPE_NORMAL] to
     * [CandidatesViewManager.VIEW_TYPE_CLOSE]
     */
    abstract var viewType: Int

    /**
     * Display candidates.
     *
     * @param converter  The [WnnEngine] from which [CandidatesViewManager] gets the candidates
     *
     * @see jp.co.omronsoft.openwnn.WnnEngine.getNextCandidate
     */
    abstract fun displayCandidates(converter: WnnEngine?)

    /**
     * Clear and hide the candidates view.
     */
    abstract fun clearCandidates()

    /**
     * Replace the preferences in the candidates view.
     *
     * @param pref    The preferences
     */
    abstract fun setPreferences(pref: SharedPreferences)

    /**
     * KeyEvent action for soft key board.
     *
     * @param key    Key event
     */
    abstract fun processMoveKeyEvent(key: Int)

    /**
     * Get candidate is focused now.
     *
     * @return the Candidate is focused of a flag.
     */
    abstract val isFocusCandidate: Boolean

    /**
     * Select candidate that has focus.
     */
    abstract fun selectFocusCandidate()

    /**
     * MSG_SET_CANDIDATES removeMessages.
     */
    abstract fun setCandidateMsgRemove()

    /**
     * Display Dialog.
     *
     * @param view  View,
     * @param word  Display word,
     */
    protected fun displayDialog(view: View, word: WnnWord) {
        if ((view is CandidateTextView) && (null != mViewLongPressDialog)) {
            closeDialog()
            mDialog = Dialog(view.getContext(), R.style.Dialog)

            val text =
                mViewLongPressDialog!!.findViewById<View>(R.id.candidate_longpress_dialog_text) as TextView
            text.text = word.candidate

            mDialog!!.setContentView(mViewLongPressDialog!!)
            view.displayCandidateDialog(mDialog)
        }
    }

    /**
     * Close Dialog.
     *
     */
    fun closeDialog() {
        if (null != mDialog) {
            mDialog!!.dismiss()
            mDialog = null
            if (null != mViewLongPressDialog) {
                val parent = mViewLongPressDialog!!.parent as ViewGroup
                parent?.removeView(mViewLongPressDialog)
            }
        }
    }

    companion object {
        /** Size of candidates view (normal)  */
        const val VIEW_TYPE_NORMAL: Int = 0

        /** Size of candidates view (full)  */
        const val VIEW_TYPE_FULL: Int = 1

        /** Size of candidates view (close/non-display)  */
        const val VIEW_TYPE_CLOSE: Int = 2

        /**
         * Attribute of a word (no attribute)
         * @see jp.co.omronsoft.openwnn.WnnWord
         */
        const val ATTRIBUTE_NONE: Int = 0

        /**
         * Attribute of a word (a candidate in the history list)
         * @see jp.co.omronsoft.openwnn.WnnWord
         */
        const val ATTRIBUTE_HISTORY: Int = 1

        /**
         * Attribute of a word (the best candidate)
         * @see jp.co.omronsoft.openwnn.WnnWord
         */
        const val ATTRIBUTE_BEST: Int = 2

        /**
         * Attribute of a word (auto generated/not in the dictionary)
         * @see jp.co.omronsoft.openwnn.WnnWord
         */
        const val ATTRIBUTE_AUTO_GENERATED: Int = 4
    }
}
