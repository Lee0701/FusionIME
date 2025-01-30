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
import android.media.AudioManager

/**
 * Class used to manage related sound resources.
 */
class SoundManager private constructor(private val mContext: Context) {
    private var mAudioManager: AudioManager? = null

    // Align sound effect volume on music volume
    private val FX_VOLUME = -1.0f
    private var mSilentMode = false

    init {
        updateRingerMode()
    }

    fun updateRingerMode() {
        if (mAudioManager == null) {
            mAudioManager = mContext
                .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        mSilentMode = (mAudioManager!!.ringerMode != AudioManager.RINGER_MODE_NORMAL)
    }

    fun playKeyDown() {
        if (mAudioManager == null) {
            updateRingerMode()
        }
        if (!mSilentMode) {
            val sound = AudioManager.FX_KEYPRESS_STANDARD
            mAudioManager!!.playSoundEffect(sound, FX_VOLUME)
        }
    }

    companion object {
        private var mInstance: SoundManager? = null
        fun getInstance(context: Context?): SoundManager? {
            if (null == mInstance) {
                if (null != context) {
                    mInstance = SoundManager(context)
                }
            }
            return mInstance
        }
    }
}
