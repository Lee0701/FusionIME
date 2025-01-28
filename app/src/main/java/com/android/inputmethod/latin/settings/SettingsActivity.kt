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

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.MenuItem
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.android.inputmethod.latin.permissions.PermissionsManager
import com.android.inputmethod.latin.utils.FragmentUtils
import com.android.inputmethod.latin.utils.StatsUtils

class SettingsActivity : PreferenceActivity(), OnRequestPermissionsResultCallback {
    private var mShowHomeAsUp = false

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        val actionBar = actionBar
        val intent = intent
        if (actionBar != null) {
            mShowHomeAsUp = intent.getBooleanExtra(EXTRA_SHOW_HOME_AS_UP, true)
            actionBar.setDisplayHomeAsUpEnabled(mShowHomeAsUp)
            actionBar.setHomeButtonEnabled(mShowHomeAsUp)
        }
        StatsUtils.onSettingsActivity(
            if (intent.hasExtra(EXTRA_ENTRY_KEY))
                intent.getStringExtra(EXTRA_ENTRY_KEY)
            else
                EXTRA_ENTRY_VALUE_SYSTEM_SETTINGS
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mShowHomeAsUp && item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getIntent(): Intent {
        val intent = super.getIntent()
        val fragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
        if (fragment == null) {
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT)
        }
        intent.putExtra(EXTRA_NO_HEADERS, true)
        return intent
    }

    public override fun isValidFragment(fragmentName: String): Boolean {
        return FragmentUtils.isValidFragment(fragmentName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        PermissionsManager.get(this)!!
            .onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val DEFAULT_FRAGMENT: String = SettingsFragment::class.java.name

        const val EXTRA_SHOW_HOME_AS_UP: String = "show_home_as_up"
        const val EXTRA_ENTRY_KEY: String = "entry"
        const val EXTRA_ENTRY_VALUE_LONG_PRESS_COMMA: String = "long_press_comma"
        const val EXTRA_ENTRY_VALUE_APP_ICON: String = "app_icon"
        const val EXTRA_ENTRY_VALUE_NOTICE_DIALOG: String = "important_notice"
        const val EXTRA_ENTRY_VALUE_SYSTEM_SETTINGS: String = "system_settings"
    }
}
