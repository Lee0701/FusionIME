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

import android.app.Activity
import android.app.Fragment
import android.os.Bundle

/**
 * Test activity to use when testing preference fragments. <br></br>
 * Usage: <br></br>
 * Create an ActivityInstrumentationTestCase2 for this activity
 * and call setIntent() with an intent that specifies the fragment to load in the activity.
 * The fragment can then be obtained from this activity and used for testing/verification.
 */
class TestFragmentActivity : Activity() {
    var mFragment: Fragment? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        val intent = intent
        val fragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
        requireNotNull(fragmentName) { "No fragment name specified for testing" }

        mFragment = Fragment.instantiate(this, fragmentName)
        val fragmentManager = fragmentManager
        fragmentManager.beginTransaction().add(mFragment, fragmentName).commit()
    }

    companion object {
        /**
         * The fragment name that should be loaded when starting this activity.
         * This must be specified when starting this activity, as this activity is only
         * meant to test fragments from instrumentation tests.
         */
        const val EXTRA_SHOW_FRAGMENT: String = "show_fragment"
    }
}
