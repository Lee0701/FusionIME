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
package com.android.inputmethod.latin.settings

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.RadioButton
import com.android.inputmethod.latin.R

/**
 * Radio Button preference
 */
open class RadioButtonPreference @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.preferenceStyle
) :
    Preference(context, attrs, defStyleAttr) {
    interface OnRadioButtonClickedListener {
        /**
         * Called when this preference needs to be saved its state.
         *
         * @param preference This preference.
         */
        fun onRadioButtonClicked(preference: RadioButtonPreference?)
    }

    private var mIsSelected = false
    private var mRadioButton: RadioButton? = null
    private var mListener: OnRadioButtonClickedListener? = null
    private val mClickListener = View.OnClickListener { callListenerOnRadioButtonClicked() }

    init {
        widgetLayoutResource = R.layout.radio_button_preference_widget
    }

    fun setOnRadioButtonClickedListener(listener: OnRadioButtonClickedListener?) {
        mListener = listener
    }

    fun callListenerOnRadioButtonClicked() {
        if (mListener != null) {
            mListener!!.onRadioButtonClicked(this)
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        mRadioButton = view.findViewById<View>(R.id.radio_button) as RadioButton
        mRadioButton!!.isChecked = mIsSelected
        mRadioButton!!.setOnClickListener(mClickListener)
        view.setOnClickListener(mClickListener)
    }

    var isSelected: Boolean
        get() = mIsSelected
        set(selected) {
            if (selected == mIsSelected) {
                return
            }
            mIsSelected = selected
            if (mRadioButton != null) {
                mRadioButton!!.isChecked = selected
            }
            notifyChanged()
        }
}
