/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.content.Context
import java.util.Vector

/**
 * Class used to cache previously loaded soft keyboard layouts.
 */
class SkbPool private constructor() {
    private val mSkbTemplates = Vector<SkbTemplate>()
    private val mSoftKeyboards = Vector<SoftKeyboard>()

    fun resetCachedSkb() {
        mSoftKeyboards.clear()
    }

    fun getSkbTemplate(skbTemplateId: Int, context: Context?): SkbTemplate? {
        for (i in mSkbTemplates.indices) {
            val t = mSkbTemplates.elementAt(i)
            if (t.skbTemplateId == skbTemplateId) {
                return t
            }
        }

        if (null != context) {
            val xkbl = XmlKeyboardLoader(context)
            val t = xkbl.loadSkbTemplate(skbTemplateId)
            if (null != t) {
                mSkbTemplates.add(t)
                return t
            }
        }
        return null
    }

    // Try to find the keyboard in the pool with the cache id. If there is no
    // keyboard found, try to load it with the given xml id.
    fun getSoftKeyboard(
        skbCacheId: Int, skbXmlId: Int,
        skbWidth: Int, skbHeight: Int, context: Context?
    ): SoftKeyboard? {
        for (i in mSoftKeyboards.indices) {
            val skb = mSoftKeyboards.elementAt(i)
            if (skb.cacheId == skbCacheId && skb.skbXmlId == skbXmlId) {
                skb.setSkbCoreSize(skbWidth, skbHeight)
                skb.newlyLoadedFlag = false
                return skb
            }
        }
        if (null != context) {
            val xkbl = XmlKeyboardLoader(context)
            val skb = xkbl.loadKeyboard(skbXmlId, skbWidth, skbHeight)
            if (skb != null) {
                if (skb.cacheFlag) {
                    skb.cacheId = skbCacheId
                    mSoftKeyboards.add(skb)
                }
            }
            return skb
        }
        return null
    }

    companion object {
        private var mInstance: SkbPool? = null

        val instance: SkbPool
            get() {
                if (null == mInstance) mInstance = SkbPool()
                return mInstance!!
            }
    }
}
