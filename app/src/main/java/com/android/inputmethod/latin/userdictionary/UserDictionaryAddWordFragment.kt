/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.latin.userdictionary

import android.app.Fragment
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.userdictionary.UserDictionaryAddWordContents.LocaleRenderer
import com.android.inputmethod.latin.userdictionary.UserDictionaryLocalePicker.LocationChangedListener
import java.util.Locale

// Caveat: This class is basically taken from
// packages/apps/Settings/src/com/android/settings/inputmethod/UserDictionaryAddWordFragment.java
// in order to deal with some devices that have issues with the user dictionary handling
/**
 * Fragment to add a word/shortcut to the user dictionary.
 *
 * As opposed to the UserDictionaryActivity, this is only invoked within Settings
 * from the UserDictionarySettings.
 */
class UserDictionaryAddWordFragment : Fragment(), AdapterView.OnItemSelectedListener,
    LocationChangedListener {
    private var mContents: UserDictionaryAddWordContents? = null
    private var mRootView: View? = null
    private var mIsDeleting: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        getActivity().getActionBar()!!.setTitle(R.string.edit_personal_dictionary)
        // Keep the instance so that we remember mContents when configuration changes (eg rotation)
        setRetainInstance(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedState: Bundle
    ): View? {
        mRootView = inflater.inflate(R.layout.user_dictionary_add_word_fullscreen, null)
        mIsDeleting = false
        // If we have a non-null mContents object, it's the old value before a configuration
        // change (eg rotation) so we need to use its values. Otherwise, read from the arguments.
        if (null == mContents) {
            mContents = UserDictionaryAddWordContents(mRootView, getArguments())
        } else {
            // We create a new mContents object to account for the new situation : a word has
            // been added to the user dictionary when we started rotating, and we are now editing
            // it. That means in particular if the word undergoes any change, the old version should
            // be updated, so the mContents object needs to switch to EDIT mode if it was in
            // INSERT mode.
            mContents = UserDictionaryAddWordContents(
                mRootView,
                mContents!! /* oldInstanceToBeEdited */
            )
        }
        getActivity().getActionBar()!!.setSubtitle(
            UserDictionarySettingsUtils.getLocaleDisplayName(
                getActivity(), mContents.getCurrentUserDictionaryLocale()
            )
        )
        return mRootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val actionItemAdd: MenuItem = menu.add(
            0, OPTIONS_MENU_ADD, 0,
            R.string.user_dict_settings_add_menu_title
        ).setIcon(R.drawable.ic_menu_add)
        actionItemAdd.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
        )
        val actionItemDelete: MenuItem = menu.add(
            0, OPTIONS_MENU_DELETE, 0,
            R.string.user_dict_settings_delete
        ).setIcon(android.R.drawable.ic_menu_delete)
        actionItemDelete.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
        )
    }

    /**
     * Callback for the framework when a menu option is pressed.
     *
     * @param item the item that was pressed
     * @return false to allow normal menu processing to proceed, true to consume it here
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == OPTIONS_MENU_ADD) {
            // added the entry in "onPause"
            getActivity().onBackPressed()
            return true
        }
        if (item.getItemId() == OPTIONS_MENU_DELETE) {
            mContents!!.delete(getActivity())
            mIsDeleting = true
            getActivity().onBackPressed()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // We are being shown: display the word
        updateSpinner()
    }

    private fun updateSpinner() {
        val localesList: ArrayList<LocaleRenderer?> = mContents!!.getLocalesList(getActivity())

        val localeSpinner: Spinner =
            mRootView!!.findViewById<View>(R.id.user_dictionary_add_locale) as Spinner
        val adapter: ArrayAdapter<LocaleRenderer?> = ArrayAdapter(
            getActivity(), android.R.layout.simple_spinner_item, localesList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        localeSpinner.setAdapter(adapter)
        localeSpinner.setOnItemSelectedListener(this)
    }

    override fun onPause() {
        super.onPause()
        // We are being hidden: commit changes to the user dictionary, unless we were deleting it
        if (!mIsDeleting) {
            mContents!!.apply(getActivity(), null)
        }
    }

    override fun onItemSelected(
        parent: AdapterView<*>, view: View, pos: Int,
        id: Long
    ) {
        val locale: LocaleRenderer = parent.getItemAtPosition(pos) as LocaleRenderer
        if (locale.isMoreLanguages()) {
            val preferenceActivity: PreferenceActivity = getActivity() as PreferenceActivity
            preferenceActivity.startPreferenceFragment(UserDictionaryLocalePicker(), true)
        } else {
            mContents!!.updateLocale(locale.getLocaleString())
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // I'm not sure we can come here, but if we do, that's the right thing to do.
        val args: Bundle = getArguments()
        mContents!!.updateLocale(args.getString(UserDictionaryAddWordContents.Companion.EXTRA_LOCALE))
    }

    // Called by the locale picker
    override fun onLocaleSelected(locale: Locale) {
        mContents!!.updateLocale(locale.toString())
        getActivity().onBackPressed()
    }

    companion object {
        private val OPTIONS_MENU_ADD: Int = Menu.FIRST
        private val OPTIONS_MENU_DELETE: Int = Menu.FIRST + 1
    }
}

