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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
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
import android.view.animation.AnimationUtils
import com.android.inputmethod.dictionarypack.UpdateHandler.UpdateEventListener
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.common.LocaleUtils
import java.util.Locale
import java.util.TreeMap

/**
 * Preference screen.
 */
class DictionarySettingsFragment
/**
 * Empty constructor for fragment generation.
 */
    : PreferenceFragment(), UpdateEventListener {
    private var mLoadingView: View? = null
    private var mClientId: String? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mUpdateNowMenu: MenuItem? = null
    private var mChangedSettings: Boolean = false
    private val mDictionaryListInterfaceState: DictionaryListInterfaceState =
        DictionaryListInterfaceState()

    // never null
    private var mCurrentPreferenceMap: TreeMap<String, WordListPreference> = TreeMap()

    private val mConnectivityChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshNetworkState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.loading_page, container, true)
        mLoadingView = v.findViewById(R.id.loading_container)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity: Activity = getActivity()
        mClientId = activity.getIntent().getStringExtra(DICT_SETTINGS_FRAGMENT_CLIENT_ID_ARGUMENT)
        mConnectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        addPreferencesFromResource(R.xml.dictionary_settings)
        refreshInterface()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        object : AsyncTask<Void?, Void?, String?>() {
            override fun doInBackground(vararg params: Void?): String? {
                return MetadataDbHelper.getMetadataUriAsString(getActivity(), mClientId)
            }

            override fun onPostExecute(metadataUri: String?) {
                // We only add the "Refresh" button if we have a non-empty URL to refresh from. If
                // the URL is empty, of course we can't refresh so it makes no sense to display
                // this.
                if (!TextUtils.isEmpty(metadataUri)) {
                    if (mUpdateNowMenu == null) {
                        mUpdateNowMenu = menu.add(
                            Menu.NONE, MENU_UPDATE_NOW, 0,
                            R.string.check_for_updates_now
                        ).apply {
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }
                    }
                    refreshNetworkState()
                }
            }
        }.execute()
    }

    override fun onResume() {
        super.onResume()
        mChangedSettings = false
        UpdateHandler.registerUpdateEventListener(this)
        val activity: Activity = getActivity()
        val filter: IntentFilter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        getActivity().registerReceiver(mConnectivityChangedReceiver, filter)
        refreshNetworkState()

        object : Thread("onResume") {
            override fun run() {
                if (!MetadataDbHelper.isClientKnown(activity, mClientId)) {
                    Log.i(
                        TAG, ("Unknown dictionary pack client: " + mClientId
                                + ". Requesting info.")
                    )
                    val unknownClientBroadcast: Intent =
                        Intent(DictionaryPackConstants.UNKNOWN_DICTIONARY_PROVIDER_CLIENT)
                    unknownClientBroadcast.putExtra(
                        DictionaryPackConstants.DICTIONARY_PROVIDER_CLIENT_EXTRA, mClientId
                    )
                    activity.sendBroadcast(unknownClientBroadcast)
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        val activity: Activity = getActivity()
        UpdateHandler.unregisterUpdateEventListener(this)
        activity.unregisterReceiver(mConnectivityChangedReceiver)
        if (mChangedSettings) {
            val newDictBroadcast: Intent =
                Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION)
            activity.sendBroadcast(newDictBroadcast)
            mChangedSettings = false
        }
    }

    override fun downloadedMetadata(succeeded: Boolean) {
        stopLoadingAnimation()
        if (!succeeded) return  // If the download failed nothing changed, so no need to refresh

        object : Thread("refreshInterface") {
            override fun run() {
                refreshInterface()
            }
        }.start()
    }

    override fun wordListDownloadFinished(wordListId: String, succeeded: Boolean) {
        val pref: WordListPreference? = findWordListPreference(wordListId)
        if (null == pref) return
        // TODO: Report to the user if !succeeded
        val activity: Activity? = getActivity()
        if (null == activity) return
        activity.runOnUiThread(object : Runnable {
            override fun run() {
                // We have to re-read the db in case the description has changed, and to
                // find out what state it ended up if the download wasn't successful
                // TODO: don't redo everything, only re-read and set this word list status
                refreshInterface()
            }
        })
    }

    private fun findWordListPreference(id: String): WordListPreference? {
        val prefScreen: PreferenceGroup? = getPreferenceScreen()
        if (null == prefScreen) {
            Log.e(TAG, "Could not find the preference group")
            return null
        }
        for (i in prefScreen.getPreferenceCount() - 1 downTo 0) {
            val pref: Preference = prefScreen.getPreference(i)
            if (pref is WordListPreference) {
                val wlPref: WordListPreference = pref
                if (id == wlPref.mWordlistId) {
                    return wlPref
                }
            }
        }
        Log.e(TAG, "Could not find the preference for a word list id " + id)
        return null
    }

    override fun updateCycleCompleted() {}

    fun refreshNetworkState() {
        val info: NetworkInfo? = mConnectivityManager!!.getActiveNetworkInfo()
        val isConnected: Boolean = if (null == info) false else info.isConnected()
        if (null != mUpdateNowMenu) mUpdateNowMenu!!.setEnabled(isConnected)
    }

    fun refreshInterface() {
        val activity: Activity? = getActivity()
        if (null == activity) return
        val prefScreen: PreferenceGroup = getPreferenceScreen()
        val prefList: Collection<Preference> =
            createInstalledDictSettingsCollection(mClientId)

        activity.runOnUiThread(object : Runnable {
            override fun run() {
                // TODO: display this somewhere
                // if (0 != lastUpdate) mUpdateNowPreference.setSummary(updateNowSummary);
                refreshNetworkState()

                removeAnyDictSettings(prefScreen)
                var i: Int = 0
                for (preference: Preference in prefList) {
                    preference.setOrder(i++)
                    prefScreen.addPreference(preference)
                }
            }
        })
    }

    /**
     * Creates a WordListPreference list to be added to the screen.
     *
     * This method only creates the preferences but does not add them.
     * Thus, it can be called on another thread.
     *
     * @param clientId the id of the client for which we want to display the dictionary list
     * @return A collection of preferences ready to add to the interface.
     */
    private fun createInstalledDictSettingsCollection(
        clientId: String?
    ): Collection<Preference> {
        // This will directly contact the DictionaryProvider and request the list exactly like
        // any regular client would do.
        // Considering the respective value of the respective constants used here for each path,
        // segment, the url generated by this is of the form (assuming "clientId" as a clientId)
        // content://com.android.inputmethod.latin.dictionarypack/clientId/list?procotol=2
        val contentUri: Uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(getString(R.string.authority))
            .appendPath(clientId)
            .appendPath(DICT_LIST_ID) // Need to use version 2 to get this client's list
            .appendQueryParameter(
                DictionaryProvider.QUERY_PARAMETER_PROTOCOL_VERSION,
                "2"
            )
            .build()
        val activity: Activity? = getActivity()
        val cursor: Cursor? = if ((null == activity))
            null
        else
            activity.getContentResolver().query(contentUri, null, null, null, null)

        if (null == cursor) {
            val result: ArrayList<Preference> = ArrayList()
            result.add(createErrorMessage(activity, R.string.cannot_connect_to_dict_service))
            return result
        }
        try {
            if (!cursor.moveToFirst()) {
                val result: ArrayList<Preference> = ArrayList()
                result.add(createErrorMessage(activity, R.string.no_dictionaries_available))
                return result
            }
            val systemLocaleString: String = Locale.getDefault().toString()
            val prefMap: TreeMap<String, WordListPreference> = TreeMap()
            val idIndex: Int = cursor.getColumnIndex(MetadataDbHelper.WORDLISTID_COLUMN)
            val versionIndex: Int = cursor.getColumnIndex(MetadataDbHelper.VERSION_COLUMN)
            val localeIndex: Int = cursor.getColumnIndex(MetadataDbHelper.LOCALE_COLUMN)
            val descriptionIndex: Int =
                cursor.getColumnIndex(MetadataDbHelper.DESCRIPTION_COLUMN)
            val statusIndex: Int = cursor.getColumnIndex(MetadataDbHelper.STATUS_COLUMN)
            val filesizeIndex: Int =
                cursor.getColumnIndex(MetadataDbHelper.FILESIZE_COLUMN)
            do {
                val wordlistId: String = cursor.getString(idIndex)
                val version: Int = cursor.getInt(versionIndex)
                val localeString: String = cursor.getString(localeIndex)
                val locale: Locale = Locale(localeString)
                val description: String = cursor.getString(descriptionIndex)
                val status: Int = cursor.getInt(statusIndex)
                val matchLevel: Int = LocaleUtils.getMatchLevel(systemLocaleString, localeString)
                val matchLevelString: String = LocaleUtils.getMatchLevelSortedString(matchLevel)
                val filesize: Int = cursor.getInt(filesizeIndex)
                // The key is sorted in lexicographic order, according to the match level, then
                // the description.
                val key: String = matchLevelString + "." + description + "." + wordlistId
                val existingPref: WordListPreference? = prefMap.get(key)
                if (null == existingPref || existingPref.hasPriorityOver(status)) {
                    val oldPreference: WordListPreference? = mCurrentPreferenceMap.get(key)
                    val pref: WordListPreference
                    if (null != oldPreference && oldPreference.mVersion == version && oldPreference.hasStatus(
                            status
                        )
                        && oldPreference.mLocale == locale
                    ) {
                        // If the old preference has all the new attributes, reuse it. Ideally,
                        // we should reuse the old pref even if its status is different and call
                        // setStatus here, but setStatus calls Preference#setSummary() which
                        // needs to be done on the UI thread and we're not on the UI thread
                        // here. We could do all this work on the UI thread, but in this case
                        // it's probably lighter to stay on a background thread and throw this
                        // old preference out.
                        pref = oldPreference
                    } else {
                        // Otherwise, discard it and create a new one instead.
                        // TODO: when the status is different from the old one, we need to
                        // animate the old one out before animating the new one in.
                        pref = WordListPreference(
                            activity, mDictionaryListInterfaceState,
                            mClientId, wordlistId, version, locale, description, status,
                            filesize
                        )
                    }
                    prefMap.put(key, pref)
                }
            } while (cursor.moveToNext())
            mCurrentPreferenceMap = prefMap
            return prefMap.values
        } finally {
            cursor.close()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            MENU_UPDATE_NOW -> {
                if (View.GONE == mLoadingView!!.getVisibility()) {
                    startRefresh()
                } else {
                    cancelRefresh()
                }
                return true
            }
        }
        return false
    }

    private fun startRefresh() {
        startLoadingAnimation()
        mChangedSettings = true
        UpdateHandler.registerUpdateEventListener(this)
        val activity: Activity = getActivity()
        object : Thread("updateByHand") {
            override fun run() {
                // We call tryUpdate(), which returns whether we could successfully start an update.
                // If we couldn't, we'll never receive the end callback, so we stop the loading
                // animation and return to the previous screen.
                if (!UpdateHandler.tryUpdate(activity)) {
                    stopLoadingAnimation()
                }
            }
        }.start()
    }

    private fun cancelRefresh() {
        UpdateHandler.unregisterUpdateEventListener(this)
        val context: Context = getActivity()
        object : Thread("cancelByHand") {
            override fun run() {
                UpdateHandler.cancelUpdate(context, mClientId)
                stopLoadingAnimation()
            }
        }.start()
    }

    private fun startLoadingAnimation() {
        mLoadingView!!.setVisibility(View.VISIBLE)
        getView()!!.setVisibility(View.GONE)
        // We come here when the menu element is pressed so presumably it can't be null. But
        // better safe than sorry.
        if (null != mUpdateNowMenu) mUpdateNowMenu!!.setTitle(R.string.cancel)
    }

    fun stopLoadingAnimation() {
        val preferenceView: View? = getView()
        val activity: Activity? = getActivity()
        if (null == activity) return
        val loadingView: View? = mLoadingView
        val updateNowMenu: MenuItem? = mUpdateNowMenu
        activity.runOnUiThread(object : Runnable {
            override fun run() {
                loadingView!!.setVisibility(View.GONE)
                preferenceView!!.setVisibility(View.VISIBLE)
                loadingView.startAnimation(
                    AnimationUtils.loadAnimation(
                        activity, android.R.anim.fade_out
                    )
                )
                preferenceView.startAnimation(
                    AnimationUtils.loadAnimation(
                        activity, android.R.anim.fade_in
                    )
                )
                // The menu is created by the framework asynchronously after the activity,
                // which means it's possible to have the activity running but the menu not
                // created yet - hence the necessity for a null check here.
                if (null != updateNowMenu) {
                    updateNowMenu.setTitle(R.string.check_for_updates_now)
                }
            }
        })
    }

    companion object {
        private val TAG: String = DictionarySettingsFragment::class.java.getSimpleName()

        private const val DICT_LIST_ID: String = "list"
        const val DICT_SETTINGS_FRAGMENT_CLIENT_ID_ARGUMENT: String = "clientId"

        private val MENU_UPDATE_NOW: Int = Menu.FIRST

        private fun createErrorMessage(activity: Activity?, messageResource: Int): Preference {
            val message: Preference = Preference(activity)
            message.setTitle(messageResource)
            message.setEnabled(false)
            return message
        }

        fun removeAnyDictSettings(prefGroup: PreferenceGroup) {
            for (i in prefGroup.getPreferenceCount() - 1 downTo 0) {
                prefGroup.removePreference(prefGroup.getPreference(i))
            }
        }
    }
}
