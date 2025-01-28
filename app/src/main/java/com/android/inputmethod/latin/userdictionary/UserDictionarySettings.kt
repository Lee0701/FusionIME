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

import android.app.ListFragment
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.preference.PreferenceActivity
import android.provider.UserDictionary.Words
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AlphabetIndexer
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SectionIndexer
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import com.android.inputmethod.latin.R
import java.util.Locale

// Caveat: This class is basically taken from
// packages/apps/Settings/src/com/android/settings/inputmethod/UserDictionarySettings.java
// in order to deal with some devices that have issues with the user dictionary handling
class UserDictionarySettings : ListFragment() {
    private var mCursor: Cursor? = null

    protected var mLocale: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivity().getActionBar()!!.setTitle(R.string.edit_personal_dictionary)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle
    ): View? {
        return inflater.inflate(
            R.layout.user_dictionary_preference_list_fragment, container, false
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val intent: Intent? = getActivity().getIntent()
        val localeFromIntent: String? =
            if (null == intent) null else intent.getStringExtra("locale")

        val arguments: Bundle? = getArguments()
        val localeFromArguments: String? =
            if (null == arguments) null else arguments.getString("locale")

        val locale: String?
        if (null != localeFromArguments) {
            locale = localeFromArguments
        } else if (null != localeFromIntent) {
            locale = localeFromIntent
        } else {
            locale = null
        }

        mLocale = locale
        // WARNING: The following cursor is never closed! TODO: don't put that in a member, and
        // make sure all cursors are correctly closed. Also, this comes from a call to
        // Activity#managedQuery, which has been deprecated for a long time (and which FORBIDS
        // closing the cursor, so take care when resolving this TODO). We should either use a
        // regular query and close the cursor, or switch to a LoaderManager and a CursorLoader.
        mCursor = createCursor(locale)
        val emptyView: TextView = getView()!!.findViewById<View>(android.R.id.empty) as TextView
        emptyView.setText(R.string.user_dict_settings_empty_text)

        val listView: ListView = getListView()
        listView.setAdapter(createAdapter())
        listView.setFastScrollEnabled(true)
        listView.setEmptyView(emptyView)

        setHasOptionsMenu(true)
        // Show the language as a subtitle of the action bar
        getActivity().getActionBar()!!.setSubtitle(
            UserDictionarySettingsUtils.getLocaleDisplayName(getActivity(), mLocale)
        )
    }

    override fun onResume() {
        super.onResume()
        val adapter: ListAdapter? = getListView().getAdapter()
        if (adapter != null && adapter is MyAdapter) {
            // The list view is forced refreshed here. This allows the changes done 
            // in UserDictionaryAddWordFragment (update/delete/insert) to be seen when 
            // user goes back to this view. 
            adapter.notifyDataSetChanged()
        }
    }

    @Suppress("deprecation")
    private fun createCursor(locale: String?): Cursor {
        // Locale can be any of:
        // - The string representation of a locale, as returned by Locale#toString()
        // - The empty string. This means we want a cursor returning words valid for all locales.
        // - null. This means we want a cursor for the current locale, whatever this is.
        // Note that this contrasts with the data inside the database, where NULL means "all
        // locales" and there should never be an empty string. The confusion is called by the
        // historical use of null for "all locales".
        // TODO: it should be easy to make this more readable by making the special values
        // human-readable, like "all_locales" and "current_locales" strings, provided they
        // can be guaranteed not to match locales that may exist.
        if ("" == locale) {
            // Case-insensitive sort
            return getActivity().managedQuery(
                Words.CONTENT_URI, QUERY_PROJECTION,
                QUERY_SELECTION_ALL_LOCALES, null,
                "UPPER(" + Words.WORD + ")"
            )
        }
        val queryLocale: String = if (null != locale) locale else Locale.getDefault().toString()
        return getActivity().managedQuery(
            Words.CONTENT_URI, QUERY_PROJECTION,
            QUERY_SELECTION, arrayOf(queryLocale),
            "UPPER(" + Words.WORD + ")"
        )
    }

    private fun createAdapter(): ListAdapter {
        return MyAdapter(
            getActivity(), R.layout.user_dictionary_item, mCursor,
            ADAPTER_FROM, ADAPTER_TO
        )
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val word: String? = getWord(position)
        val shortcut: String? = getShortcut(position)
        if (word != null) {
            showAddOrEditDialog(word, shortcut)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!IS_SHORTCUT_API_SUPPORTED) {
            val systemLocale: Locale = getResources().getConfiguration().locale
            if (!TextUtils.isEmpty(mLocale) && mLocale != systemLocale.toString()) {
                // Hide the add button for ICS because it doesn't support specifying a locale
                // for an entry. This new "locale"-aware API has been added in conjunction
                // with the shortcut API.
                return
            }
        }
        val actionItem: MenuItem =
            menu.add(0, OPTIONS_MENU_ADD, 0, R.string.user_dict_settings_add_menu_title)
                .setIcon(R.drawable.ic_menu_add)
        actionItem.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == OPTIONS_MENU_ADD) {
            showAddOrEditDialog(null, null)
            return true
        }
        return false
    }

    /**
     * Add or edit a word. If editingWord is null, it's an add; otherwise, it's an edit.
     * @param editingWord the word to edit, or null if it's an add.
     * @param editingShortcut the shortcut for this entry, or null if none.
     */
    private fun showAddOrEditDialog(editingWord: String?, editingShortcut: String?) {
        val args: Bundle = Bundle()
        args.putInt(
            UserDictionaryAddWordContents.EXTRA_MODE, if (null == editingWord)
                UserDictionaryAddWordContents.MODE_INSERT
            else
                UserDictionaryAddWordContents.MODE_EDIT
        )
        args.putString(UserDictionaryAddWordContents.EXTRA_WORD, editingWord)
        args.putString(UserDictionaryAddWordContents.EXTRA_SHORTCUT, editingShortcut)
        args.putString(UserDictionaryAddWordContents.EXTRA_LOCALE, mLocale)
        val pa: PreferenceActivity =
            getActivity() as PreferenceActivity
        pa.startPreferencePanel(
            UserDictionaryAddWordFragment::class.java.getName(),
            args, R.string.user_dict_settings_add_dialog_title, null, null, 0
        )
    }

    private fun getWord(position: Int): String? {
        if (null == mCursor) return null
        mCursor!!.moveToPosition(position)
        // Handle a possible race-condition
        if (mCursor!!.isAfterLast()) return null

        return mCursor!!.getString(
            mCursor!!.getColumnIndexOrThrow(Words.WORD)
        )
    }

    private fun getShortcut(position: Int): String? {
        if (!IS_SHORTCUT_API_SUPPORTED) return null
        if (null == mCursor) return null
        mCursor!!.moveToPosition(position)
        // Handle a possible race-condition
        if (mCursor!!.isAfterLast()) return null

        return mCursor!!.getString(
            mCursor!!.getColumnIndexOrThrow(Words.SHORTCUT)
        )
    }

    private class MyAdapter(
        context: Context, layout: Int, c: Cursor?,
        from: Array<String>?, to: IntArray?
    ) :
        SimpleCursorAdapter(context, layout, c, from, to, 0 /* flags */),
        SectionIndexer {
        private var mIndexer: AlphabetIndexer? = null

        private val mViewBinder: ViewBinder = object : ViewBinder {
            override fun setViewValue(v: View, c: Cursor, columnIndex: Int): Boolean {
                if (!IS_SHORTCUT_API_SUPPORTED) {
                    // just let SimpleCursorAdapter set the view values
                    return false
                }
                if (columnIndex == INDEX_SHORTCUT) {
                    val shortcut: String = c.getString(INDEX_SHORTCUT)
                    if (TextUtils.isEmpty(shortcut)) {
                        v.setVisibility(View.GONE)
                    } else {
                        (v as TextView).setText(shortcut)
                        v.setVisibility(View.VISIBLE)
                    }
                    v.invalidate()
                    return true
                }

                return false
            }
        }

        init {
            if (null != c) {
                val alphabet: String = context.getString(R.string.user_dict_fast_scroll_alphabet)
                val wordColIndex: Int = c.getColumnIndexOrThrow(Words.WORD)
                mIndexer = AlphabetIndexer(c, wordColIndex, alphabet)
            }
            setViewBinder(mViewBinder)
        }

        override fun getPositionForSection(section: Int): Int {
            return mIndexer?.getPositionForSection(section) ?: 0
        }

        override fun getSectionForPosition(position: Int): Int {
            return mIndexer?.getSectionForPosition(position) ?: 0
        }

        override fun getSections(): Array<Any>? {
            return mIndexer?.sections
        }
    }

    companion object {
        val IS_SHORTCUT_API_SUPPORTED: Boolean = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN

        private val QUERY_PROJECTION_SHORTCUT_UNSUPPORTED: Array<String> =
            arrayOf(Words._ID, Words.WORD)
        private val QUERY_PROJECTION_SHORTCUT_SUPPORTED: Array<String> =
            arrayOf(Words._ID, Words.WORD, Words.SHORTCUT)
        private val QUERY_PROJECTION: Array<String> =
            if (IS_SHORTCUT_API_SUPPORTED) QUERY_PROJECTION_SHORTCUT_SUPPORTED else QUERY_PROJECTION_SHORTCUT_UNSUPPORTED

        // The index of the shortcut in the above array.
        private const val INDEX_SHORTCUT: Int = 2

        private val ADAPTER_FROM_SHORTCUT_UNSUPPORTED: Array<String> = arrayOf(
            Words.WORD,
        )

        private val ADAPTER_FROM_SHORTCUT_SUPPORTED: Array<String> = arrayOf(
            Words.WORD, Words.SHORTCUT
        )

        private val ADAPTER_FROM: Array<String> =
            if (IS_SHORTCUT_API_SUPPORTED) ADAPTER_FROM_SHORTCUT_SUPPORTED else ADAPTER_FROM_SHORTCUT_UNSUPPORTED

        private val ADAPTER_TO_SHORTCUT_UNSUPPORTED: IntArray = intArrayOf(
            android.R.id.text1,
        )

        private val ADAPTER_TO_SHORTCUT_SUPPORTED: IntArray = intArrayOf(
            android.R.id.text1, android.R.id.text2
        )

        private val ADAPTER_TO: IntArray =
            if (IS_SHORTCUT_API_SUPPORTED) ADAPTER_TO_SHORTCUT_SUPPORTED else ADAPTER_TO_SHORTCUT_UNSUPPORTED

        // Either the locale is empty (means the word is applicable to all locales)
        // or the word equals our current locale
        private val QUERY_SELECTION: String = Words.LOCALE + "=?"
        private val QUERY_SELECTION_ALL_LOCALES: String = Words.LOCALE + " is null"

        private val DELETE_SELECTION_WITH_SHORTCUT: String = (Words.WORD
                + "=? AND " + Words.SHORTCUT + "=?")
        private val DELETE_SELECTION_WITHOUT_SHORTCUT: String = (Words.WORD
                + "=? AND " + Words.SHORTCUT + " is null OR "
                + Words.SHORTCUT + "=''")
        private val DELETE_SELECTION_SHORTCUT_UNSUPPORTED: String = Words.WORD + "=?"

        private val OPTIONS_MENU_ADD: Int = Menu.FIRST

        fun deleteWord(
            word: String?, shortcut: String?,
            resolver: ContentResolver
        ) {
            if (!IS_SHORTCUT_API_SUPPORTED) {
                resolver.delete(
                    Words.CONTENT_URI, DELETE_SELECTION_SHORTCUT_UNSUPPORTED,
                    arrayOf(word)
                )
            } else if (TextUtils.isEmpty(shortcut)) {
                resolver.delete(
                    Words.CONTENT_URI, DELETE_SELECTION_WITHOUT_SHORTCUT,
                    arrayOf(word)
                )
            } else {
                resolver.delete(
                    Words.CONTENT_URI, DELETE_SELECTION_WITH_SHORTCUT,
                    arrayOf(word, shortcut)
                )
            }
        }
    }
}

