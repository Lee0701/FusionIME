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
package com.android.inputmethod.latin.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.settings.CustomInputStylePreference.KeyboardLayoutSetAdapter
import com.android.inputmethod.latin.settings.CustomInputStylePreference.SubtypeLocaleAdapter
import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils
import com.android.inputmethod.latin.utils.DialogUtils
import com.android.inputmethod.latin.utils.IntentUtils
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils

class CustomInputStyleSettingsFragment : PreferenceFragment(), CustomInputStylePreference.Listener {
    private var mRichImm: RichInputMethodManager? = null
    private var mPrefs: SharedPreferences? = null
    override var subtypeLocaleAdapter: SubtypeLocaleAdapter? = null
        private set
    override var keyboardLayoutSetAdapter: KeyboardLayoutSetAdapter? = null
        private set

    private var mIsAddingNewSubtype = false
    private var mSubtypeEnablerNotificationDialog: AlertDialog? = null
    private var mSubtypePreferenceKeyForSubtypeEnabler: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mPrefs = preferenceManager.sharedPreferences
        RichInputMethodManager.init(activity)
        mRichImm = RichInputMethodManager.instance
        addPreferencesFromResource(R.xml.additional_subtype_settings)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        // For correct display in RTL locales, we need to set the layout direction of the
        // fragment's top view.
        ViewCompat.setLayoutDirection(view!!, ViewCompat.LAYOUT_DIRECTION_LOCALE)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val context: Context = activity
        subtypeLocaleAdapter = SubtypeLocaleAdapter(context)
        keyboardLayoutSetAdapter =
            KeyboardLayoutSetAdapter(context)

        val prefSubtypes: String =
            Settings.readPrefAdditionalSubtypes(
                mPrefs!!,
                resources
            )
        if (DEBUG_CUSTOM_INPUT_STYLES) {
            Log.i(
                TAG,
                "Load custom input styles: $prefSubtypes"
            )
        }
        setPrefSubtypes(prefSubtypes, context)

