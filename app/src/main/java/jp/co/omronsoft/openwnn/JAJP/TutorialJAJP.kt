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
/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jp.co.omronsoft.openwnn.JAJP

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.PopupWindow
import android.widget.TextView
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.OpenWnnJAJP
import kotlin.math.max

class TutorialJAJP(
    private val mIme: OpenWnnJAJP,
    private val mInputView: View,
    private val mInputManager: DefaultSoftKeyboardJAJP
) :
    OnTouchListener {
    private val mBubbles: MutableList<Bubble> = ArrayList()
    private val mLocation = IntArray(2)
    private var mBubbleIndex = 0
    private var mEnableKeyTouch = false

    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SHOW_BUBBLE -> {
                    val bubba = msg.obj as Bubble
                    bubba.show(mLocation[0], mLocation[1])
                }
            }
        }
    }

    internal inner class Bubble {
        var bubbleBackground: Drawable? = null
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
        var gravity: Int = 0
        var text: CharSequence? = null
        var dismissOnTouch: Boolean = false
        var dismissOnClose: Boolean = false
        var window: PopupWindow? = null
        var textView: TextView? = null
        var inputView: View? = null

        constructor(
            context: Context, inputView: View,
            backgroundResource: Int, bx: Int, by: Int, description: Int, guide: Int
        ) {
            val text = context.resources.getText(description)
            init(context, inputView, backgroundResource, bx, by, text, guide, false)
        }

        constructor(
            context: Context, inputView: View, backgroundResource: Int, bx: Int, by: Int,
            description: CharSequence?, guide: Int, leftAlign: Boolean
        ) {
            init(context, inputView, backgroundResource, bx, by, description, guide, leftAlign)
        }

        fun init(
            context: Context, inputView: View, backgroundResource: Int,
            bx: Int, by: Int, description: CharSequence?, guide: Int, leftAlign: Boolean
        ) {
            bubbleBackground = context.resources.getDrawable(backgroundResource)
            x = bx
            y = by
            width = (inputView.width * 0.9).toInt()
            this.gravity = Gravity.TOP or Gravity.LEFT
            text = SpannableStringBuilder()
                .append(description)
                .append("\n")
                .append(context.resources.getText(guide))
            this.dismissOnTouch = true
            this.dismissOnClose = false
            this.inputView = inputView
            window = PopupWindow(context)
            window!!.setBackgroundDrawable(null)
            val inflate =
                context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            textView = inflate.inflate(R.layout.bubble_text, null) as TextView
            textView!!.setBackgroundDrawable(bubbleBackground)
            textView!!.text = text
            if (leftAlign) {
                textView!!.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            }
            window!!.contentView = textView
            window!!.isFocusable = false
            window!!.isTouchable = true
            window!!.isOutsideTouchable = false
        }

        private fun chooseSize(
            pop: PopupWindow,
            parentView: View?,
            text: CharSequence?,
            tv: TextView
        ): Int {
            val wid = tv.paddingLeft + tv.paddingRight
            val ht = tv.paddingTop + tv.paddingBottom

            /*
             * Figure out how big the text would be if we laid it out to the
             * full width of this view minus the border.
             */
            val cap = width - wid

            val l: Layout = StaticLayout(
                text, tv.paint, cap,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true
            )
            var max = 0f
            for (i in 0 until l.lineCount) {
                max = max(max.toDouble(), l.getLineWidth(i).toDouble()).toFloat()
            }

            /*
             * Now set the popup size to be big enough for the text plus the border.
             */
            pop.width = width
            pop.height = ht + l.height
            return l.height
        }

        fun show(offx: Int, offy: Int) {
            var offx = offx
            var offy = offy
            val textHeight = chooseSize(window!!, inputView, text, textView!!)
            offy -= textView!!.paddingTop + textHeight
            if (inputView!!.visibility == View.VISIBLE
                && inputView!!.windowVisibility == View.VISIBLE
            ) {
                try {
                    if ((gravity and Gravity.BOTTOM) == Gravity.BOTTOM) offy -= window!!.height
                    if ((gravity and Gravity.RIGHT) == Gravity.RIGHT) offx -= window!!.width
                    textView!!.setOnTouchListener { view, me ->
                        val ret = !mEnableKeyTouch
                        when (me.action) {
                            MotionEvent.ACTION_UP -> if (mBubbleIndex >= mBubbles.size) {
                                mInputView.setOnTouchListener(null)
                            } else {
                                this@TutorialJAJP.next()
                            }

                            else -> {}
                        }
                        ret
                    }
                    window!!.showAtLocation(inputView, Gravity.NO_GRAVITY, x + offx, y + offy)
                } catch (e: Exception) {
                }
            }
        }

        fun hide() {
            if (window!!.isShowing) {
                textView!!.setOnTouchListener(null)
                window!!.dismiss()
            }
        }

        val isShowing: Boolean
            get() = window!!.isShowing
    }

    /** Constructor  */
    init {
        val context = mInputView.context
        val inputWidth = mInputView.width
        val r = mInputView.context.resources
        val x = inputWidth / 20
        r.getDimensionPixelOffset(R.dimen.bubble_pointer_offset)

        val spannable = SpannableStringBuilder()
        var button: Bubble

        spannable.clear()
        spannable.append(r.getText(R.string.tip_to_step1))

        setSpan(spannable, "\u25cb", R.drawable.tutorial_12key_key)
        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, false
        )
        mBubbles.add(button)

        spannable.clear()
        spannable.append(r.getText(R.string.tip_to_step2_a))

        setSpan(spannable, "\u25cb", R.drawable.tutorial_12key_toggle)
        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, true
        )
        mBubbles.add(button)

        spannable.append(r.getText(R.string.tip_to_step2_b))

        setSpan(spannable, "\u2192", R.drawable.tutorial_12key_right)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, true
        )
        mBubbles.add(button)

        spannable.append(r.getText(R.string.tip_to_step2_c))

        setSpan(spannable, "\u25cb", R.drawable.tutorial_12key_toggle)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, true
        )
        mBubbles.add(button)

        spannable.append(r.getText(R.string.tip_to_step2_d))

        setSpan(spannable, "\u25a0", R.drawable.tutorial_12key_space_jp)

        setSpan(spannable, "\u2193", R.drawable.tutorial_12key_enter)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, true
        )
        mBubbles.add(button)

        spannable.clear()
        spannable.append(r.getText(R.string.tip_to_step3_a))

        setSpan(spannable, "\u25a0", R.drawable.tutorial_12key_mode)
        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble_moji, x, 0,
            spannable, R.string.touch_to_continue, false
        )
        mBubbles.add(button)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble_moji, x, 0,
            R.string.tip_to_step3_b, R.string.touch_to_continue
        )
        mBubbles.add(button)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble_moji, x, 0,
            R.string.tip_to_step3_c, R.string.touch_to_continue
        )
        mBubbles.add(button)

        spannable.clear()
        spannable.append(r.getText(R.string.tip_to_step4))

        setSpan(spannable, "\u25a0", R.drawable.tutorial_12key_mode)
        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble_moji, x, 0,
            spannable, R.string.touch_to_try, false
        )
        mBubbles.add(button)

        spannable.clear()
        spannable.append(r.getText(R.string.tip_to_step5))

        setSpan(spannable, "\u2190", R.drawable.tutorial_back)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            spannable, R.string.touch_to_continue, false
        )
        mBubbles.add(button)

        button = Bubble(
            context, mInputView,
            R.drawable.dialog_bubble, x, 0,
            R.string.tip_to_step6, R.string.touch_to_finish
        )
        mBubbles.add(button)
    }

    private fun setSpan(spannable: SpannableStringBuilder, marker: String, imageResourceId: Int) {
        val text = spannable.toString()
        var target = text.indexOf(marker)
        while (0 <= target) {
            val span = ImageSpan(
                mIme, imageResourceId,
                DynamicDrawableSpan.ALIGN_BOTTOM
            )
            spannable.setSpan(span, target, target + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            target = text.indexOf(marker, target + 1)
        }
    }

    fun start() {
        mInputView.getLocationInWindow(mLocation)
        mBubbleIndex = -1
        mInputView.setOnTouchListener(this)
        next()
    }

    fun next(): Boolean {
        if (mBubbleIndex >= 0) {
            if (!mBubbles[mBubbleIndex].isShowing) {
                return true
            }
            for (i in 0..mBubbleIndex) {
                mBubbles[i].hide()
            }
        }
        mBubbleIndex++
        if (mBubbleIndex >= mBubbles.size) {
            mEnableKeyTouch = true
            mIme.sendDownUpKeyEvents(-1)
            mIme.tutorialDone()
            return false
        }

        if ((6 <= mBubbleIndex) && (mBubbleIndex <= 8)) {
            mInputManager.nextKeyMode()
        }

        if (mBubbleIndex == LONG_PRESS_INDEX) {
            mEnableKeyTouch = true
        } else if (LONG_PRESS_INDEX < mBubbleIndex) {
            mEnableKeyTouch = false
        }

        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(MSG_SHOW_BUBBLE, mBubbles[mBubbleIndex]), 500
        )
        return true
    }

    fun hide() {
        for (i in mBubbles.indices) {
            mBubbles[i].hide()
        }
        mInputView.setOnTouchListener(null)
    }

    fun close(): Boolean {
        mHandler.removeMessages(MSG_SHOW_BUBBLE)
        hide()
        return true
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val ret = !mEnableKeyTouch
        if (event.action == MotionEvent.ACTION_UP) {
            if (mBubbleIndex >= mBubbles.size) {
                mInputView.setOnTouchListener(null)
            } else {
                if (mBubbleIndex != LONG_PRESS_INDEX) {
                    next()
                }
            }
        }
        return ret
    }

    companion object {
        private const val LONG_PRESS_INDEX = 8
        private const val MSG_SHOW_BUBBLE = 0
    }
}
