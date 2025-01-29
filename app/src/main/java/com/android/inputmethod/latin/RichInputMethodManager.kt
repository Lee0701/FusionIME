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
package com.android.inputmethod.latin

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.AsyncTask
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.compat.InputMethodManagerCompatWrapper
import com.android.inputmethod.compat.InputMethodSubtypeCompatUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils
import com.android.inputmethod.latin.utils.LanguageOnSpacebarUtils
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.Locale

/**
 * Enrichment class for InputMethodManager to simplify interaction and add functionality.
 */
// non final for easy mocking.
class RichInputMethodManager private constructor() {
    private var mContext: Context? = null
    private var mImmWrapper: InputMethodManagerCompatWrapper? = null
    private var mInputMethodInfoCache: InputMethodInfoCache? = null
    private var mCurrentRichInputMethodSubtype: RichInputMethodSubtype? = null
    private var mShortcutInputMethodInfo: InputMethodInfo? = null
    private var mShortcutSubtype: InputMethodSubtype? = null

    private val isInitialized: Boolean
        get() {
            return mImmWrapper != null
        }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw RuntimeException("$TAG is used before initialization")
        }
    }

    private fun initInternal(context: Context) {
        if (isInitialized) {
            return
        }
        mImmWrapper = InputMethodManagerCompatWrapper(context)
        mContext = context
        mInputMethodInfoCache = InputMethodInfoCache(
            mImmWrapper!!.mImm, context.getPackageName()
        )

        // Initialize additional subtypes.
        SubtypeLocaleUtils.init(context)
        val additionalSubtypes: Array<InputMethodSubtype> = additionalSubtypes
        mImmWrapper!!.mImm.setAdditionalInputMethodSubtypes(
            inputMethodIdOfThisIme, additionalSubtypes
        )

        // Initialize the current input method subtype and the shortcut IME.
        refreshSubtypeCaches()
    }

    val additionalSubtypes: Array<InputMethodSubtype>
        get() {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
            val prefAdditionalSubtypes: String =
                Settings.readPrefAdditionalSubtypes(
                    prefs, mContext!!.resources
                )
            return AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefAdditionalSubtypes)
        }

    val inputMethodManager: InputMethodManager
        get() {
            checkInitialized()
            return mImmWrapper!!.mImm
        }

    fun getMyEnabledInputMethodSubtypeList(
        allowsImplicitlySelectedSubtypes: Boolean
    ): List<InputMethodSubtype> {
        return getEnabledInputMethodSubtypeList(
            inputMethodInfoOfThisIme, allowsImplicitlySelectedSubtypes
        )
    }

    fun switchToNextInputMethod(token: IBinder, onlyCurrentIme: Boolean): Boolean {
        if (mImmWrapper!!.switchToNextInputMethod(token, onlyCurrentIme)) {
            return true
        }
        // Was not able to call {@link InputMethodManager#switchToNextInputMethodIBinder,boolean)}
        // because the current device is running ICS or previous and lacks the API.
        if (switchToNextInputSubtypeInThisIme(token, onlyCurrentIme)) {
            return true
        }
        return switchToNextInputMethodAndSubtype(token)
    }

    private fun switchToNextInputSubtypeInThisIme(
        token: IBinder,
        onlyCurrentIme: Boolean
    ): Boolean {
        val imm = mImmWrapper!!.mImm
        val currentSubtype: InputMethodSubtype? = imm.currentInputMethodSubtype
        val enabledSubtypes: List<InputMethodSubtype> = getMyEnabledInputMethodSubtypeList(
            true /* allowsImplicitlySelectedSubtypes */
        )
        val currentIndex: Int = getSubtypeIndexInList(currentSubtype, enabledSubtypes)
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(
                TAG, "Can't find current subtype in enabled subtypes: subtype="
                        + SubtypeLocaleUtils.getSubtypeNameForLogging(currentSubtype)
            )
            return false
        }
        val nextIndex: Int = (currentIndex + 1) % enabledSubtypes.size
        if (nextIndex <= currentIndex && !onlyCurrentIme) {
            // The current subtype is the last or only enabled one and it needs to switch to
            // next IME.
            return false
        }
        val nextSubtype: InputMethodSubtype = enabledSubtypes.get(nextIndex)
        setInputMethodAndSubtype(token, nextSubtype)
        return true
    }

    private fun switchToNextInputMethodAndSubtype(token: IBinder): Boolean {
        val imm = mImmWrapper!!.mImm
        val enabledImis: List<InputMethodInfo> = imm.enabledInputMethodList
        val currentIndex: Int = getImiIndexInList(
            inputMethodInfoOfThisIme, enabledImis
        )
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(
                TAG, "Can't find current IME in enabled IMEs: IME package="
                        + inputMethodInfoOfThisIme.getPackageName()
            )
            return false
        }
        val nextImi: InputMethodInfo = getNextNonAuxiliaryIme(currentIndex, enabledImis)
        val enabledSubtypes: List<InputMethodSubtype> = getEnabledInputMethodSubtypeList(
            nextImi,
            true /* allowsImplicitlySelectedSubtypes */
        )
        if (enabledSubtypes.isEmpty()) {
            // The next IME has no subtype.
            imm.setInputMethod(token, nextImi.id)
            return true
        }
        val firstSubtype: InputMethodSubtype = enabledSubtypes[0]
        imm.setInputMethodAndSubtype(token, nextImi.id, firstSubtype)
        return true
    }

    private class InputMethodInfoCache(imm: InputMethodManager?, imePackageName: String) {
        private val mImm: InputMethodManager? = imm
        private val mImePackageName: String = imePackageName

        private var mCachedThisImeInfo: InputMethodInfo? = null
        private val mCachedSubtypeListWithImplicitlySelected: HashMap<InputMethodInfo, List<InputMethodSubtype>> =
            HashMap()

        private val mCachedSubtypeListOnlyExplicitlySelected: HashMap<InputMethodInfo, List<InputMethodSubtype>> =
            HashMap()

        @get:Synchronized
        val inputMethodOfThisIme: InputMethodInfo
            get() {
                mCachedThisImeInfo?.let { return it }
                for (imi: InputMethodInfo in mImm!!.getInputMethodList()) {
                    if (imi.packageName == mImePackageName) {
                        mCachedThisImeInfo = imi
                        return imi
                    }
                }
                throw RuntimeException("Input method id for " + mImePackageName + " not found.")
            }

        @Synchronized
        fun getEnabledInputMethodSubtypeList(
            imi: InputMethodInfo, allowsImplicitlySelectedSubtypes: Boolean
        ): List<InputMethodSubtype> {
            val cache: HashMap<InputMethodInfo, List<InputMethodSubtype>> =
                if (allowsImplicitlySelectedSubtypes)
                    mCachedSubtypeListWithImplicitlySelected
                else
                    mCachedSubtypeListOnlyExplicitlySelected
            val cachedList: List<InputMethodSubtype>? = cache.get(imi)
            if (cachedList != null) {
                return cachedList
            }
            val result: List<InputMethodSubtype> = mImm!!.getEnabledInputMethodSubtypeList(
                imi, allowsImplicitlySelectedSubtypes
            )
            cache.put(imi, result)
            return result
        }

        @Synchronized
        fun clear() {
            mCachedThisImeInfo = null
            mCachedSubtypeListWithImplicitlySelected.clear()
            mCachedSubtypeListOnlyExplicitlySelected.clear()
        }
    }

    val inputMethodInfoOfThisIme: InputMethodInfo
        get() {
            return mInputMethodInfoCache?.inputMethodOfThisIme!!
        }

    val inputMethodIdOfThisIme: String
        get() {
            return inputMethodInfoOfThisIme.getId()
        }

    fun checkIfSubtypeBelongsToThisImeAndEnabled(subtype: InputMethodSubtype?): Boolean {
        return checkIfSubtypeBelongsToList(
            subtype,
            getEnabledInputMethodSubtypeList(
                inputMethodInfoOfThisIme,
                true /* allowsImplicitlySelectedSubtypes */
            )
        )
    }

    fun checkIfSubtypeBelongsToThisImeAndImplicitlyEnabled(
        subtype: InputMethodSubtype?
    ): Boolean {
        val subtypeEnabled: Boolean = checkIfSubtypeBelongsToThisImeAndEnabled(subtype)
        val subtypeExplicitlyEnabled: Boolean = checkIfSubtypeBelongsToList(
            subtype,
            getMyEnabledInputMethodSubtypeList(false /* allowsImplicitlySelectedSubtypes */)
        )
        return subtypeEnabled && !subtypeExplicitlyEnabled
    }

    fun onSubtypeChanged(newSubtype: InputMethodSubtype) {
        updateCurrentSubtype(newSubtype)
        updateShortcutIme()
        if (DEBUG) {
            Log.w(TAG, "onSubtypeChanged: " + mCurrentRichInputMethodSubtype?.nameForLogging)
        }
    }

    val currentSubtypeLocale: Locale?
        get() = sForcedSubtypeForTesting?.locale ?: currentSubtype?.locale

    val currentSubtype: RichInputMethodSubtype?
        get() = sForcedSubtypeForTesting ?: mCurrentRichInputMethodSubtype


    val combiningRulesExtraValueOfCurrentSubtype: String
        get() {
            return SubtypeLocaleUtils.getCombiningRulesExtraValue(currentSubtype?.rawSubtype!!)
        }

    fun hasMultipleEnabledIMEsOrSubtypes(shouldIncludeAuxiliarySubtypes: Boolean): Boolean {
        val enabledImis: List<InputMethodInfo> = mImmWrapper!!.mImm.getEnabledInputMethodList()
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, enabledImis)
    }

    fun hasMultipleEnabledSubtypesInThisIme(
        shouldIncludeAuxiliarySubtypes: Boolean
    ): Boolean {
        val imiList: List<InputMethodInfo> = listOf(
            inputMethodInfoOfThisIme
        )
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, imiList)
    }

    private fun hasMultipleEnabledSubtypes(
        shouldIncludeAuxiliarySubtypes: Boolean,
        imiList: List<InputMethodInfo>
    ): Boolean {
        // Number of the filtered IMEs
        var filteredImisCount: Int = 0

        for (imi: InputMethodInfo in imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) return true
            val subtypes: List<InputMethodSubtype> = getEnabledInputMethodSubtypeList(imi, true)
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount
                continue
            }

            var auxCount: Int = 0
            for (subtype: InputMethodSubtype in subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount
                }
            }
            val nonAuxCount: Int = subtypes.size - auxCount

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount
            }
        }

        if (filteredImisCount > 1) {
            return true
        }
        val subtypes: List<InputMethodSubtype> = getMyEnabledInputMethodSubtypeList(true)
        var keyboardCount: Int = 0
        // imm.getEnabledInputMethodSubtypeList(null, true) will return the current IME's
        // both explicitly and implicitly enabled input method subtype.
        // (The current IME should be LatinIME.)
        for (subtype: InputMethodSubtype in subtypes) {
            if (Constants.Subtype.KEYBOARD_MODE == subtype.getMode()) {
                ++keyboardCount
            }
        }
        return keyboardCount > 1
    }

    fun findSubtypeByLocaleAndKeyboardLayoutSet(
        localeString: String,
        keyboardLayoutSetName: String
    ): InputMethodSubtype? {
        val myImi: InputMethodInfo = inputMethodInfoOfThisIme
        val count: Int = myImi.subtypeCount
        for (i in 0 until count) {
            val subtype: InputMethodSubtype = myImi.getSubtypeAt(i)
            val layoutName: String = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype)
            if (localeString == subtype.locale
                && keyboardLayoutSetName == layoutName
            ) {
                return subtype
            }
        }
        return null
    }

    fun findSubtypeByLocale(locale: Locale): InputMethodSubtype? {
        // Find the best subtype based on a straightforward matching algorithm.
        // TODO: Use LocaleList#getFirstMatch() instead.
        val subtypes: List<InputMethodSubtype> =
            getMyEnabledInputMethodSubtypeList(true /* allowsImplicitlySelectedSubtypes */)
        val count: Int = subtypes.size
        for (i in 0 until count) {
            val subtype: InputMethodSubtype = subtypes.get(i)
            val subtypeLocale: Locale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype)
            if (subtypeLocale == locale) {
                return subtype
            }
        }
        for (i in 0 until count) {
            val subtype: InputMethodSubtype = subtypes.get(i)
            val subtypeLocale: Locale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype)
            if (subtypeLocale.getLanguage() == locale.getLanguage() &&
                subtypeLocale.getCountry() == locale.getCountry() &&
                subtypeLocale.getVariant() == locale.getVariant()
            ) {
                return subtype
            }
        }
        for (i in 0 until count) {
            val subtype: InputMethodSubtype = subtypes.get(i)
            val subtypeLocale: Locale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype)
            if (subtypeLocale.language == locale.language &&
                subtypeLocale.country == locale.country
            ) {
                return subtype
            }
        }
        for (i in 0 until count) {
            val subtype: InputMethodSubtype = subtypes.get(i)
            val subtypeLocale: Locale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype)
            if (subtypeLocale.language == locale.language) {
                return subtype
            }
        }
        return null
    }

    fun setInputMethodAndSubtype(token: IBinder, subtype: InputMethodSubtype?) {
        mImmWrapper?.mImm?.setInputMethodAndSubtype(
            token, inputMethodIdOfThisIme, subtype
        )
    }

    fun setAdditionalInputMethodSubtypes(subtypes: Array<InputMethodSubtype>) {
        mImmWrapper?.mImm?.setAdditionalInputMethodSubtypes(
            inputMethodIdOfThisIme, subtypes
        )
        // Clear the cache so that we go read the {@link InputMethodInfo} of this IME and list of
        // subtypes again next time.
        refreshSubtypeCaches()
    }

    private fun getEnabledInputMethodSubtypeList(
        imi: InputMethodInfo,
        allowsImplicitlySelectedSubtypes: Boolean
    ): List<InputMethodSubtype> {
        return mInputMethodInfoCache?.getEnabledInputMethodSubtypeList(
            imi, allowsImplicitlySelectedSubtypes
        ).orEmpty()
    }

    fun refreshSubtypeCaches() {
        mInputMethodInfoCache!!.clear()
        updateCurrentSubtype(mImmWrapper?.mImm?.currentInputMethodSubtype)
        updateShortcutIme()
    }

    fun shouldOfferSwitchingToNextInputMethod(
        binder: IBinder?,
        defaultValue: Boolean
    ): Boolean {
        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= VERSION_CODES.KITKAT) {
            return defaultValue
        }
        return mImmWrapper?.shouldOfferSwitchingToNextInputMethod(binder) == true
    }

    val isSystemLocaleSameAsLocaleOfAllEnabledSubtypesOfEnabledImes: Boolean
        get() {
            val systemLocale: Locale = mContext!!.resources.configuration.locale
            val enabledSubtypesOfEnabledImes: MutableSet<InputMethodSubtype> =
                HashSet()
            val inputMethodManager = inputMethodManager
            val enabledInputMethodInfoList: List<InputMethodInfo> =
                inputMethodManager.enabledInputMethodList
            for (info: InputMethodInfo? in enabledInputMethodInfoList) {
                val enabledSubtypes: List<InputMethodSubtype> =
                    inputMethodManager.getEnabledInputMethodSubtypeList(
                        info, true /* allowsImplicitlySelectedSubtypes */
                    )
                if (enabledSubtypes.isEmpty()) {
                    // An IME with no subtypes is found.
                    return false
                }
                enabledSubtypesOfEnabledImes.addAll(enabledSubtypes)
            }
            for (subtype: InputMethodSubtype in enabledSubtypesOfEnabledImes) {
                if (!subtype.isAuxiliary && subtype.locale.isNotEmpty()
                    && systemLocale != SubtypeLocaleUtils.getSubtypeLocale(subtype)
                ) {
                    return false
                }
            }
            return true
        }

    private fun updateCurrentSubtype(subtype: InputMethodSubtype?) {
        mCurrentRichInputMethodSubtype =
            RichInputMethodSubtype.getRichInputMethodSubtype(subtype)
    }

    private fun updateShortcutIme() {
        if (DEBUG) {
            val id = if (mShortcutInputMethodInfo == null) "<null>" else mShortcutInputMethodInfo?.id
            val locale = if (mShortcutSubtype == null) "<null>" else (mShortcutSubtype!!.locale + ", " + mShortcutSubtype!!.mode)
            Log.d(
                TAG, "Update shortcut IME from : $id, $locale"
            )
        }
        val richSubtype = mCurrentRichInputMethodSubtype
        val implicitlyEnabledSubtype = checkIfSubtypeBelongsToThisImeAndImplicitlyEnabled(
            richSubtype?.rawSubtype
        )
        val systemLocale: Locale = mContext!!.resources.configuration.locale
        LanguageOnSpacebarUtils.onSubtypeChanged(
            richSubtype!!, implicitlyEnabledSubtype, systemLocale
        )
        LanguageOnSpacebarUtils.setEnabledSubtypes(
            getMyEnabledInputMethodSubtypeList(
                true /* allowsImplicitlySelectedSubtypes */
            )
        )

        // TODO: Update an icon for shortcut IME
        val shortcuts: Map<InputMethodInfo, List<InputMethodSubtype>> =
            inputMethodManager.getShortcutInputMethodsAndSubtypes()
        mShortcutInputMethodInfo = null
        mShortcutSubtype = null
        for (imi: InputMethodInfo in shortcuts.keys) {
            val subtypes: List<InputMethodSubtype>? = shortcuts[imi]
            // TODO: Returns the first found IMI for now. Should handle all shortcuts as
            // appropriate.
            mShortcutInputMethodInfo = imi
            // TODO: Pick up the first found subtype for now. Should handle all subtypes
            // as appropriate.
            mShortcutSubtype = if (subtypes!!.isNotEmpty()) subtypes[0] else null
            break
        }
        if (DEBUG) {
            val shortcutInputMethodInfo = this.mShortcutInputMethodInfo
            Log.d(
                TAG, ("Update shortcut IME to : "
                        + (if (shortcutInputMethodInfo == null)
                    "<null>"
                else
                    shortcutInputMethodInfo.id) + ", "
                        + (if (mShortcutSubtype == null) "<null>" else (mShortcutSubtype?.locale + ", " + mShortcutSubtype?.mode)))
            )
        }
    }

    fun switchToShortcutIme(context: InputMethodService) {
        if (mShortcutInputMethodInfo == null) {
            return
        }

        val imiId: String = mShortcutInputMethodInfo!!.id
        switchToTargetIME(imiId, mShortcutSubtype, context)
    }

    private fun switchToTargetIME(
        imiId: String, subtype: InputMethodSubtype?,
        context: InputMethodService
    ) {
        val token: IBinder = context.window.window!!.attributes.token ?: return
        val imm = inputMethodManager
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                imm!!.setInputMethodAndSubtype(token, imiId, subtype)
                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    val isShortcutImeReady: Boolean
        get() {
            if (mShortcutInputMethodInfo == null) {
                return false
            }
            if (mShortcutSubtype == null) {
                return true
            }
            return true
        }

    companion object {
        private val TAG: String = RichInputMethodManager::class.java.getSimpleName()
        private const val DEBUG: Boolean = false

        private val sInstance: RichInputMethodManager = RichInputMethodManager()

        private val INDEX_NOT_FOUND: Int = -1

        val instance: RichInputMethodManager
            get() {
                sInstance.checkInitialized()
                return sInstance
            }

        fun init(context: Context) {
            sInstance.initInternal(context)
        }

        private fun getImiIndexInList(
            inputMethodInfo: InputMethodInfo,
            imiList: List<InputMethodInfo>
        ): Int {
            val count: Int = imiList.size
            for (index in 0 until count) {
                val imi: InputMethodInfo = imiList.get(index)
                if (imi == inputMethodInfo) {
                    return index
                }
            }
            return INDEX_NOT_FOUND
        }

        // This method mimics {@link InputMethodManager#switchToNextInputMethod(IBinder,boolean)}.
        private fun getNextNonAuxiliaryIme(
            currentIndex: Int,
            imiList: List<InputMethodInfo>
        ): InputMethodInfo {
            val count: Int = imiList.size
            for (i in 1 until count) {
                val nextIndex: Int = (currentIndex + i) % count
                val nextImi: InputMethodInfo = imiList.get(nextIndex)
                if (!isAuxiliaryIme(nextImi)) {
                    return nextImi
                }
            }
            return imiList.get(currentIndex)
        }

        // Copied from {@link InputMethodInfo}. See how auxiliary of IME is determined.
        private fun isAuxiliaryIme(imi: InputMethodInfo): Boolean {
            val count: Int = imi.getSubtypeCount()
            if (count == 0) {
                return false
            }
            for (index in 0 until count) {
                val subtype: InputMethodSubtype = imi.getSubtypeAt(index)
                if (!subtype.isAuxiliary()) {
                    return false
                }
            }
            return true
        }

        private fun checkIfSubtypeBelongsToList(
            subtype: InputMethodSubtype?,
            subtypes: List<InputMethodSubtype>
        ): Boolean {
            return getSubtypeIndexInList(subtype, subtypes) != INDEX_NOT_FOUND
        }

        private fun getSubtypeIndexInList(
            subtype: InputMethodSubtype?,
            subtypes: List<InputMethodSubtype>
        ): Int {
            val count: Int = subtypes.size
            for (index in 0 until count) {
                val ims: InputMethodSubtype = subtypes.get(index)
                if (ims == subtype) {
                    return index
                }
            }
            return INDEX_NOT_FOUND
        }

        private var sForcedSubtypeForTesting: RichInputMethodSubtype? = null

        @UsedForTesting
        fun forceSubtype(subtype: InputMethodSubtype) {
            sForcedSubtypeForTesting = RichInputMethodSubtype.getRichInputMethodSubtype(subtype)
        }
    }
}
