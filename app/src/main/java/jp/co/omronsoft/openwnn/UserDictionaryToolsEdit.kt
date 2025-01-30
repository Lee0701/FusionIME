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

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import ee.oyatl.ime.fusion.R

/**
 * The abstract class for user dictionary's word editor.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
abstract class UserDictionaryToolsEdit : Activity, View.OnClickListener {
    /** The class information for intent(Set this informations in the extend class)  */
    protected var mListViewName: String? = null

    /** The class information for intent(Set this informations in the extend class)  */
    protected var mPackageName: String? = null

    /** Widgets which constitute this screen of activity  */
    private var mReadEditText: EditText? = null
    private var mCandidateEditText: EditText? = null
    private var mEntryButton: Button? = null
    private var mCancelButton: Button? = null

    /** The word information which contains the previous information  */
    private var mBeforeEditWnnWord: WnnWord? = null

    /** The instance of word list activity  */
    private var mListInstance: UserDictionaryToolsList? = null

    /** The operation mode of this activity  */
    private var mRequestState = 0

    /**
     * Constructor
     */
    constructor() : super()

    /**
     * Constructor
     *
     * @param  focusView      The information of view
     * @param  focusPairView  The information of pair of view
     */
    constructor(focusView: View?, focusPairView: View?) : super() {
        sFocusingView = focusView
        sFocusingPairView = focusPairView
    }

    /**
     * Send the specified event to IME
     *
     * @param ev    The event object
     * @return      `true` if this event is processed.
     */
    protected abstract fun sendEventToIME(ev: OpenWnnEvent): Boolean

    /** @see android.app.Activity.onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* create view from XML layout */
        setContentView(R.layout.user_dictionary_tools_edit)

        /* get widgets */
        mEntryButton = findViewById<View>(R.id.addButton) as Button
        mCancelButton = findViewById<View>(R.id.cancelButton) as Button
        mReadEditText = findViewById<View>(R.id.editRead) as EditText
        mCandidateEditText = findViewById<View>(R.id.editCandidate) as EditText

        /* set the listener */
        mEntryButton!!.setOnClickListener(this)
        mCancelButton!!.setOnClickListener(this)

        /* initialize */
        mRequestState = STATE_UNKNOWN
        mReadEditText!!.setSingleLine()
        mCandidateEditText!!.setSingleLine()


        /* get the request and do it */
        val intent = intent
        val action = intent.action
        if (action == Intent.ACTION_INSERT) {
            /* add a word */
            mEntryButton!!.isEnabled = false
            mRequestState = STATE_INSERT
        } else if (action == Intent.ACTION_EDIT) {
            /* edit a word */
            mEntryButton!!.isEnabled = true
            mReadEditText!!.setText((sFocusingView as TextView).text)
            mCandidateEditText!!.setText((sFocusingPairView as TextView).text)
            mRequestState = STATE_EDIT

            /* save the word's information before this edit */
            mBeforeEditWnnWord = WnnWord()
            mBeforeEditWnnWord!!.stroke = (sFocusingView as TextView).text.toString()
            mBeforeEditWnnWord!!.candidate = (sFocusingPairView as TextView).text.toString()
        } else {
            /* finish if it is unknown request */
            Log.e("OpenWnn", "onCreate() : Invaled Get Intent. ID=$intent")
            finish()
            return
        }

        window.setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.user_dictionary_tools_edit_header
        )

        /* set control buttons */
        setAddButtonControl()
    }

    /** @see android.app.Activity.onKeyDown
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            /* go back to the word list view */
            screenTransition()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Change the state of the "Add" button into the depending state of input area.
     */
    fun setAddButtonControl() {
        /* Text changed listener for the reading text */

        mReadEditText!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                /* Enable/disable the "Add" button */
                if ((mReadEditText!!.text.toString().length != 0) &&
                    (mCandidateEditText!!.text.toString().length != 0)
                ) {
                    mEntryButton!!.isEnabled = true
                } else {
                    mEntryButton!!.isEnabled = false
                }
            }
        })
        /* Text changed listener for the candidate text */
        mCandidateEditText!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                /* Enable/disable the "Add" button */
                if ((mReadEditText!!.text.toString().length != 0) &&
                    (mCandidateEditText!!.text.toString().length != 0)
                ) {
                    mEntryButton!!.isEnabled = true
                } else {
                    mEntryButton!!.isEnabled = false
                }
            }
        })
    }

    /** @see android.view.View.OnClickListener
     */
    override fun onClick(v: View) {
        mEntryButton!!.isEnabled = false
        mCancelButton!!.isEnabled = false

        when (v.id) {
            R.id.addButton ->                 /* save the word */
                doSaveAction()

            R.id.cancelButton ->                 /* cancel the edit */
                doRevertAction()

            else -> {
                Log.e("OpenWnn", "onClick: Get Invalid ButtonID. ID=" + v.id)
                finish()
                return
            }
        }
    }

    /**
     * Process the adding or editing action
     */
    private fun doSaveAction() {
        when (mRequestState) {
            STATE_INSERT ->             /* register a word */
                if (inputDataCheck(mReadEditText!!) && inputDataCheck(mCandidateEditText!!)) {
                    val stroke = mReadEditText!!.text.toString()
                    val candidate = mCandidateEditText!!.text.toString()
                    if (addDictionary(stroke, candidate)) {
                        screenTransition()
                    }
                }

            STATE_EDIT ->             /* edit a word (=delete the word selected & add the word edited) */
                if (inputDataCheck(mReadEditText!!) && inputDataCheck(mCandidateEditText!!)) {
                    deleteDictionary(mBeforeEditWnnWord)
                    val stroke = mReadEditText!!.text.toString()
                    val candidate = mCandidateEditText!!.text.toString()
                    if (addDictionary(stroke, candidate)) {
                        screenTransition()
                    } else {
                        addDictionary(mBeforeEditWnnWord!!.stroke, mBeforeEditWnnWord!!.candidate)
                    }
                }

            else -> Log.e(
                "OpenWnn",
                "doSaveAction: Invalid Add Status. Status=$mRequestState"
            )
        }
    }

    /**
     * Process the cancel action
     */
    private fun doRevertAction() {
        /* go back to the words list */
        screenTransition()
    }

    /**
     * Create the alert dialog for notifying the error
     *
     * @param  id        The dialog ID
     * @return           The information of the dialog
     */
    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            DIALOG_CONTROL_WORDS_DUPLICATE ->                 /* there is the same word in the dictionary */
                return AlertDialog.Builder(this@UserDictionaryToolsEdit)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.user_dictionary_words_duplication_message)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { dialog, whichButton ->
                        mEntryButton!!.isEnabled = true
                        mCancelButton!!.isEnabled = true
                    }
                    .setCancelable(true)
                    .setOnCancelListener {
                        mEntryButton!!.isEnabled = true
                        mCancelButton!!.isEnabled = true
                    }
                    .create()

            DIALOG_CONTROL_OVER_MAX_TEXT_SIZE ->                 /* the length of the word exceeds the limit */
                return AlertDialog.Builder(this@UserDictionaryToolsEdit)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.user_dictionary_over_max_text_size_message)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { dialog, witchButton ->
                        mEntryButton!!.isEnabled = true
                        mCancelButton!!.isEnabled = true
                    }
                    .setCancelable(true)
                    .create()
        }
        return super.onCreateDialog(id)
    }

    /**
     * Add the word
     *
     * @param  stroke       The stroke of the word
     * @param  candidate    The string of the word
     * @return              `true` if success; `false` if fail.
     */
    private fun addDictionary(stroke: String?, candidate: String?): Boolean {
        val ret: Boolean

        /* create WnnWord from the strings */
        val wnnWordAdd = WnnWord()
        wnnWordAdd.stroke = stroke
        wnnWordAdd.candidate = candidate
        /* add word event */
        val event = OpenWnnEvent(
            OpenWnnEvent.Companion.ADD_WORD,
            WnnEngine.Companion.DICTIONARY_TYPE_USER,
            wnnWordAdd
        )
        /* notify the event to IME */
        ret = sendEventToIME(event)
        if (ret == false) {
            /* get error code if the process in IME is failed */
            val ret_code = event.errorCode
            if (ret_code == RETURN_SAME_WORD) {
                showDialog(DIALOG_CONTROL_WORDS_DUPLICATE)
            }
        } else {
            /* update the dictionary */
            mListInstance = createUserDictionaryToolsList()
        }
        return ret
    }

    /**
     * Delete the word
     *
     * @param  word     The information of word
     */
    private fun deleteDictionary(word: WnnWord?) {
        /* delete the word from the dictionary */
        mListInstance = createUserDictionaryToolsList()
        val deleted = mListInstance!!.deleteWord(word!!)
        if (!deleted) {
            Toast.makeText(
                applicationContext,
                R.string.user_dictionary_delete_fail,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Create the instance of UserDictionaryToolList object
     */
    protected abstract fun createUserDictionaryToolsList(): UserDictionaryToolsList

    /**
     * Check the input string
     *
     * @param   v       The information of view
     * @return          `true` if success; `false` if fail.
     */
    private fun inputDataCheck(v: View): Boolean {
        /* return false if the length of the string exceeds the limit. */

        if (((v as TextView).text.length) > MAX_TEXT_SIZE) {
            showDialog(DIALOG_CONTROL_OVER_MAX_TEXT_SIZE)
            Log.e("OpenWnn", "inputDataCheck() : over max string length.")
            return false
        }

        return true
    }

    /**
     * Transit the new state
     */
    private fun screenTransition() {
        finish()

        /* change to the word listing window */
        val intent = Intent()
        intent.setClassName(mPackageName!!, mListViewName!!)
        startActivity(intent)
    }

    companion object {
        /** The operation mode (Unknown)  */
        private const val STATE_UNKNOWN = 0

        /** The operation mode (Add the word)  */
        private const val STATE_INSERT = 1

        /** The operation mode (Edit the word)  */
        private const val STATE_EDIT = 2

        /** Maximum length of a word's string  */
        private const val MAX_TEXT_SIZE = 20

        /** The error code (Already registered the same word)  */
        private const val RETURN_SAME_WORD = -11

        /** The focus view and pair view  */
        private var sFocusingView: View? = null
        private var sFocusingPairView: View? = null

        /** The constant for notifying dialog (Already exists the specified word)  */
        private const val DIALOG_CONTROL_WORDS_DUPLICATE = 0

        /** The constant for notifying dialog (The length of specified stroke or candidate exceeds the limit)  */
        private const val DIALOG_CONTROL_OVER_MAX_TEXT_SIZE = 1
    }
}
