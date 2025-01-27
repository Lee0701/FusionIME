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

import android.Manifest.permission
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.TwoStatePreference
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.accounts.AccountStateChangedListener
import com.android.inputmethod.latin.accounts.LoginAccountUtils
import com.android.inputmethod.latin.define.ProductionFlags
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.utils.ManagedProfileUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * "Accounts & Privacy" settings sub screen.
 *
 * This settings sub screen handles the following preferences:
 *  *  Account selection/management for IME
 *  *  Sync preferences
 *  *  Privacy preferences
 */
class AccountsSettingsFragment : SubScreenFragment() {
    /**
     * Onclick listener for sync now pref.
     */
    private val mSyncNowListener: OnPreferenceClickListener = SyncNowListener()

    /**
     * Onclick listener for delete sync pref.
     */
    private val mDeleteSyncDataListener: OnPreferenceClickListener = DeleteSyncDataListener()

    /**
     * Onclick listener for enable sync pref.
     */
    private val mEnableSyncClickListener: OnPreferenceClickListener = EnableSyncClickListener()

    /**
     * Enable sync checkbox pref.
     */
    private var mEnableSyncPreference: TwoStatePreference? = null

    /**
     * Enable sync checkbox pref.
     */
    private var mSyncNowPreference: Preference? = null

    /**
     * Clear sync data pref.
     */
    private var mClearSyncDataPreference: Preference? = null

    /**
     * Account switcher preference.
     */
    private var mAccountSwitcher: Preference? = null

    /**
     * Stores if we are currently detecting a managed profile.
     */
    private val mManagedProfileBeingDetected = AtomicBoolean(true)

