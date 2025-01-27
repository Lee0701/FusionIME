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
package com.android.inputmethod.latin

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Process
import android.preference.PreferenceManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.dictionarypack.DictionaryPackConstants
import com.android.inputmethod.dictionarypack.DownloadManagerWrapper
import com.android.inputmethod.keyboard.KeyboardLayoutSet
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.setup.SetupActivity
import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils

/**
 * This class detects the [Intent.ACTION_MY_PACKAGE_REPLACED] broadcast intent when this IME
 * package has been replaced by a newer version of the same package. This class also detects
 * [Intent.ACTION_BOOT_COMPLETED] and [Intent.ACTION_USER_INITIALIZE] broadcast intent.
 *
 * If this IME has already been installed in the system image and a new version of this IME has
 * been installed, [Intent.ACTION_MY_PACKAGE_REPLACED] is received by this receiver and it
 * will hide the setup wizard's icon.
 *
 * If this IME has already been installed in the data partition and a new version of this IME has
 * been installed, [Intent.ACTION_MY_PACKAGE_REPLACED] is received by this receiver but it
 * will not hide the setup wizard's icon, and the icon will appear on the launcher.
 *
 * If this IME hasn't been installed yet and has been newly installed, no
 * [Intent.ACTION_MY_PACKAGE_REPLACED] will be sent and the setup wizard's icon will appear
 * on the launcher.
 *
 * When the device has been booted, [Intent.ACTION_BOOT_COMPLETED] is received by this
 * receiver and it checks whether the setup wizard's icon should be appeared or not on the launcher
 * depending on which partition this IME is installed.
 *
 * When the system locale has been changed, [Intent.ACTION_LOCALE_CHANGED] is received by
 * this receiver and the [KeyboardLayoutSet]'s cache is cleared.
 */
class SystemBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction: String? = intent.getAction()
        if (Intent.ACTION_MY_PACKAGE_REPLACED == intentAction) {
            Log.i(TAG, "Package has been replaced: " + context.getPackageName())
            // Need to restore additional subtypes because system always clears additional
            // subtypes when the package is replaced.
            RichInputMethodManager.Companion.init(context)
            val richImm: RichInputMethodManager = RichInputMethodManager.Companion.getInstance()
            val additionalSubtypes: Array<InputMethodSubtype?>? = richImm.getAdditionalSubtypes()
            richImm.setAdditionalInputMethodSubtypes(additionalSubtypes)
            toggleAppIcon(context)

            // Remove all the previously scheduled downloads. This will also makes sure
            // that any erroneously stuck downloads will get cleared. (b/21797386)
            removeOldDownloads(context)
            // b/21797386
            // downloadLatestDictionaries(context);
        } else if (Intent.ACTION_BOOT_COMPLETED == intentAction) {
            Log.i(TAG, "Boot has been completed")
            toggleAppIcon(context)
        } else if (Intent.ACTION_LOCALE_CHANGED == intentAction) {
            Log.i(TAG, "System locale changed")
            KeyboardLayoutSet.Companion.onSystemLocaleChanged()
        }

        // The process that hosts this broadcast receiver is invoked and remains alive even after
        // 1) the package has been re-installed,
        // 2) the device has just booted,
        // 3) a new user has been created.
        // There is no good reason to keep the process alive if this IME isn't a current IME.
        val imm: InputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Called to check whether this IME has been triggered by the current user or not
        val isInputMethodManagerValidForUserOfThisProcess: Boolean =
            !imm.getInputMethodList().isEmpty()
        val isCurrentImeOfCurrentUser: Boolean = isInputMethodManagerValidForUserOfThisProcess
                && UncachedInputMethodManagerUtils.isThisImeCurrent(context, imm)
        if (!isCurrentImeOfCurrentUser) {
            val myPid: Int = Process.myPid()
            Log.i(TAG, "Killing my process: pid=" + myPid)
            Process.killProcess(myPid)
        }
    }

    private fun removeOldDownloads(context: Context) {
        try {
            Log.i(TAG, "Removing the old downloads in progress of the previous keyboard version.")
            val downloadManagerWrapper: DownloadManagerWrapper = DownloadManagerWrapper(
                context
            )
            val q: DownloadManager.Query = DownloadManager.Query()
            // Query all the download statuses except the succeeded ones.
            q.setFilterByStatus(
                (DownloadManager.STATUS_FAILED
                        or DownloadManager.STATUS_PAUSED
                        or DownloadManager.STATUS_PENDING
                        or DownloadManager.STATUS_RUNNING)
            )
            val c: Cursor? = downloadManagerWrapper.query(q)
            if (c != null) {
                c.moveToFirst()
                while (!c.isAfterLast()) {
                    val downloadId: Long = c
                        .getLong(c.getColumnIndex(DownloadManager.COLUMN_ID))
                    downloadManagerWrapper.remove(downloadId)
                    Log.i(TAG, "Removed the download with Id: " + downloadId)
                    c.moveToNext()
                }
                c.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while removing old downloads.")
        }
    }

    private fun downloadLatestDictionaries(context: Context) {
        val updateIntent: Intent = Intent(
            DictionaryPackConstants.INIT_AND_UPDATE_NOW_INTENT_ACTION
        )
        context.sendBroadcast(updateIntent)
    }

    companion object {
        private val TAG: String = SystemBroadcastReceiver::class.java.getSimpleName()

        fun toggleAppIcon(context: Context) {
            val appInfoFlags: Int = context.getApplicationInfo().flags
            val isSystemApp: Boolean = (appInfoFlags and ApplicationInfo.FLAG_SYSTEM) > 0
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "toggleAppIcon() : FLAG_SYSTEM = " + isSystemApp)
            }
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            context.getPackageManager().setComponentEnabledSetting(
                ComponentName(context, SetupActivity::class.java),
                if (Settings.Companion.readShowSetupWizardIcon(prefs, context))
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
