/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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
package jp.co.omronsoft.openwnn

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.LinearLayout

/**
 * The view of the base input.
 */
class BaseInputView : LinearLayout {
    /** The dialog that opens with long tap  */
    var mOptionsDialog: AlertDialog? = null

    /**
     * Constructor
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Constructor
     */
    internal constructor(context: Context?) : super(context)

    /**
     * Called when the window containing has change its visibility.
     *
     * @see android.view.View.onWindowVisibilityChanged
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if ((visibility != VISIBLE) && (mOptionsDialog != null)) {
            mOptionsDialog!!.dismiss()
        }
    }

    /**
     * Show dialog.
     *
     * @param builder   the builder of dialog
     */
    fun showDialog(builder: AlertDialog.Builder) {
        if (mOptionsDialog != null) {
            mOptionsDialog!!.dismiss()
        }

        mOptionsDialog = builder.create()
        val window = mOptionsDialog?.getWindow()
        val dialogLayoutParams = window!!.attributes
        dialogLayoutParams.token = windowToken
        dialogLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window.attributes = dialogLayoutParams
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        mOptionsDialog?.show()
    }

    /**
     * Close dialog.
     */
    fun closeDialog() {
        if (mOptionsDialog != null) {
            mOptionsDialog!!.dismiss()
            mOptionsDialog = null
        }
    }
}