    /**
     * Stores if we have successfully detected if the device has a managed profile.
     */
    private val mHasManagedProfile = AtomicBoolean(false)

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_accounts)

        mAccountSwitcher = findPreference(PREF_ACCCOUNT_SWITCHER)
        mEnableSyncPreference = findPreference(PREF_ENABLE_SYNC_NOW) as TwoStatePreference
        mSyncNowPreference = findPreference(PREF_SYNC_NOW)
        mClearSyncDataPreference = findPreference(PREF_CLEAR_SYNC_DATA)

        if (ProductionFlags.IS_METRICS_LOGGING_SUPPORTED) {
            val enableMetricsLogging =
                findPreference(Settings.Companion.PREF_ENABLE_METRICS_LOGGING)
            val res = resources
            if (enableMetricsLogging != null) {
                val enableMetricsLoggingTitle = res.getString(
                    R.string.enable_metrics_logging, applicationName
                )
                enableMetricsLogging.title = enableMetricsLoggingTitle
            }
        } else {
            removePreference(Settings.Companion.PREF_ENABLE_METRICS_LOGGING)
        }

        if (!ProductionFlags.ENABLE_USER_HISTORY_DICTIONARY_SYNC) {
            removeSyncPreferences()
        } else {
            // Disable by default till we are sure we can enable this.
            disableSyncPreferences()
            ManagedProfileCheckerTask(this).execute()
        }
    }

    /**
     * Task to check work profile. If found, it removes the sync prefs. If not,
     * it enables them.
     */
    private class ManagedProfileCheckerTask(fragment: AccountsSettingsFragment) :
        AsyncTask<Void?, Void?, Boolean>() {
        private val mFragment = fragment

        override fun onPreExecute() {
            mFragment.mManagedProfileBeingDetected.set(true)
        }

        override fun doInBackground(vararg params: Void): Boolean {
            return ManagedProfileUtils.Companion.getInstance().hasWorkProfile(mFragment.activity)
        }

        override fun onPostExecute(hasWorkProfile: Boolean) {
            mFragment.mHasManagedProfile.set(hasWorkProfile)
            mFragment.mManagedProfileBeingDetected.set(false)
            mFragment.refreshSyncSettingsUI()
        }
    }

    private fun enableSyncPreferences(
        accountsForLogin: Array<String?>,
        currentAccountName: String?
    ) {
        if (!ProductionFlags.ENABLE_USER_HISTORY_DICTIONARY_SYNC) {
            return
        }
        mAccountSwitcher!!.isEnabled = true

        mEnableSyncPreference!!.isEnabled = true
        mEnableSyncPreference!!.onPreferenceClickListener = mEnableSyncClickListener

        mSyncNowPreference!!.isEnabled = true
        mSyncNowPreference!!.onPreferenceClickListener = mSyncNowListener

        mClearSyncDataPreference!!.isEnabled = true
        mClearSyncDataPreference!!.onPreferenceClickListener = mDeleteSyncDataListener

        if (currentAccountName != null) {
            mAccountSwitcher!!.onPreferenceClickListener = object : OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference): Boolean {
                    if (accountsForLogin.size > 0) {
                        // TODO: Add addition of account.
                        createAccountPicker(
                            accountsForLogin, this.signedInAccountName,
                            AccountChangedListener(null)
                        ).show()
                    }
                    return true
                }
            }
        }
    }

    /**
     * Two reasons for disable - work profile or no accounts on device.
     */
    private fun disableSyncPreferences() {
        if (!ProductionFlags.ENABLE_USER_HISTORY_DICTIONARY_SYNC) {
            return
        }

        mAccountSwitcher!!.isEnabled = false
        mEnableSyncPreference!!.isEnabled = false
        mSyncNowPreference!!.isEnabled = false
        mClearSyncDataPreference!!.isEnabled = false
    }

    /**
     * Called only when ProductionFlag is turned off.
     */
    private fun removeSyncPreferences() {
        removePreference(PREF_ACCCOUNT_SWITCHER)
        removePreference(LocalSettingsConstants.PREF_ENABLE_CLOUD_SYNC)
        removePreference(PREF_SYNC_NOW)
        removePreference(PREF_CLEAR_SYNC_DATA)
    }

    override fun onResume() {
        super.onResume()
        refreshSyncSettingsUI()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (TextUtils.equals(key, LocalSettingsConstants.PREF_ACCOUNT_NAME)) {
            refreshSyncSettingsUI()
        } else if (TextUtils.equals(key, LocalSettingsConstants.PREF_ENABLE_CLOUD_SYNC)) {
            mEnableSyncPreference = findPreference(PREF_ENABLE_SYNC_NOW) as TwoStatePreference
            val syncEnabled = prefs.getBoolean(LocalSettingsConstants.PREF_ENABLE_CLOUD_SYNC, false)
            if (isSyncEnabled) {
                mEnableSyncPreference!!.summary = getString(R.string.cloud_sync_summary)
            } else {
                mEnableSyncPreference!!.summary = getString(R.string.cloud_sync_summary_disabled)
            }
            AccountStateChangedListener.onSyncPreferenceChanged(
                signedInAccountName,
                syncEnabled
            )
        }
    }

    /**
     * Checks different states like whether account is present or managed profile is present
     * and sets the sync settings accordingly.
     */
    private fun refreshSyncSettingsUI() {
        if (!ProductionFlags.ENABLE_USER_HISTORY_DICTIONARY_SYNC) {
            return
        }
        val hasAccountsPermission = PermissionsUtil.checkAllPermissionsGranted(
            activity, permission.READ_CONTACTS
        )

        val accountsForLogin = if (hasAccountsPermission) LoginAccountUtils.getAccountsForLogin(
            activity
        ) else arrayOfNulls(0)
        val currentAccount = if (hasAccountsPermission) signedInAccountName else null

        if (hasAccountsPermission && !mManagedProfileBeingDetected.get() &&
            !mHasManagedProfile.get() && accountsForLogin.size > 0
        ) {
            // Sync can be used by user; enable all preferences.
            enableSyncPreferences(accountsForLogin, currentAccount)
        } else {
            // Sync cannot be used by user; disable all preferences.
            disableSyncPreferences()
        }
        refreshSyncSettingsMessaging(
            hasAccountsPermission, mManagedProfileBeingDetected.get(),
            mHasManagedProfile.get(), accountsForLogin.size > 0,
            currentAccount
        )
    }

    /**
     * @param hasAccountsPermission whether the app has the permission to read accounts.
     * @param managedProfileBeingDetected whether we are in process of determining work profile.
     * @param hasManagedProfile whether the device has work profile.
     * @param hasAccountsForLogin whether the device has enough accounts for login.
     * @param currentAccount the account currently selected in the application.
     */
    private fun refreshSyncSettingsMessaging(
        hasAccountsPermission: Boolean,
        managedProfileBeingDetected: Boolean,
        hasManagedProfile: Boolean,
        hasAccountsForLogin: Boolean,
        currentAccount: String?
    ) {
        if (!ProductionFlags.ENABLE_USER_HISTORY_DICTIONARY_SYNC) {
            return
        }

        if (!hasAccountsPermission) {
            mEnableSyncPreference!!.isChecked = false
            mEnableSyncPreference!!.summary = getString(R.string.cloud_sync_summary_disabled)
            mAccountSwitcher!!.summary = ""
            return
        } else if (managedProfileBeingDetected) {
            // If we are determining eligiblity, we show empty summaries.
            // Once we have some deterministic result, we set summaries based on different results.
            mEnableSyncPreference!!.summary = ""
            mAccountSwitcher!!.summary = ""
        } else if (hasManagedProfile) {
            mEnableSyncPreference!!.summary =
                getString(R.string.cloud_sync_summary_disabled_work_profile)
        } else if (!hasAccountsForLogin) {
            mEnableSyncPreference!!.summary = getString(R.string.add_account_to_enable_sync)
        } else if (isSyncEnabled) {
            mEnableSyncPreference!!.summary = getString(R.string.cloud_sync_summary)
        } else {
            mEnableSyncPreference!!.summary = getString(R.string.cloud_sync_summary_disabled)
        }

        // Set some interdependent settings.
        // No account automatically turns off sync.
        if (!managedProfileBeingDetected && !hasManagedProfile) {
            if (currentAccount != null) {
                mAccountSwitcher!!.summary = getString(R.string.account_selected, currentAccount)
            } else {
                mEnableSyncPreference!!.isChecked = false
                mAccountSwitcher!!.summary = getString(R.string.no_accounts_selected)
            }
        }
    }

    val signedInAccountName: String?
        get() = sharedPreferences.getString(LocalSettingsConstants.PREF_ACCOUNT_NAME, null)

    val isSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            LocalSettingsConstants.PREF_ENABLE_CLOUD_SYNC,
            false
        )

    /**
     * Creates an account picker dialog showing the given accounts in a list and selecting
     * the selected account by default.  The list of accounts must not be null/empty.
     *
     * Package-private for testing.
     *
     * @param accounts list of accounts on the device.
     * @param selectedAccount currently selected account
     * @param positiveButtonClickListener listener that gets called when positive button is
     * clicked
     */
    @UsedForTesting
    fun createAccountPicker(
        accounts: Array<String?>,
        selectedAccount: String?,
        positiveButtonClickListener: DialogInterface.OnClickListener?
    ): AlertDialog {
        require(!(accounts == null || accounts.size == 0)) { "List of accounts must not be empty" }

        // See if the currently selected account is in the list.
        // If it is, the entry is selected, and a sign-out button is provided.
        // If it isn't, select the 0th account by default which will get picked up
        // if the user presses OK.
        var index = 0
        var isSignedIn = false
        for (i in accounts.indices) {
            if (TextUtils.equals(accounts[i], selectedAccount)) {
                index = i
                isSignedIn = true
                break
            }
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.account_select_title)
            .setSingleChoiceItems(accounts, index, null)
            .setPositiveButton(R.string.account_select_ok, positiveButtonClickListener)
            .setNegativeButton(R.string.account_select_cancel, null)
        if (isSignedIn) {
            builder.setNeutralButton(R.string.account_select_sign_out, positiveButtonClickListener)
        }
        return builder.create()
    }

    /**
     * Listener for a account selection changes from the picker.
     * Persists/removes the account to/from shared preferences and sets up sync if required.
     */
    internal inner class AccountChangedListener(dependentPreference: TwoStatePreference?) :
        DialogInterface.OnClickListener {
        /**
         * Represents preference that should be changed based on account chosen.
         */
        private val mDependentPreference = dependentPreference

        override fun onClick(dialog: DialogInterface, which: Int) {
            val oldAccount: String = this.signedInAccountName
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val lv = (dialog as AlertDialog).listView
                    val newAccount =
                        lv.getItemAtPosition(lv.checkedItemPosition) as String
                    sharedPreferences
                        .edit()
                        .putString(LocalSettingsConstants.PREF_ACCOUNT_NAME, newAccount)
                        .apply()
                    AccountStateChangedListener.onAccountSignedIn(oldAccount, newAccount)
                    if (mDependentPreference != null) {
                        mDependentPreference.isChecked = true
                    }
                }

                DialogInterface.BUTTON_NEUTRAL -> {
                    AccountStateChangedListener.onAccountSignedOut(oldAccount)
                    sharedPreferences
                        .edit()
                        .remove(LocalSettingsConstants.PREF_ACCOUNT_NAME)
                        .apply()
                }
            }
        }
    }

    /**
     * Listener that initiates the process of sync in the background.
     */
    internal inner class SyncNowListener : OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            AccountStateChangedListener.forceSync(this.signedInAccountName)
            return true
        }
    }

    /**
     * Listener that initiates the process of deleting user's data from the cloud.
     */
    internal inner class DeleteSyncDataListener : OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val confirmationDialog = AlertDialog.Builder(
                activity
            )
                .setTitle(R.string.clear_sync_data_title)
                .setMessage(R.string.clear_sync_data_confirmation)
                .setPositiveButton(R.string.clear_sync_data_ok,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                AccountStateChangedListener.forceDelete(
                                    this.signedInAccountName
                                )
                            }
                        }
                    })
                .setNegativeButton(R.string.cloud_sync_cancel, null /* OnClickListener */)
                .create()
            confirmationDialog.show()
            return true
        }
    }

    /**
     * Listens to events when user clicks on "Enable sync" feature.
     */
    internal inner class EnableSyncClickListener : OnShowListener, OnPreferenceClickListener {
        // TODO(cvnguyen): Write tests.
        override fun onPreferenceClick(preference: Preference): Boolean {
            val syncPreference = preference as TwoStatePreference
            if (syncPreference.isChecked) {
                // Uncheck for now.
                syncPreference.isChecked = false

                // Show opt-in.
                val optInDialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.cloud_sync_title)
                    .setMessage(R.string.cloud_sync_opt_in_text)
                    .setPositiveButton(R.string.account_select_ok,
                        object : DialogInterface.OnClickListener {
                            override fun onClick(
                                dialog: DialogInterface,
                                which: Int
                            ) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    val context: Context = activity
                                    val accountsForLogin =
                                        LoginAccountUtils.getAccountsForLogin(context)
                                    createAccountPicker(
                                        accountsForLogin,
                                        this.signedInAccountName,
                                        AccountChangedListener(syncPreference)
                                    )
                                        .show()
                                }
                            }
                        })
                    .setNegativeButton(R.string.cloud_sync_cancel, null)
                    .create()
                optInDialog.setOnShowListener(this)
                optInDialog.show()
            }
            return true
        }

        override fun onShow(dialog: DialogInterface) {
            val messageView = (dialog as AlertDialog).findViewById<View>(
                android.R.id.message
            ) as TextView
            if (messageView != null) {
                messageView.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    companion object {
        private const val PREF_ENABLE_SYNC_NOW = "pref_enable_cloud_sync"
        private const val PREF_SYNC_NOW = "pref_sync_now"
        private const val PREF_CLEAR_SYNC_DATA = "pref_clear_sync_data"

        const val PREF_ACCCOUNT_SWITCHER: String = "account_switcher"
    }
}
