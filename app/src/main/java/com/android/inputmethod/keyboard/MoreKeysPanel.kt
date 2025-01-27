/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.keyboard

import android.view.View
import android.view.ViewGroup

interface MoreKeysPanel {
    interface Controller {
        /**
         * Add the [MoreKeysPanel] to the target view.
         * @param panel the panel to be shown.
         */
        fun onShowMoreKeysPanel(panel: MoreKeysPanel)

        /**
         * Remove the current [MoreKeysPanel] from the target view.
         */
        fun onDismissMoreKeysPanel()

        /**
         * Instructs the parent to cancel the panel (e.g., when entering a different input mode).
         */
        fun onCancelMoreKeysPanel()
    }

    /**
     * Initializes the layout and event handling of this [MoreKeysPanel] and calls the
     * controller's onShowMoreKeysPanel to add the panel's container view.
     *
     * @param parentView the parent view of this [MoreKeysPanel]
     * @param controller the controller that can dismiss this [MoreKeysPanel]
     * @param pointX x coordinate of this [MoreKeysPanel]
     * @param pointY y coordinate of this [MoreKeysPanel]
     * @param listener the listener that will receive keyboard action from this
     * [MoreKeysPanel].
     */
    // TODO: Currently the MoreKeysPanel is inside a container view that is added to the parent.
    // Consider the simpler approach of placing the MoreKeysPanel itself into the parent view.
    fun showMoreKeysPanel(
        parentView: View, controller: Controller, pointX: Int,
        pointY: Int, listener: KeyboardActionListener?
    )

    /**
     * Dismisses the more keys panel and calls the controller's onDismissMoreKeysPanel to remove
     * the panel's container view.
     */
    fun dismissMoreKeysPanel()

    /**
     * Process a move event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     * @param eventTime timestamp of touch point
     */
    fun onMoveEvent(x: Int, y: Int, pointerId: Int, eventTime: Long)

    /**
     * Process a down event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     * @param eventTime timestamp of touch point
     */
    fun onDownEvent(x: Int, y: Int, pointerId: Int, eventTime: Long)

    /**
     * Process an up event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     * @param eventTime timestamp of touch point
     */
    fun onUpEvent(x: Int, y: Int, pointerId: Int, eventTime: Long)

    /**
     * Translate X-coordinate of touch event to the local X-coordinate of this
     * [MoreKeysPanel].
     *
     * @param x the global X-coordinate
     * @return the local X-coordinate to this [MoreKeysPanel]
     */
    fun translateX(x: Int): Int

    /**
     * Translate Y-coordinate of touch event to the local Y-coordinate of this
     * [MoreKeysPanel].
     *
     * @param y the global Y-coordinate
     * @return the local Y-coordinate to this [MoreKeysPanel]
     */
    fun translateY(y: Int): Int

    /**
     * Show this [MoreKeysPanel] in the parent view.
     *
     * @param parentView the [ViewGroup] that hosts this [MoreKeysPanel].
     */
    fun showInParent(parentView: ViewGroup)

    /**
     * Remove this [MoreKeysPanel] from the parent view.
     */
    fun removeFromParent()

    /**
     * Return whether the panel is currently being shown.
     */
    val isShowingInParent: Boolean

    companion object {
        val EMPTY_CONTROLLER: Controller = object : Controller {
            override fun onShowMoreKeysPanel(panel: MoreKeysPanel) {}
            override fun onDismissMoreKeysPanel() {}
            override fun onCancelMoreKeysPanel() {}
        }
    }
}
