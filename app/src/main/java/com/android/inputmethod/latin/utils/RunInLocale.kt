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
package com.android.inputmethod.latin.utils

import android.content.res.Resources
import java.util.Locale

abstract class RunInLocale<T> {
    protected abstract fun job(res: Resources): T

    /**
     * Execute [.job] method in specified system locale exclusively.
     *
     * @param res the resources to use.
     * @param newLocale the locale to change to. Run in system locale if null.
     * @return the value returned from [.job].
     */
    fun runInLocale(res: Resources, newLocale: Locale?): T {
        synchronized(sLockForRunInLocale) {
            val conf = res.configuration
            if (newLocale == null || newLocale == conf.locale) {
                return job(res)
            }
            val savedLocale = conf.locale
            try {
                conf.locale = newLocale
                res.updateConfiguration(conf, null)
                return job(res)
            } finally {
                conf.locale = savedLocale
                res.updateConfiguration(conf, null)
            }
        }
    }

    companion object {
        private val sLockForRunInLocale = Any()
    }
}
