/*
 * Copyright (C) 2010 The Android Open Source Project
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

/**
 * Debug settings for the application.
 *
 * Note: Even though these settings are stored in the default shared preferences file,
 * they shouldn't be restored across devices.
 * If a new key is added here, it should also be blacklisted for restore in
 * [LocalSettingsConstants].
 */
object DebugSettings {
    const val PREF_DEBUG_MODE: String = "debug_mode"
    const val PREF_FORCE_NON_DISTINCT_MULTITOUCH: String = "force_non_distinct_multitouch"
    const val PREF_HAS_CUSTOM_KEY_PREVIEW_ANIMATION_PARAMS: String =
        "pref_has_custom_key_preview_animation_params"
    const val PREF_RESIZE_KEYBOARD: String = "pref_resize_keyboard"
    const val PREF_KEYBOARD_HEIGHT_SCALE: String = "pref_keyboard_height_scale"
    const val PREF_KEY_PREVIEW_DISMISS_DURATION: String = "pref_key_preview_dismiss_duration"
    const val PREF_KEY_PREVIEW_DISMISS_END_X_SCALE: String = "pref_key_preview_dismiss_end_x_scale"
    const val PREF_KEY_PREVIEW_DISMISS_END_Y_SCALE: String = "pref_key_preview_dismiss_end_y_scale"
    const val PREF_KEY_PREVIEW_SHOW_UP_DURATION: String = "pref_key_preview_show_up_duration"
    const val PREF_KEY_PREVIEW_SHOW_UP_START_X_SCALE: String =
        "pref_key_preview_show_up_start_x_scale"
    const val PREF_KEY_PREVIEW_SHOW_UP_START_Y_SCALE: String =
        "pref_key_preview_show_up_start_y_scale"
    const val PREF_SHOULD_SHOW_LXX_SUGGESTION_UI: String = "pref_should_show_lxx_suggestion_ui"
    const val PREF_SLIDING_KEY_INPUT_PREVIEW: String = "pref_sliding_key_input_preview"
}