        mIsAddingNewSubtype = (savedInstanceState != null)
                && savedInstanceState.containsKey(KEY_IS_ADDING_NEW_SUBTYPE)
        if (mIsAddingNewSubtype) {
            preferenceScreen.addPreference(
                CustomInputStylePreference.newIncompleteSubtypePreference(context, this)
            )
        }

        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(
                KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN
            )
        ) {
            mSubtypePreferenceKeyForSubtypeEnabler = savedInstanceState.getString(
                KEY_SUBTYPE_FOR_SUBTYPE_ENABLER
            )
            mSubtypeEnablerNotificationDialog = createDialog()
            mSubtypeEnablerNotificationDialog!!.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mIsAddingNewSubtype) {
            outState.putBoolean(KEY_IS_ADDING_NEW_SUBTYPE, true)
        }
        if (mSubtypeEnablerNotificationDialog != null
            && mSubtypeEnablerNotificationDialog!!.isShowing
        ) {
            outState.putBoolean(KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN, true)
            outState.putString(
                KEY_SUBTYPE_FOR_SUBTYPE_ENABLER, mSubtypePreferenceKeyForSubtypeEnabler
            )
        }
    }

    override fun onRemoveCustomInputStyle(stylePref: CustomInputStylePreference?) {
        mIsAddingNewSubtype = false
        val group: PreferenceGroup = preferenceScreen
        group.removePreference(stylePref)
        mRichImm!!.setAdditionalInputMethodSubtypes(subtypes)
    }

    override fun onSaveCustomInputStyle(stylePref: CustomInputStylePreference) {
        val subtype = stylePref.subtype
        if (!stylePref.hasBeenModified()) {
            return
        }
        if (findDuplicatedSubtype(subtype!!) == null) {
            mRichImm!!.setAdditionalInputMethodSubtypes(subtypes)
            return
        }

        // Saved subtype is duplicated.
        val group: PreferenceGroup = preferenceScreen
        group.removePreference(stylePref)
        stylePref.revert()
        group.addPreference(stylePref)
        showSubtypeAlreadyExistsToast(subtype)
    }

    override fun onAddCustomInputStyle(stylePref: CustomInputStylePreference) {
        mIsAddingNewSubtype = false
        val subtype = stylePref.subtype
        if (findDuplicatedSubtype(subtype!!) == null) {
            mRichImm!!.setAdditionalInputMethodSubtypes(subtypes)
            mSubtypePreferenceKeyForSubtypeEnabler = stylePref.key
            mSubtypeEnablerNotificationDialog = createDialog()
            mSubtypeEnablerNotificationDialog!!.show()
            return
        }

        // Newly added subtype is duplicated.
        val group: PreferenceGroup = preferenceScreen
        group.removePreference(stylePref)
        showSubtypeAlreadyExistsToast(subtype)
    }

    private fun showSubtypeAlreadyExistsToast(subtype: InputMethodSubtype) {
        val context: Context = activity
        val res = context.resources
        val message = res.getString(
            R.string.custom_input_style_already_exists,
            SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)
        )
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun findDuplicatedSubtype(subtype: InputMethodSubtype): InputMethodSubtype? {
        val localeString = subtype.locale
        val keyboardLayoutSetName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype)
        return mRichImm!!.findSubtypeByLocaleAndKeyboardLayoutSet(
            localeString, keyboardLayoutSetName
        )
    }

    private fun createDialog(): AlertDialog {
        val imeId = mRichImm?.inputMethodIdOfThisIme
        val builder = AlertDialog.Builder(
            DialogUtils.getPlatformDialogThemeContext(activity)
        )
        builder.setTitle(R.string.custom_input_styles_title)
            .setMessage(R.string.custom_input_style_note_message)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(
                R.string.enable
            ) { dialog, which ->
                val intent = IntentUtils.getInputLanguageSelectionIntent(
                    imeId,
                    (Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                // TODO: Add newly adding subtype to extra value of the intent as a hint
                // for the input language selection activity.
                // intent.putExtra("newlyAddedSubtype", subtypePref.getSubtype());
                startActivity(intent)
            }

        return builder.create()
    }

    private fun setPrefSubtypes(prefSubtypes: String, context: Context) {
        val group: PreferenceGroup = preferenceScreen
        group.removeAll()
        val subtypesArray =
            AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefSubtypes)
        for (subtype in subtypesArray) {
            val pref =
                CustomInputStylePreference(context, subtype, this)
            group.addPreference(pref)
        }
    }

    private val subtypes: Array<InputMethodSubtype>
        get() {
            val group: PreferenceGroup = preferenceScreen
            val subtypes = ArrayList<InputMethodSubtype>()
            val count = group.preferenceCount
            for (i in 0 until count) {
                val pref = group.getPreference(i)
                if (pref is CustomInputStylePreference) {
                    val subtypePref = pref
                    // We should not save newly adding subtype to preference because it is incomplete.
                    if (subtypePref.isIncomplete) continue
                    subtypes.add(subtypePref.subtype ?: continue)
                }
            }
            return subtypes.toTypedArray<InputMethodSubtype>()
        }

    override fun onPause() {
        super.onPause()
        val oldSubtypes: String = Settings.readPrefAdditionalSubtypes(
            mPrefs!!, resources
        )
        val subtypes = subtypes
        val prefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(subtypes)
        if (DEBUG_CUSTOM_INPUT_STYLES) {
            Log.i(
                TAG,
                "Save custom input styles: $prefSubtypes"
            )
        }
        if (prefSubtypes == oldSubtypes) {
            return
        }
        Settings.writePrefAdditionalSubtypes(mPrefs!!, prefSubtypes)
        mRichImm!!.setAdditionalInputMethodSubtypes(subtypes)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_style, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_add_style) {
            val newSubtype: CustomInputStylePreference =
                CustomInputStylePreference.newIncompleteSubtypePreference(activity, this)
            preferenceScreen.addPreference(newSubtype)
            newSubtype.show()
            mIsAddingNewSubtype = true
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG: String = CustomInputStyleSettingsFragment::class.java.simpleName

        // Note: We would like to turn this debug flag true in order to see what input styles are
        // defined in a bug-report.
        private const val DEBUG_CUSTOM_INPUT_STYLES = true

        private const val KEY_IS_ADDING_NEW_SUBTYPE = "is_adding_new_subtype"
        private const val KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN =
            "is_subtype_enabler_notification_dialog_open"
        private const val KEY_SUBTYPE_FOR_SUBTYPE_ENABLER = "subtype_for_subtype_enabler"

        fun updateCustomInputStylesSummary(pref: Preference) {
            // When we are called from the Settings application but we are not already running, some
            // singleton and utility classes may not have been initialized.  We have to call
            // initialization method of these classes here. See {@link LatinIME#onCreate()}.
            SubtypeLocaleUtils.init(pref.context)

            val res = pref.context.resources
            val prefs = pref.sharedPreferences
            val prefSubtype: String = Settings.readPrefAdditionalSubtypes(prefs, res)
            val subtypes =
                AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefSubtype)
            val subtypeNames = ArrayList<String>()
            for (subtype in subtypes) {
                subtypeNames.add(SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype))
            }
            // TODO: A delimiter of custom input styles should be localized.
            pref.summary = TextUtils.join(", ", subtypeNames)
        }
    }
}
