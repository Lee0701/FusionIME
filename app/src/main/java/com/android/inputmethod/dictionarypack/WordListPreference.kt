/**
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.android.inputmethod.dictionarypack

import android.content.Context
import android.content.SharedPreferences
import android.preference.Preference
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ListView
import android.widget.TextView
import com.android.inputmethod.latin.R
import java.util.Locale

/**
 * A preference for one word list.
 *
 * This preference refers to a single word list, as available in the dictionary
 * pack. Upon being pressed, it displays a menu to allow the user to install, disable,
 * enable or delete it as appropriate for the current state of the word list.
 */
class WordListPreference(
    context: Context?,
    dictionaryListInterfaceState: DictionaryListInterfaceState, clientId: String?,
    wordlistId: String, version: Int, locale: Locale,
    description: String, status: Int, filesize: Int
) :
    Preference(context, null) {
    // Members
    // The metadata word list id and version of this word list.
    val mWordlistId: String
    val mVersion: Int
    val mLocale: Locale
    val mDescription: String

    // The id of the client for which this preference is.
    private val mClientId: String?

    // The status
    private var mStatus: Int = 0

    // The size of the dictionary file
    private val mFilesize: Int

    private val mInterfaceState: DictionaryListInterfaceState

    fun setStatus(status: Int) {
        if (status == mStatus) return
        mStatus = status
        setSummary(getSummary(status))
    }

    fun hasStatus(status: Int): Boolean {
        return status == mStatus
    }

    public override fun onCreateView(parent: ViewGroup): View {
        val orphanedView: View? = mInterfaceState.findFirstOrphanedView()
        if (null != orphanedView) return orphanedView // Will be sent to onBindView

        val newView: View = super.onCreateView(parent)
        return mInterfaceState.addToCacheAndReturnView(newView)
    }

    fun hasPriorityOver(otherPrefStatus: Int): Boolean {
        // Both of these should be one of MetadataDbHelper.STATUS_*
        return mStatus > otherPrefStatus
    }

    private fun getSummary(status: Int): String {
        val context: Context = getContext()
        when (status) {
            MetadataDbHelper.Companion.STATUS_DELETING, MetadataDbHelper.Companion.STATUS_AVAILABLE -> return context.getString(
                R.string.dictionary_available
            )

            MetadataDbHelper.Companion.STATUS_DOWNLOADING -> return context.getString(R.string.dictionary_downloading)
            MetadataDbHelper.Companion.STATUS_INSTALLED -> return context.getString(R.string.dictionary_installed)
            MetadataDbHelper.Companion.STATUS_DISABLED -> return context.getString(R.string.dictionary_disabled)
            else -> return NO_STATUS_MESSAGE
        }
    }

    init {
        mInterfaceState = dictionaryListInterfaceState
        mClientId = clientId
        mVersion = version
        mWordlistId = wordlistId
        mFilesize = filesize
        mLocale = locale
        mDescription = description

        setLayoutResource(R.layout.dictionary_line)

        setTitle(description)
        setStatus(status)
        setKey(wordlistId)
    }

    private fun disableDict() {
        val context: Context = getContext()
        val prefs: SharedPreferences? = CommonPreferences.getCommonPreferences(context)
        CommonPreferences.disable(prefs!!, mWordlistId)
        UpdateHandler.markAsUnused(
            context,
            mClientId!!, mWordlistId, mVersion, mStatus
        )
        if (MetadataDbHelper.Companion.STATUS_DOWNLOADING == mStatus) {
            setStatus(MetadataDbHelper.Companion.STATUS_AVAILABLE)
        } else if (MetadataDbHelper.Companion.STATUS_INSTALLED == mStatus) {
            // Interface-wise, we should no longer be able to come here. However, this is still
            // the right thing to do if we do come here.
            setStatus(MetadataDbHelper.Companion.STATUS_DISABLED)
        } else {
            Log.e(TAG, "Unexpected state of the word list for disabling " + mStatus)
        }
    }

    private fun enableDict() {
        val context: Context = getContext()
        val prefs: SharedPreferences? = CommonPreferences.getCommonPreferences(context)
        CommonPreferences.enable(prefs!!, mWordlistId)
        // Explicit enabling by the user : allow downloading on metered data connection.
        UpdateHandler.markAsUsed(
            context,
            mClientId!!, mWordlistId, mVersion, mStatus, true
        )
        if (MetadataDbHelper.Companion.STATUS_AVAILABLE == mStatus) {
            setStatus(MetadataDbHelper.Companion.STATUS_DOWNLOADING)
        } else if (MetadataDbHelper.Companion.STATUS_DISABLED == mStatus
            || MetadataDbHelper.Companion.STATUS_DELETING == mStatus
        ) {
            // If the status is DELETING, it means Android Keyboard
            // has not deleted the word list yet, so we can safely
            // turn it to 'installed'. The status DISABLED is still supported internally to
            // avoid breaking older installations and all but there should not be a way to
            // disable a word list through the interface any more.
            setStatus(MetadataDbHelper.Companion.STATUS_INSTALLED)
        } else {
            Log.e(TAG, "Unexpected state of the word list for enabling " + mStatus)
        }
    }

    private fun deleteDict() {
        val context: Context = getContext()
        val prefs: SharedPreferences? = CommonPreferences.getCommonPreferences(context)
        CommonPreferences.disable(prefs!!, mWordlistId)
        setStatus(MetadataDbHelper.Companion.STATUS_DELETING)
        UpdateHandler.markAsDeleting(
            context,
            mClientId!!, mWordlistId, mVersion, mStatus
        )
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        (view as ViewGroup).setLayoutTransition(null)

        val progressBar: DictionaryDownloadProgressBar =
            view.findViewById<View>(R.id.dictionary_line_progress_bar) as DictionaryDownloadProgressBar
        val status: TextView = view.findViewById<View>(android.R.id.summary) as TextView
        progressBar.setIds(mClientId, mWordlistId)
        progressBar.setMax(mFilesize)
        val showProgressBar: Boolean = (MetadataDbHelper.Companion.STATUS_DOWNLOADING == mStatus)
        setSummary(getSummary(mStatus))
        status.setVisibility(if (showProgressBar) View.INVISIBLE else View.VISIBLE)
        progressBar.setVisibility(if (showProgressBar) View.VISIBLE else View.INVISIBLE)

        val buttonSwitcher: ButtonSwitcher = view.findViewById<View>(
            R.id.wordlist_button_switcher
        ) as ButtonSwitcher
        // We need to clear the state of the button switcher, because we reuse views; if we didn't
        // reset it would animate from whatever its old state was.
        buttonSwitcher.reset(mInterfaceState)
        if (mInterfaceState.isOpen(mWordlistId)) {
            // The button is open.
            val previousStatus: Int = mInterfaceState.getStatus(mWordlistId)
            buttonSwitcher.setStatusAndUpdateVisuals(getButtonSwitcherStatus(previousStatus))
            if (previousStatus != mStatus) {
                // We come here if the status has changed since last time. We need to animate
                // the transition.
                buttonSwitcher.setStatusAndUpdateVisuals(getButtonSwitcherStatus(mStatus))
                mInterfaceState.setOpen(mWordlistId, mStatus)
            }
        } else {
            // The button is closed.
            buttonSwitcher.setStatusAndUpdateVisuals(ButtonSwitcher.Companion.STATUS_NO_BUTTON)
        }
        buttonSwitcher.setInternalOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                onActionButtonClicked()
            }
        })
        view.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                onWordListClicked(v)
            }
        })
    }

    fun onWordListClicked(v: View) {
        // Note : v is the preference view
        val parent: ViewParent = v.getParent()
        // Just in case something changed in the framework, test for the concrete class
        if (parent !is ListView) return
        val listView: ListView = parent
        val indexToOpen: Int
        // Close all first, we'll open back any item that needs to be open.
        val wasOpen: Boolean = mInterfaceState.isOpen(mWordlistId)
        mInterfaceState.closeAll()
        if (wasOpen) {
            // This button being shown. Take note that we don't want to open any button in the
            // loop below.
            indexToOpen = -1
        } else {
            // This button was not being shown. Open it, and remember the index of this
            // child as the one to open in the following loop.
            mInterfaceState.setOpen(mWordlistId, mStatus)
            indexToOpen = listView.indexOfChild(v)
        }
        val lastDisplayedIndex: Int =
            listView.getLastVisiblePosition() - listView.getFirstVisiblePosition()
        // The "lastDisplayedIndex" is actually displayed, hence the <=
        for (i in 0..lastDisplayedIndex) {
            val buttonSwitcher: ButtonSwitcher = listView.getChildAt(i)
                .findViewById<View>(R.id.wordlist_button_switcher) as ButtonSwitcher
            if (i == indexToOpen) {
                buttonSwitcher.setStatusAndUpdateVisuals(getButtonSwitcherStatus(mStatus))
            } else {
                buttonSwitcher.setStatusAndUpdateVisuals(ButtonSwitcher.Companion.STATUS_NO_BUTTON)
            }
        }
    }

    fun onActionButtonClicked() {
        when (getActionIdFromStatusAndMenuEntry(mStatus)) {
            ACTION_ENABLE_DICT -> enableDict()
            ACTION_DISABLE_DICT -> disableDict()
            ACTION_DELETE_DICT -> deleteDict()
            else -> Log.e(TAG, "Unknown menu item pressed")
        }
    }

    companion object {
        private val TAG: String = WordListPreference::class.java.getSimpleName()

        // What to display in the "status" field when we receive unknown data as a status from
        // the content provider. Empty string sounds sensible.
        private const val NO_STATUS_MESSAGE: String = ""

        /** Actions */
        private const val ACTION_UNKNOWN: Int = 0
        private const val ACTION_ENABLE_DICT: Int = 1
        private const val ACTION_DISABLE_DICT: Int = 2
        private const val ACTION_DELETE_DICT: Int = 3

        // The table below needs to be kept in sync with MetadataDbHelper.STATUS_* since it uses
        // the values as indices.
        private val sStatusActionList: Array<IntArray> =
            arrayOf<IntArray>( // MetadataDbHelper.STATUS_UNKNOWN
                intArrayOf(),  // MetadataDbHelper.STATUS_AVAILABLE
                intArrayOf(
                    ButtonSwitcher.Companion.STATUS_INSTALL,
                    ACTION_ENABLE_DICT
                ),  // MetadataDbHelper.STATUS_DOWNLOADING
                intArrayOf(
                    ButtonSwitcher.Companion.STATUS_CANCEL,
                    ACTION_DISABLE_DICT
                ),  // MetadataDbHelper.STATUS_INSTALLED
                intArrayOf(
                    ButtonSwitcher.Companion.STATUS_DELETE,
                    ACTION_DELETE_DICT
                ),  // MetadataDbHelper.STATUS_DISABLED
                intArrayOf(
                    ButtonSwitcher.Companion.STATUS_DELETE,
                    ACTION_DELETE_DICT
                ),  // MetadataDbHelper.STATUS_DELETING
                // We show 'install' because the file is supposed to be deleted.
                // The user may reinstall it.
                intArrayOf(ButtonSwitcher.Companion.STATUS_INSTALL, ACTION_ENABLE_DICT)
            )

        fun getButtonSwitcherStatus(status: Int): Int {
            if (status >= sStatusActionList.size) {
                Log.e(TAG, "Unknown status " + status)
                return ButtonSwitcher.Companion.STATUS_NO_BUTTON
            }
            return sStatusActionList.get(status).get(0)
        }

        fun getActionIdFromStatusAndMenuEntry(status: Int): Int {
            if (status >= sStatusActionList.size) {
                Log.e(TAG, "Unknown status " + status)
                return ACTION_UNKNOWN
            }
            return sStatusActionList.get(status).get(1)
        }
    }
}
