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
package com.android.inputmethod.keyboard.internal

import com.android.inputmethod.keyboard.Key

abstract class UniqueKeysCache {
    abstract fun setEnabled(enabled: Boolean)
    abstract fun clear()

    abstract fun getUniqueKey(key: Key): Key

    private class UniqueKeysCacheImpl : UniqueKeysCache() {
        private val mCache: HashMap<Key, Key>

        private var mEnabled: Boolean = false

        init {
            mCache = HashMap()
        }

        override fun setEnabled(enabled: Boolean) {
            mEnabled = enabled
        }

        override fun clear() {
            mCache.clear()
        }

        override fun getUniqueKey(key: Key): Key {
            if (!mEnabled) {
                return key
            }
            val existingKey: Key? = mCache.get(key)
            if (existingKey != null) {
                // Reuse the existing object that equals to "key" without adding "key" to
                // the cache.
                return existingKey
            }
            mCache.put(key, key)
            return key
        }
    }

    companion object {
        val NO_CACHE: UniqueKeysCache = object : UniqueKeysCache() {
            override fun setEnabled(enabled: Boolean) {}

            override fun clear() {}

            override fun getUniqueKey(key: Key): Key {
                return key
            }
        }

        fun newInstance(): UniqueKeysCache {
            return UniqueKeysCacheImpl()
        }
    }
}
