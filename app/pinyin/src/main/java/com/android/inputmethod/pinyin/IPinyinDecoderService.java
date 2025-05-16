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


package com.android.inputmethod.pinyin;

import android.os.Binder;
import android.os.IBinder;

import java.util.Collections;
import java.util.List;

public interface IPinyinDecoderService {
    int getInt();
    void setMaxLens(int maxSpsLen, int maxHzsLen);
    int imSearch(byte[] pyBuf, int pyLen);
    int imDelSearch(int pos, boolean is_pos_in_splid, boolean clear_fixed_this_step);
    void imResetSearch();
    int imAddLetter(byte ch);
    String imGetPyStr(boolean decoded);
    int imGetPyStrLen(boolean decoded);
    int[] imGetSplStart();
    String imGetChoice(int choiceId);
    String imGetChoices(int choicesNum);
    List<String> imGetChoiceList(int choicesStart, int choicesNum, int sentFixedLen);
    int imChoose(int choiceId);
    int imCancelLastChoice();
    int imGetFixedLen();
    boolean imCancelInput();
    void imFlushCache();
    int imGetPredictsNum(String fixedStr);
    List<String> imGetPredictList(int predictsStart, int predictsNum);
    String imGetPredictItem(int predictNo);

    String syncUserDict(String tomerge);
    boolean syncBegin();
    void syncFinish();
    int syncPutLemmas(String tomerge);
    String syncGetLemmas();
    int syncGetLastCount();
    int syncGetTotalCount();
    void syncClearLastGot();
    int imSyncGetCapacity();

    class Stub extends Binder implements IPinyinDecoderService {
        public static IPinyinDecoderService asInterface(IBinder service) {
            return (IPinyinDecoderService) service;
        }

        @Override
        public int getInt() {
            return 0;
        }

        @Override
        public void setMaxLens(int maxSpsLen, int maxHzsLen) {

        }

        @Override
        public int imSearch(byte[] pyBuf, int pyLen) {
            return 0;
        }

        @Override
        public int imDelSearch(int pos, boolean is_pos_in_splid, boolean clear_fixed_this_step) {
            return 0;
        }

        @Override
        public void imResetSearch() {

        }

        @Override
        public int imAddLetter(byte ch) {
            return 0;
        }

        @Override
        public String imGetPyStr(boolean decoded) {
            return "";
        }

        @Override
        public int imGetPyStrLen(boolean decoded) {
            return 0;
        }

        @Override
        public int[] imGetSplStart() {
            return new int[0];
        }

        @Override
        public String imGetChoice(int choiceId) {
            return "";
        }

        @Override
        public String imGetChoices(int choicesNum) {
            return "";
        }

        @Override
        public List<String> imGetChoiceList(int choicesStart, int choicesNum, int sentFixedLen) {
            return Collections.emptyList();
        }

        @Override
        public int imChoose(int choiceId) {
            return 0;
        }

        @Override
        public int imCancelLastChoice() {
            return 0;
        }

        @Override
        public int imGetFixedLen() {
            return 0;
        }

        @Override
        public boolean imCancelInput() {
            return false;
        }

        @Override
        public void imFlushCache() {

        }

        @Override
        public int imGetPredictsNum(String fixedStr) {
            return 0;
        }

        @Override
        public List<String> imGetPredictList(int predictsStart, int predictsNum) {
            return Collections.emptyList();
        }

        @Override
        public String imGetPredictItem(int predictNo) {
            return "";
        }

        @Override
        public String syncUserDict(String tomerge) {
            return "";
        }

        @Override
        public boolean syncBegin() {
            return false;
        }

        @Override
        public void syncFinish() {

        }

        @Override
        public int syncPutLemmas(String tomerge) {
            return 0;
        }

        @Override
        public String syncGetLemmas() {
            return "";
        }

        @Override
        public int syncGetLastCount() {
            return 0;
        }

        @Override
        public int syncGetTotalCount() {
            return 0;
        }

        @Override
        public void syncClearLastGot() {

        }

        @Override
        public int imSyncGetCapacity() {
            return 0;
        }
    }
}
