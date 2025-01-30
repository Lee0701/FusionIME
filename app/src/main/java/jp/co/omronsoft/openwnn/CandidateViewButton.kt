/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
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

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Button

/**
 * The button for the candidate-view
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class CandidateViewButton : Button {
    /** The state of up  */
    private var mUpState: IntArray = intArrayOf()

    /** Constructor  */
    constructor(context: Context?) : super(context)

    /** Constructor  */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /** @see android.view.View.onTouchEvent
     */
    override fun onTouchEvent(me: MotionEvent): Boolean {
        /* for changing the button on CandidateView when it is pressed. */

        val ret = super.onTouchEvent(me)
        val d = background

        when (me.action) {
            MotionEvent.ACTION_DOWN -> {
                mUpState = d.state
                d.setState(PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET)
            }

            MotionEvent.ACTION_UP -> d.setState(mUpState)
            else -> d.setState(mUpState)
        }

        return ret
    }
}
