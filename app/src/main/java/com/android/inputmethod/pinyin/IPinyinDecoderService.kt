/* //com/andriod/inputmethod/pinyin/IPinyinDecoderService.aidl
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

import android.os.Binder
import android.os.IBinder

interface IPinyinDecoderService {
    val int: Int

    fun setMaxLens(maxSpsLen: Int, maxHzsLen: Int)
    fun imSearch(pyBuf: ByteArray?, pyLen: Int): Int
    fun imDelSearch(pos: Int, is_pos_in_splid: Boolean, clear_fixed_this_step: Boolean): Int
    fun imResetSearch()
    fun imAddLetter(ch: Byte): Int
    fun imGetPyStr(decoded: Boolean): String
    fun imGetPyStrLen(decoded: Boolean): Int
    fun imGetSplStart(): IntArray
    fun imGetChoice(choiceId: Int): String
    fun imGetChoices(choicesNum: Int): String?
    fun imGetChoiceList(choicesStart: Int, choicesNum: Int, sentFixedLen: Int): List<String>
    fun imChoose(choiceId: Int): Int
    fun imCancelLastChoice(): Int
    fun imGetFixedLen(): Int
    fun imCancelInput(): Boolean
    fun imFlushCache()
    fun imGetPredictsNum(fixedStr: String?): Int
    fun imGetPredictList(predictsStart: Int, predictsNum: Int): List<String>
    fun imGetPredictItem(predictNo: Int): String

    fun syncUserDict(tomerge: String?): String?
    fun syncBegin(): Boolean
    fun syncFinish()
    fun syncPutLemmas(tomerge: String?): Int
    fun syncGetLemmas(): String
    fun syncGetLastCount(): Int
    fun syncGetTotalCount(): Int
    fun syncClearLastGot()
    fun imSyncGetCapacity(): Int

    open class Stub : Binder(), IPinyinDecoderService {

        override val int: Int = 0

        override fun setMaxLens(maxSpsLen: Int, maxHzsLen: Int) {
        }

        override fun imSearch(pyBuf: ByteArray?, pyLen: Int): Int {
            return 0
        }

        override fun imDelSearch(
            pos: Int,
            is_pos_in_splid: Boolean,
            clear_fixed_this_step: Boolean
        ): Int {
            return 0
        }

        override fun imResetSearch() {
        }

        override fun imAddLetter(ch: Byte): Int {
            return 0
        }

        override fun imGetPyStr(decoded: Boolean): String {
            return ""
        }

        override fun imGetPyStrLen(decoded: Boolean): Int {
            return 0
        }

        override fun imGetSplStart(): IntArray {
            return IntArray(0)
        }

        override fun imGetChoice(choiceId: Int): String {
            return ""
        }

        override fun imGetChoices(choicesNum: Int): String? {
            return ""
        }

        override fun imGetChoiceList(
            choicesStart: Int,
            choicesNum: Int,
            sentFixedLen: Int
        ): List<String> {
            return emptyList()
        }

        override fun imChoose(choiceId: Int): Int {
            return 0
        }

        override fun imCancelLastChoice(): Int {
            return 0
        }

        override fun imGetFixedLen(): Int {
            return 0
        }

        override fun imCancelInput(): Boolean {
            return false
        }

        override fun imFlushCache() {
        }

        override fun imGetPredictsNum(fixedStr: String?): Int {
            return 0
        }

        override fun imGetPredictList(predictsStart: Int, predictsNum: Int): List<String> {
            return emptyList()
        }

        override fun imGetPredictItem(predictNo: Int): String {
            return ""
        }

        override fun syncUserDict(tomerge: String?): String? {
            return ""
        }

        override fun syncBegin(): Boolean {
            return false
        }

        override fun syncFinish() {
        }

        override fun syncPutLemmas(tomerge: String?): Int {
            return 0
        }

        override fun syncGetLemmas(): String {
            return ""
        }

        override fun syncGetLastCount(): Int {
            return 0
        }

        override fun syncGetTotalCount(): Int {
            return 0
        }

        override fun syncClearLastGot() {
        }

        override fun imSyncGetCapacity(): Int {
            return 0
        }

        companion object {
            fun asInterface(service: IBinder?): IPinyinDecoderService? {
                return service as IPinyinDecoderService?
            }
        }
    }
}
