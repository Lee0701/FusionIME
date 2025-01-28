/**
 * Copyright (C) 2013 The Android Open Source Project
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

import android.view.View

/**
 * Helper class to maintain the interface state of word list preferences.
 *
 * This is necessary because the views are created on-demand by calling code. There are many
 * situations where views are renewed with little relation with user interaction. For example,
 * when scrolling, the view is reused so it doesn't keep its state, which means we need to keep
 * it separately. Also whenever the underlying dictionary list undergoes a change (for example,
 * update the metadata, or finish downloading) the whole list has to be thrown out and recreated
 * in case some dictionaries appeared, disappeared, changed states etc.
 */
class DictionaryListInterfaceState {
    internal class State {
        var mOpen: Boolean = false
        var mStatus: Int = MetadataDbHelper.STATUS_UNKNOWN
    }

    private val mWordlistToState: HashMap<String, State> = HashMap()
    private val mViewCache: ArrayList<View> = ArrayList()

    fun isOpen(wordlistId: String): Boolean {
        val state: State? = mWordlistToState.get(wordlistId)
        if (null == state) return false
        return state.mOpen
    }

    fun getStatus(wordlistId: String): Int {
        val state: State? = mWordlistToState.get(wordlistId)
        if (null == state) return MetadataDbHelper.STATUS_UNKNOWN
        return state.mStatus
    }

    fun setOpen(wordlistId: String, status: Int) {
        val newState: State
        val state: State? = mWordlistToState.get(wordlistId)
        newState = if (null == state) State() else state
        newState.mOpen = true
        newState.mStatus = status
        mWordlistToState.put(wordlistId, newState)
    }

    fun closeAll() {
        for (state: State in mWordlistToState.values) {
            state.mOpen = false
        }
    }

    fun findFirstOrphanedView(): View? {
        for (v: View in mViewCache) {
            if (null == v.getParent()) return v
        }
        return null
    }

    fun addToCacheAndReturnView(view: View): View {
        mViewCache.add(view)
        return view
    }

    fun removeFromCache(view: View) {
        mViewCache.remove(view)
    }
}
