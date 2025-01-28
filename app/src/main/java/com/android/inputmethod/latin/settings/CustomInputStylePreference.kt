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

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Parcel
import android.os.Parcelable
import android.preference.DialogPreference
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.android.inputmethod.compat.InputMethodSubtypeCompatUtils
import com.android.inputmethod.compat.ViewCompatUtils
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.settings.CustomInputStylePreference.SubtypeLocaleAdapter
import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.TreeSet

class CustomInputStylePreference(
    context: Context?, subtype: InputMethodSubtype?,
    proxy: Listener
) : DialogPreference(context, null), DialogInterface.OnCancelListener {
    interface Listener {
        fun onRemoveCustomInputStyle(stylePref: CustomInputStylePreference?)
        fun onSaveCustomInputStyle(stylePref: CustomInputStylePreference)
        fun onAddCustomInputStyle(stylePref: CustomInputStylePreference)
        val subtypeLocaleAdapter: SubtypeLocaleAdapter?
        val keyboardLayoutSetAdapter: KeyboardLayoutSetAdapter?
    }

    private var mSubtype: InputMethodSubtype? = null
    private var mPreviousSubtype: InputMethodSubtype? = null

    private val mProxy: Listener
    private var mSubtypeLocaleSpinner: Spinner? = null
    private var mKeyboardLayoutSetSpinner: Spinner? = null

    var subtype: InputMethodSubtype?
        get() = mSubtype
        set(subtype) {
            mPreviousSubtype = mSubtype
            mSubtype = subtype
            if (isIncomplete) {
                title = null
                setDialogTitle(R.string.add_style)
                key = KEY_NEW_SUBTYPE
            } else {
                val displayName =
                    SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype!!)
                title = displayName
                dialogTitle = displayName
                key = (KEY_PREFIX + subtype.locale + "_"
                        + SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype))
            }
        }

    init {
        dialogLayoutResource = R.layout.additional_subtype_dialog
        isPersistent = false
        mProxy = proxy
        this.subtype = subtype
    }

    fun show() {
        showDialog(null)
    }

    val isIncomplete: Boolean
        get() = mSubtype == null

    fun revert() {
        subtype = mPreviousSubtype
    }

    fun hasBeenModified(): Boolean {
        return mSubtype != null && mSubtype != mPreviousSubtype
    }

    override fun onCreateDialogView(): View {
        val v = super.onCreateDialogView()
        mSubtypeLocaleSpinner = v.findViewById<View>(R.id.subtype_locale_spinner) as Spinner
        mSubtypeLocaleSpinner!!.adapter = mProxy.subtypeLocaleAdapter
        mKeyboardLayoutSetSpinner =
            v.findViewById<View>(R.id.keyboard_layout_set_spinner) as Spinner
        mKeyboardLayoutSetSpinner!!.adapter = mProxy.keyboardLayoutSetAdapter
        // All keyboard layout names are in the Latin script and thus left to right. That means
        // the view would align them to the left even if the system locale is RTL, but that
        // would look strange. To fix this, we align them to the view's start, which will be
        // natural for any direction.
        ViewCompatUtils.setTextAlignment(
            mKeyboardLayoutSetSpinner, ViewCompatUtils.TEXT_ALIGNMENT_VIEW_START
        )
        return v
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setCancelable(true).setOnCancelListener(this)
        if (isIncomplete) {
            builder.setPositiveButton(R.string.add, this)
                .setNegativeButton(android.R.string.cancel, this)
        } else {
            builder.setPositiveButton(R.string.save, this)
                .setNeutralButton(android.R.string.cancel, this)
                .setNegativeButton(R.string.remove, this)
            val localeItem = SubtypeLocaleItem(mSubtype!!)
            val layoutItem = KeyboardLayoutSetItem(mSubtype!!)
            setSpinnerPosition(mSubtypeLocaleSpinner!!, localeItem)
            setSpinnerPosition(mKeyboardLayoutSetSpinner!!, layoutItem)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        if (isIncomplete) {
            mProxy.onRemoveCustomInputStyle(this)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        super.onClick(dialog, which)
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val isEditing = !isIncomplete
                val locale =
                    mSubtypeLocaleSpinner!!.selectedItem as SubtypeLocaleItem
                val layout =
                    mKeyboardLayoutSetSpinner!!.selectedItem as KeyboardLayoutSetItem
                val subtype =
                    AdditionalSubtypeUtils.createAsciiEmojiCapableAdditionalSubtype(
                        locale.mLocaleString, layout.mLayoutName
                    )
                this.subtype = subtype
                notifyChanged()
                if (isEditing) {
                    mProxy.onSaveCustomInputStyle(this)
                } else {
                    mProxy.onAddCustomInputStyle(this)
                }
            }

            DialogInterface.BUTTON_NEUTRAL -> {}
            DialogInterface.BUTTON_NEGATIVE -> mProxy.onRemoveCustomInputStyle(this)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val dialog = dialog
        if (dialog == null || !dialog.isShowing) {
            return superState
        }

        val myState = SavedState(superState)
        myState.mSubtype = mSubtype
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state
        super.onRestoreInstanceState(myState.superState)
        subtype = myState.mSubtype
    }

    internal class SavedState : BaseSavedState {
        var mSubtype: InputMethodSubtype? = null

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeParcelable(mSubtype, 0)
        }

        constructor(source: Parcel) : super(source) {
            mSubtype = source.readParcelable<Parcelable>(null) as InputMethodSubtype?
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    class SubtypeLocaleItem(subtype: InputMethodSubtype) :
        Comparable<SubtypeLocaleItem> {
        val mLocaleString: String = subtype.locale
        private val mDisplayName: String =
            SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(
                mLocaleString
            )

        // {@link ArrayAdapter<T>} that hosts the instance of this class needs {@link #toString()}
        // to get display name.
        override fun toString(): String {
            return mDisplayName
        }

        override fun compareTo(o: SubtypeLocaleItem): Int {
            return mLocaleString.compareTo(o.mLocaleString)
        }
    }

    class SubtypeLocaleAdapter(context: Context) :
        ArrayAdapter<SubtypeLocaleItem?>(context, android.R.layout.simple_spinner_item) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val items = TreeSet<SubtypeLocaleItem>()
            val imi: InputMethodInfo = RichInputMethodManager.instance
                .inputMethodInfoOfThisIme
            val count = imi.subtypeCount
            for (i in 0 until count) {
                val subtype = imi.getSubtypeAt(i)
                if (DEBUG_SUBTYPE_ID) {
                    Log.d(
                        TAG_SUBTYPE, String.format(
                            "%-6s 0x%08x %11d %s",
                            subtype.locale, subtype.hashCode(), subtype.hashCode(),
                            SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)
                        )
                    )
                }
                if (InputMethodSubtypeCompatUtils.isAsciiCapable(subtype)) {
                    items.add(SubtypeLocaleItem(subtype))
                }
            }
            // TODO: Should filter out already existing combinations of locale and layout.
            addAll(items)
        }

        companion object {
            private val TAG_SUBTYPE: String = SubtypeLocaleAdapter::class.java.simpleName
        }
    }

    class KeyboardLayoutSetItem(subtype: InputMethodSubtype) {
        val mLayoutName: String = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype)
        private val mDisplayName = SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(subtype)!!

        // {@link ArrayAdapter<T>} that hosts the instance of this class needs {@link #toString()}
        // to get display name.
        override fun toString(): String {
            return mDisplayName
        }
    }

    class KeyboardLayoutSetAdapter(context: Context) :
        ArrayAdapter<KeyboardLayoutSetItem?>(context, android.R.layout.simple_spinner_item) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val predefinedKeyboardLayoutSet = context.resources.getStringArray(
                R.array.predefined_layouts
            )
            // TODO: Should filter out already existing combinations of locale and layout.
            for (layout in predefinedKeyboardLayoutSet) {
                // This is a placeholder for a subtype with NO_LANGUAGE, only for display.
                val subtype =
                    AdditionalSubtypeUtils.createDummyAdditionalSubtype(
                        SubtypeLocaleUtils.NO_LANGUAGE, layout
                    )
                add(KeyboardLayoutSetItem(subtype))
            }
        }
    }

    companion object {
        private const val DEBUG_SUBTYPE_ID = false

        private const val KEY_PREFIX = "subtype_pref_"
        private const val KEY_NEW_SUBTYPE = KEY_PREFIX + "new"

        fun newIncompleteSubtypePreference(
            context: Context?, proxy: Listener
        ): CustomInputStylePreference {
            return CustomInputStylePreference(context, null, proxy)
        }

        private fun setSpinnerPosition(spinner: Spinner, itemToSelect: Any) {
            val adapter = spinner.adapter
            val count = adapter.count
            for (i in 0 until count) {
                val item = spinner.getItemAtPosition(i)
                if (item == itemToSelect) {
                    spinner.setSelection(i)
                    return
                }
            }
        }
    }
}
