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

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.os.IBinder
import android.util.Log
import ee.oyatl.ime.fusion.R
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Vector

/**
 * This class is used to separate the input method kernel in an individual
 * service so that both IME and IME-syncer can use it.
 */
class PinyinDecoderService : Service() {
    private var mUsr_dict_file: String? = null

    // Get file name of the specified dictionary
    private fun getUsrDictFileName(usr_dict: ByteArray?): Boolean {
        if (null == usr_dict) {
            return false
        }

        for (i in 0 until mUsr_dict_file!!.length) usr_dict[i] =
            mUsr_dict_file!![i].code.toByte()
        usr_dict[mUsr_dict_file!!.length] = 0

        return true
    }

    private fun initPinyinEngine() {
        val usr_dict: ByteArray
        usr_dict = ByteArray(MAX_PATH_FILE_LENGTH)

        // Here is how we open a built-in dictionary for access through
        // a file descriptor...
        val afd: AssetFileDescriptor = getResources().openRawResourceFd(
            R.raw.dict_pinyin
        )
        if (Environment.instance.needDebug()) {
            Log
                .i(
                    "foo", ("Dict: start=" + afd.getStartOffset()
                            + ", length=" + afd.getLength() + ", fd="
                            + afd.getParcelFileDescriptor())
                )
        }
        if (getUsrDictFileName(usr_dict)) {
            inited = nativeImOpenDecoderFd(
                afd.getFileDescriptor(), afd
                    .getStartOffset(), afd.getLength(), usr_dict
            )
        }
        try {
            afd.close()
        } catch (e: IOException) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        mUsr_dict_file = getFileStreamPath("usr_dict.dat").getPath()
        // This is a hack to make sure our "files" directory has been
        // created.
        try {
            openFileOutput("dummy", 0).close()
        } catch (e: FileNotFoundException) {
        } catch (e: IOException) {
        }

        initPinyinEngine()
    }

    override fun onDestroy() {
        nativeImCloseDecoder()
        inited = false
        super.onDestroy()
    }

