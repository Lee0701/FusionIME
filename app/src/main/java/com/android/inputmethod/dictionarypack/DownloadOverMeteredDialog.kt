/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.inputmethod.dictionarypack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.android.inputmethod.annotations.ExternallyReferenced
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.common.LocaleUtils

/**
 * This implements the dialog for asking the user whether it's okay to download dictionaries over
 * a metered connection or not (e.g. their mobile data plan).
 */
class DownloadOverMeteredDialog : Activity() {
    private var mClientId: String? = null
    private var mWordListToDownload: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent: Intent = getIntent()
        mClientId = intent.getStringExtra(CLIENT_ID_KEY)
        mWordListToDownload = intent.getStringExtra(WORDLIST_TO_DOWNLOAD_KEY)
        val localeString: String? = intent.getStringExtra(LOCALE_KEY)
        val size: Long = intent.getIntExtra(SIZE_KEY, 0).toLong()
        setContentView(R.layout.download_over_metered)
        setTexts(localeString, size)
    }

    private fun setTexts(localeString: String?, size: Long) {
        val promptFormat: String = getString(R.string.should_download_over_metered_prompt)
        val allowButtonFormat: String = getString(R.string.download_over_metered)
        val language: String = if ((null == localeString))
            ""
        else
            LocaleUtils.constructLocaleFromString(localeString)!!.getDisplayLanguage()
        val prompt: TextView = findViewById<View>(R.id.download_over_metered_prompt) as TextView
        prompt.setText(Html.fromHtml(String.format(promptFormat, language)))
        val allowButton: Button = findViewById<View>(R.id.allow_button) as Button
        allowButton.setText(String.format(allowButtonFormat, (size.toFloat()) / (1024 * 1024)))
    }

    // This method is externally referenced from layout/download_over_metered.xml using onClick
    // attribute of Button.
    @ExternallyReferenced
    @Suppress("unused")
    fun onClickDeny(v: View?) {
        UpdateHandler.setDownloadOverMeteredSetting(this, false)
        finish()
    }

    // This method is externally referenced from layout/download_over_metered.xml using onClick
    // attribute of Button.
    @ExternallyReferenced
    @Suppress("unused")
    fun onClickAllow(v: View?) {
        UpdateHandler.setDownloadOverMeteredSetting(this, true)
        UpdateHandler.installIfNeverRequested(
            this, mClientId,
            mWordListToDownload!!
        )
        finish()
    }

    companion object {
        const val CLIENT_ID_KEY: String = "client_id"
        const val WORDLIST_TO_DOWNLOAD_KEY: String = "wordlist_to_download"
        const val SIZE_KEY: String = "size"
        const val LOCALE_KEY: String = "locale"
    }
}