    private val mBinder: IPinyinDecoderService.Stub = object : IPinyinDecoderService.Stub() {

        override val int: Int = 12345

        override fun setMaxLens(maxSpsLen: Int, maxHzsLen: Int) {
            nativeImSetMaxLens(maxSpsLen, maxHzsLen)
        }

        override fun imSearch(pyBuf: ByteArray?, pyLen: Int): Int {
            return nativeImSearch(pyBuf, pyLen)
        }

        override fun imDelSearch(
            pos: Int, is_pos_in_splid: Boolean,
            clear_fixed_this_step: Boolean
        ): Int {
            return nativeImDelSearch(
                pos, is_pos_in_splid,
                clear_fixed_this_step
            )
        }

        override fun imResetSearch() {
            nativeImResetSearch()
        }

        override fun imAddLetter(ch: Byte): Int {
            return nativeImAddLetter(ch)
        }

        override fun imGetPyStr(decoded: Boolean): String {
            return nativeImGetPyStr(decoded)
        }

        override fun imGetPyStrLen(decoded: Boolean): Int {
            return nativeImGetPyStrLen(decoded)
        }

        override fun imGetSplStart(): IntArray {
            return nativeImGetSplStart()
        }

        override fun imGetChoice(choiceId: Int): String {
            return nativeImGetChoice(choiceId)
        }

        override fun imGetChoices(choicesNum: Int): String? {
            var retStr: String? = null
            for (i in 0 until choicesNum) {
                if (null == retStr) retStr = nativeImGetChoice(i)
                else retStr += " " + nativeImGetChoice(i)
            }
            return retStr
        }

        override fun imGetChoiceList(
            choicesStart: Int, choicesNum: Int,
            sentFixedLen: Int
        ): List<String> {
            val choiceList: Vector<String> = Vector()
            for (i in choicesStart until choicesStart + choicesNum) {
                var retStr: String = nativeImGetChoice(i)
                if (0 == i) retStr = retStr.substring(sentFixedLen)
                choiceList.add(retStr)
            }
            return choiceList
        }

        override fun imChoose(choiceId: Int): Int {
            return nativeImChoose(choiceId)
        }

        override fun imCancelLastChoice(): Int {
            return nativeImCancelLastChoice()
        }

        override fun imGetFixedLen(): Int {
            return nativeImGetFixedLen()
        }

        override fun imCancelInput(): Boolean {
            return nativeImCancelInput()
        }

        override fun imFlushCache() {
            nativeImFlushCache()
        }

        override fun imGetPredictsNum(fixedStr: String?): Int {
            return nativeImGetPredictsNum(fixedStr)
        }

        override fun imGetPredictItem(predictNo: Int): String {
            return nativeImGetPredictItem(predictNo)
        }

        override fun imGetPredictList(predictsStart: Int, predictsNum: Int): List<String> {
            val predictList: Vector<String> = Vector()
            for (i in predictsStart until predictsStart + predictsNum) {
                predictList.add(nativeImGetPredictItem(i))
            }
            return predictList
        }

        override fun syncUserDict(tomerge: String?): String? {
            val usr_dict: ByteArray
            usr_dict = ByteArray(MAX_PATH_FILE_LENGTH)

            if (getUsrDictFileName(usr_dict)) {
                return nativeSyncUserDict(usr_dict, tomerge)
            }
            return null
        }

        override fun syncBegin(): Boolean {
            val usr_dict: ByteArray
            usr_dict = ByteArray(MAX_PATH_FILE_LENGTH)

            if (getUsrDictFileName(usr_dict)) {
                return nativeSyncBegin(usr_dict)
            }
            return false
        }

        override fun syncFinish() {
            nativeSyncFinish()
        }

        override fun syncPutLemmas(tomerge: String?): Int {
            return nativeSyncPutLemmas(tomerge)
        }

        override fun syncGetLemmas(): String {
            return nativeSyncGetLemmas()
        }

        override fun syncGetLastCount(): Int {
            return nativeSyncGetLastCount()
        }

        override fun syncGetTotalCount(): Int {
            return nativeSyncGetTotalCount()
        }

        override fun syncClearLastGot() {
            nativeSyncClearLastGot()
        }

        override fun imSyncGetCapacity(): Int {
            return nativeSyncGetCapacity()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    companion object {
        @JvmStatic
        external fun nativeImOpenDecoder(
            fn_sys_dict: ByteArray?,
            fn_usr_dict: ByteArray?
        ): Boolean

        @JvmStatic
        external fun nativeImOpenDecoderFd(
            fd: FileDescriptor?,
            startOffset: Long, length: Long, fn_usr_dict: ByteArray?
        ): Boolean

        @JvmStatic
        external fun nativeImSetMaxLens(maxSpsLen: Int, maxHzsLen: Int)

        @JvmStatic
        external fun nativeImCloseDecoder(): Boolean

        @JvmStatic
        external fun nativeImSearch(pyBuf: ByteArray?, pyLen: Int): Int

        @JvmStatic
        external fun nativeImDelSearch(
            pos: Int, is_pos_in_splid: Boolean,
            clear_fixed_this_step: Boolean
        ): Int

        @JvmStatic
        external fun nativeImResetSearch()

        @JvmStatic
        external fun nativeImAddLetter(ch: Byte): Int

        @JvmStatic
        external fun nativeImGetPyStr(decoded: Boolean): String

        @JvmStatic
        external fun nativeImGetPyStrLen(decoded: Boolean): Int

        @JvmStatic
        external fun nativeImGetSplStart(): IntArray

        @JvmStatic
        external fun nativeImGetChoice(choiceId: Int): String

        @JvmStatic
        external fun nativeImChoose(choiceId: Int): Int

        @JvmStatic
        external fun nativeImCancelLastChoice(): Int

        @JvmStatic
        external fun nativeImGetFixedLen(): Int

        @JvmStatic
        external fun nativeImCancelInput(): Boolean

        @JvmStatic
        external fun nativeImFlushCache(): Boolean

        @JvmStatic
        external fun nativeImGetPredictsNum(fixedStr: String?): Int

        @JvmStatic
        external fun nativeImGetPredictItem(predictNo: Int): String

        // Sync related
        @JvmStatic
        external fun nativeSyncUserDict(user_dict: ByteArray?, tomerge: String?): String?

        @JvmStatic
        external fun nativeSyncBegin(user_dict: ByteArray?): Boolean

        @JvmStatic
        external fun nativeSyncFinish(): Boolean

        @JvmStatic
        external fun nativeSyncGetLemmas(): String

        @JvmStatic
        external fun nativeSyncPutLemmas(tomerge: String?): Int

        @JvmStatic
        external fun nativeSyncGetLastCount(): Int

        @JvmStatic
        external fun nativeSyncGetTotalCount(): Int

        @JvmStatic
        external fun nativeSyncClearLastGot(): Boolean

        @JvmStatic
        external fun nativeSyncGetCapacity(): Int

        private const val MAX_PATH_FILE_LENGTH: Int = 100
        private var inited: Boolean = false

        init {
            try {
                System.loadLibrary("jni_pinyinime")
            } catch (ule: UnsatisfiedLinkError) {
                Log.e(
                    "PinyinDecoderService",
                    "WARNING: Could not load jni_pinyinime natives"
                )
            }
        }
    }
}
