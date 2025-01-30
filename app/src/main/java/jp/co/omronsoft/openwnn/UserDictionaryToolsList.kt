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
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import ee.oyatl.ime.fusion.R
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.min

/**
 * The abstract class for user dictionary tool.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
abstract class UserDictionaryToolsList : Activity(), View.OnClickListener,
    OnTouchListener, OnFocusChangeListener {
    /** The class name of the user dictionary tool  */
    protected var mListViewName: String? = null

    /** The class name of the user dictionary editor  */
    protected var mEditViewName: String? = null

    /** The package name of the user dictionary editor  */
    protected var mPackageName: String? = null

    /** ID of the menu item (add)  */
    private val MENU_ITEM_ADD = 0

    /** ID of the menu item (edit)  */
    private val MENU_ITEM_EDIT = 1

    /** ID of the menu item (delete)  */
    private val MENU_ITEM_DELETE = 2

    /** ID of the menu item (initialize)  */
    private val MENU_ITEM_INIT = 3

    /** ID of the dialog control (confirm deletion)  */
    private val DIALOG_CONTROL_DELETE_CONFIRM = 0

    /** ID of the dialog control (confirm initialize)  */
    private val DIALOG_CONTROL_INIT_CONFIRM = 1

    /** The size of font */
    private val WORD_TEXT_SIZE = 16

    /** The color of background (unfocused item)  */
    private val UNFOCUS_BACKGROUND_COLOR = -0xdbdbdc

    /** The color of background (focused item)  */
    private val FOCUS_BACKGROUND_COLOR = -0x7b00

    /** The minimum count of registered words  */
    private val MIN_WORD_COUNT = 0

    /** The maximum count of registered words  */
    private val MAX_WORD_COUNT = 100

    /** Maximum word count to display  */
    private val MAX_LIST_WORD_COUNT = 100

    /** The threshold time of the double tapping  */
    private val DOUBLE_TAP_TIME = 300

    /** Widgets which constitute this screen of activity  */
    private var mMenu: Menu? = null

    /** Table layout for the lists  */
    private var mTableLayout: TableLayout? = null

    /** Objects which control state transitions  */
    private var mIntent: Intent? = null

    /** The number of the registered words  */
    private var mWordCount = 0

    /** The state of "Add" menu item  */
    private var mAddMenuEnabled = false

    /** The state of "Edit" menu item  */
    private var mEditMenuEnabled = false

    /** The state of "Delete" menu item  */
    private var mDeleteMenuEnabled = false

    /** The state of "Initialize" menu item  */
    private var mInitMenuEnabled = false

    /** `true` if the menu option is initialized  */
    private var mInitializedMenu = false

    /** `true` if one of word is selected  */
    private var mSelectedWords = false

    /** The viewID which is selected  */
    private var mSelectedViewID = -1

    /** List of the words in the user dictionary  */
    private var mWordList: ArrayList<WnnWord?>? = null

    /** Work area for sorting the word list  */
    private var mSortData: Array<WnnWord?>

    /** Whether the view is initialized  */
    private var mInit = false

    /** Page left button  */
    private var mLeftButton: Button? = null

    /** Page right button  */
    private var mRightButton: Button? = null

    /**
     * Send the specified event to IME
     *
     * @param ev    The event object
     * @return      `true` if this event is processed
     */
    protected abstract fun sendEventToIME(ev: OpenWnnEvent): Boolean

    /** Get the comparator for sorting the list  */
    protected abstract val comparator: Comparator<WnnWord?>

    /** Show Dialog Num  */
    private var mDialogShow = -1

    /** @see android.app.Activity.onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* create XML layout */
        setContentView(R.layout.user_dictionary_tools_list)
        mTableLayout = findViewById<View>(R.id.user_dictionary_tools_table) as TableLayout

        var b = findViewById<View>(R.id.user_dictionary_left_button) as Button
        b.setOnClickListener {
            val pos = mWordCount - MAX_LIST_WORD_COUNT
            if (0 <= pos) {
                mWordCount = pos
                updateWordList()
                mTableLayout!!.findViewById<View>(1).requestFocus()
            }
        }
        mLeftButton = b

        b = findViewById<View>(R.id.user_dictionary_right_button) as Button
        b.setOnClickListener {
            val pos = mWordCount + MAX_LIST_WORD_COUNT
            if (pos < mWordList!!.size) {
                mWordCount = pos
                updateWordList()
                mTableLayout!!.findViewById<View>(1).requestFocus()
            }
        }
        mRightButton = b
    }

    /** @see android.app.Activity.onStart
     */
    override fun onStart() {
        super.onStart()
        mDialogShow = -1
        sBeforeSelectedViewID = -1
        sJustBeforeActionTime = -1
        mWordList = words

        val leftText =
            findViewById<View>(R.id.user_dictionary_tools_list_title_words_count) as TextView
        leftText.text = mWordList!!.size.toString() + "/" + MAX_WORD_COUNT

        isXLarge = ((resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE)
        updateWordList()
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @see android.app.Activity.onPause
     */
    override fun onPause() {
        if (mDialogShow == DIALOG_CONTROL_DELETE_CONFIRM) {
            dismissDialog(DIALOG_CONTROL_DELETE_CONFIRM)
            mDialogShow = -1
        } else if (mDialogShow == DIALOG_CONTROL_INIT_CONFIRM) {
            dismissDialog(DIALOG_CONTROL_INIT_CONFIRM)
            mDialogShow = -1
        }

        super.onPause()
    }

    /**
     * Set parameters of table
     *
     * @param  w        The width of the table
     * @param  h        The height of the table
     * @return          The information of the layout
     */
    private fun tableCreateParam(w: Int, h: Int): TableLayout.LayoutParams {
        return TableLayout.LayoutParams(w, h)
    }

    /** @see android.app.Activity.onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* initialize the menu */


        menu.clear()
        /* set the menu item enable/disable */
        setOptionsMenuEnabled()
        /* [menu] add a word */
        menu.add(0, MENU_ITEM_ADD, 0, R.string.user_dictionary_add)
            .setIcon(android.R.drawable.ic_menu_add)
            .setEnabled(mAddMenuEnabled)
        /* [menu] edit a word */
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.user_dictionary_edit)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setEnabled(mEditMenuEnabled)
        /* [menu] delete a word */
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.user_dictionary_delete)
            .setIcon(android.R.drawable.ic_menu_delete)
            .setEnabled(mDeleteMenuEnabled)
        /* [menu] clear the dictionary */
        menu.add(1, MENU_ITEM_INIT, 0, R.string.user_dictionary_init)
            .setIcon(android.R.drawable.ic_menu_delete)
            .setEnabled(mInitMenuEnabled)

        mMenu = menu
        mInitializedMenu = true


        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Change state of the option menus according to a current state of the list widget
     */
    private fun setOptionsMenuEnabled() {
        /* [menu] add a word */


        mAddMenuEnabled = if (mWordList!!.size >= MAX_WORD_COUNT) {
            /* disable if the number of registered word exceeds MAX_WORD_COUNT */
            false
        } else {
            true
        }


        /* [menu] edit a word/delete a word */
        if (mWordList!!.size <= MIN_WORD_COUNT) {
            /* disable if no word is registered or no word is selected */
            mEditMenuEnabled = false
            mDeleteMenuEnabled = false
        } else {
            mEditMenuEnabled = true
            mDeleteMenuEnabled = if (mSelectedWords) {
                true
            } else {
                false
            }
        }


        /* [menu] clear the dictionary (always enabled) */
        mInitMenuEnabled = true
    }

    /** @see android.app.Activity.onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ret: Boolean
        when (item.itemId) {
            MENU_ITEM_ADD -> {
                /* add a word */
                wordAdd()
                ret = true
            }

            MENU_ITEM_EDIT -> {
                /* edit the word (show dialog) */
                wordEdit(sFocusingView, sFocusingPairView)
                ret = true
            }

            MENU_ITEM_DELETE -> {
                /* delete the word (show dialog) */
                showDialog(DIALOG_CONTROL_DELETE_CONFIRM)
                mDialogShow = DIALOG_CONTROL_DELETE_CONFIRM
                ret = true
            }

            MENU_ITEM_INIT -> {
                /* clear the dictionary (show dialog) */
                showDialog(DIALOG_CONTROL_INIT_CONFIRM)
                mDialogShow = DIALOG_CONTROL_INIT_CONFIRM
                ret = true
            }

            else -> ret = false
        }

        return ret
    }

    /** @see android.app.Activity.onKeyUp
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        /* open the menu if KEYCODE_DPAD_CENTER is pressed */
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            openOptionsMenu()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /** @see android.app.Activity.onCreateDialog
     */
    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            DIALOG_CONTROL_DELETE_CONFIRM -> return AlertDialog.Builder(this@UserDictionaryToolsList)
                .setMessage(R.string.user_dictionary_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, mDialogDeleteWords)
                .setCancelable(true)
                .create()

            DIALOG_CONTROL_INIT_CONFIRM -> return AlertDialog.Builder(this@UserDictionaryToolsList)
                .setMessage(R.string.dialog_clear_user_dictionary_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, mDialogInitWords)
                .setCancelable(true)
                .create()

            else -> Log.e("OpenWnn", "onCreateDialog : Invaled Get DialogID. ID=$id")
        }


        return super.onCreateDialog(id)
    }

    /**
     * Process the event when the button on the "Delete word" dialog is pushed
     *
     * @param  dialog    The information of the dialog
     * @param  button    The button that is pushed
     */
    private val mDialogDeleteWords: DialogInterface.OnClickListener =
        object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, button: Int) {
                mDialogShow = -1
                val focusString = (sFocusingView as TextView).text
                val focusPairString = (sFocusingPairView as TextView).text
                val wnnWordSearch = WnnWord()

                if (mSelectedViewID > MAX_WORD_COUNT) {
                    wnnWordSearch.stroke = focusPairString.toString()
                    wnnWordSearch.candidate = focusString.toString()
                } else {
                    wnnWordSearch.stroke = focusString.toString()
                    wnnWordSearch.candidate = focusPairString.toString()
                }
                val deleted = deleteWord(wnnWordSearch)
                if (deleted) {
                    Toast.makeText(
                        applicationContext,
                        R.string.user_dictionary_delete_complete,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string.user_dictionary_delete_fail,
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                mWordList = this.words
                val size = mWordList!!.size
                if (size <= mWordCount) {
                    val newPos = (mWordCount - MAX_LIST_WORD_COUNT)
                    mWordCount = if ((0 <= newPos)) newPos else 0
                }
                updateWordList()

                val leftText =
                    findViewById<View>(R.id.user_dictionary_tools_list_title_words_count) as TextView
                leftText.text = "$size/$MAX_WORD_COUNT"

                if (mInitializedMenu) {
                    onCreateOptionsMenu(mMenu!!)
                }
            }
        }

    /**
     * Process the event when the button on the "Initialize" dialog is pushed
     *
     * @param  dialog    The information of the dialog
     * @param  button    The button that is pushed
     */
    private val mDialogInitWords =
        DialogInterface.OnClickListener { dialog, button ->
            mDialogShow = -1
            /* clear the user dictionary */
            val ev = OpenWnnEvent(OpenWnnEvent.Companion.INITIALIZE_USER_DICTIONARY, WnnWord())

            sendEventToIME(ev)
            /* show the message */
            Toast.makeText(
                applicationContext, R.string.dialog_clear_user_dictionary_done,
                Toast.LENGTH_LONG
            ).show()
            mWordList = ArrayList()
            mWordCount = 0
            updateWordList()
            val leftText =
                findViewById<View>(R.id.user_dictionary_tools_list_title_words_count) as TextView
            leftText.text = mWordList!!.size.toString() + "/" + MAX_WORD_COUNT
            if (mInitializedMenu) {
                onCreateOptionsMenu(mMenu!!)
            }
        }


    /** @see android.view.View.OnClickListener.onClick
     */
    override fun onClick(arg0: View) {
    }

    /** @see android.view.View.OnTouchListener.onTouch
     */
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        mSelectedViewID = (v as TextView).id
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                /* double tap handling */
                if (sBeforeSelectedViewID != v.id) {
                    /* save the view id if the id is not same as previously selected one */
                    sBeforeSelectedViewID = v.id
                } else {
                    if ((e.downTime - sJustBeforeActionTime) < DOUBLE_TAP_TIME) {
                        /* edit the word if double tapped */
                        sFocusingView = v
                        sFocusingPairView = (v as UserDictionaryToolsListFocus).pairView
                        wordEdit(sFocusingView, sFocusingPairView)
                    }
                }
                /* save the action time */
                sJustBeforeActionTime = e.downTime
            }
        }

        return false
    }

    /** @see android.view.View.OnFocusChangeListener.onFocusChange
     */
    override fun onFocusChange(v: View, hasFocus: Boolean) {
        mSelectedViewID = (v as TextView).id
        sFocusingView = v
        sFocusingPairView = (v as UserDictionaryToolsListFocus).pairView
        if (hasFocus) {
            (v as TextView).setTextColor(Color.BLACK)
            v.setBackgroundColor(FOCUS_BACKGROUND_COLOR)
            (sFocusingPairView as TextView).setTextColor(Color.BLACK)
            sFocusingPairView.setBackgroundColor(FOCUS_BACKGROUND_COLOR)
            mSelectedWords = true
        } else {
            mSelectedWords = false
            (v as TextView).setTextColor(Color.LTGRAY)
            v.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR)
            (sFocusingPairView as TextView).setTextColor(Color.LTGRAY)
            sFocusingPairView.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR)
        }
        if (mInitializedMenu) {
            onCreateOptionsMenu(mMenu!!)
        }
    }

    /**
     * Add the word
     */
    fun wordAdd() {
        /** change to the edit window  */
        screenTransition(Intent.ACTION_INSERT, mEditViewName!!)
    }

    /**
     * Edit the specified word
     *
     * @param  focusView       The information of view
     * @param  focusPairView   The information of pair of view
     */
    fun wordEdit(focusView: View?, focusPairView: View?) {
        if (mSelectedViewID > MAX_WORD_COUNT) {
            createUserDictionaryToolsEdit(focusPairView, focusView)
        } else {
            createUserDictionaryToolsEdit(focusView, focusPairView)
        }
        screenTransition(Intent.ACTION_EDIT, mEditViewName!!)
    }

    /**
     * The internal process of editing the specified word
     *
     * @param  focusView        The information of view
     * @param  focusPairView    The information of pair of view
     */
    protected abstract fun createUserDictionaryToolsEdit(
        focusView: View?,
        focusPairView: View?
    ): UserDictionaryToolsEdit

    /**
     * Delete the specified word
     *
     * @param  searchword   The information of searching
     * @return          `true` if success; `false` if fail.
     */
    fun deleteWord(searchword: WnnWord): Boolean {
        var event = OpenWnnEvent(
            OpenWnnEvent.Companion.LIST_WORDS_IN_USER_DICTIONARY,
            WnnEngine.Companion.DICTIONARY_TYPE_USER,
            searchword
        )

        var deleted = false
        sendEventToIME(event)
        for (i in 0 until MAX_WORD_COUNT) {
            var getword: WnnWord? = WnnWord()
            event = OpenWnnEvent(
                OpenWnnEvent.Companion.GET_WORD,
                getword
            )
            sendEventToIME(event)
            getword = event.word
            val len = getword!!.candidate!!.length
            if (len == 0) {
                break
            }
            if (searchword.candidate == getword.candidate) {
                val delword = WnnWord()
                delword.stroke = searchword.stroke
                delword.candidate = searchword.candidate
                delword.id = i
                event = OpenWnnEvent(
                    OpenWnnEvent.Companion.DELETE_WORD,
                    delword
                )
                deleted = sendEventToIME(event)
                break
            }
        }

        if (mInitializedMenu) {
            onCreateOptionsMenu(mMenu!!)
        }

        return deleted
    }


    /**
     * Processing the transition of screen
     *
     * @param  action       The string of action
     * @param  classname    The class name
     */
    private fun screenTransition(action: String, classname: String) {
        mIntent = if (action == "") {
            Intent()
        } else {
            Intent(action)
        }
        mIntent!!.setClassName(mPackageName!!, classname)
        startActivity(mIntent)
        finish()
    }

    private val words: ArrayList<WnnWord?>
        /**
         * Get the list of words in the user dictionary.
         * @return The list of words
         */
        get() {
            val word = WnnWord()
            var event = OpenWnnEvent(
                OpenWnnEvent.Companion.LIST_WORDS_IN_USER_DICTIONARY,
                WnnEngine.Companion.DICTIONARY_TYPE_USER,
                word
            )
            sendEventToIME(event)

            val list = ArrayList<WnnWord?>()
            for (i in 0 until MAX_WORD_COUNT) {
                event = OpenWnnEvent(OpenWnnEvent.Companion.GET_WORD, word)
                if (!sendEventToIME(event)) {
                    break
                }
                list.add(event.word)
            }

            compareTo(list)

            return list
        }

    /**
     * Sort the list of words
     * @param array The array list of the words
     */
    protected fun compareTo(array: ArrayList<WnnWord?>) {
        mSortData = arrayOfNulls(array.size)
        array.toArray(mSortData)
        Arrays.sort(mSortData, comparator)
    }


    /**
     * Update the word list.
     */
    private fun updateWordList() {
        if (!mInit) {
            mInit = true
            mSelectedViewID = 1

            val window = window
            val windowManager = window.windowManager
            val display = windowManager.defaultDisplay
            val system_width = display.width

            val dummy = UserDictionaryToolsListFocus(this)
            dummy.textSize = WORD_TEXT_SIZE.toFloat()
            val paint = dummy.paint
            val fontMetrics = paint.fontMetricsInt
            val row_hight = ((abs(fontMetrics.top.toDouble()) + fontMetrics.bottom) * 2).toInt()

            for (i in 1..MAX_LIST_WORD_COUNT) {
                val row = TableRow(this)
                val stroke = UserDictionaryToolsListFocus(this)
                stroke.id = i
                stroke.width = system_width / 2
                stroke.textSize = WORD_TEXT_SIZE.toFloat()
                stroke.setTextColor(Color.LTGRAY)
                stroke.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR)
                stroke.setSingleLine()
                stroke.setPadding(1, 0, 1, 1)
                stroke.ellipsize = TextUtils.TruncateAt.END
                stroke.isClickable = true
                stroke.isFocusable = true
                stroke.isFocusableInTouchMode = true
                stroke.setOnTouchListener(this)
                stroke.onFocusChangeListener = this
                if (isXLarge) {
                    stroke.height = row_hight
                    stroke.gravity = Gravity.CENTER_VERTICAL
                }

                val candidate = UserDictionaryToolsListFocus(this)
                candidate.id = i + MAX_WORD_COUNT
                candidate.width = system_width / 2
                candidate.textSize = WORD_TEXT_SIZE.toFloat()
                candidate.setTextColor(Color.LTGRAY)
                candidate.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR)
                candidate.setSingleLine()
                candidate.setPadding(1, 0, 1, 1)
                candidate.ellipsize = TextUtils.TruncateAt.END
                candidate.isClickable = true
                candidate.isFocusable = true
                candidate.isFocusableInTouchMode = true
                candidate.setOnTouchListener(this)
                candidate.onFocusChangeListener = this

                if (isXLarge) {
                    candidate.height = row_hight
                    candidate.gravity = Gravity.CENTER_VERTICAL
                }
                stroke.setPairView(candidate)
                candidate.setPairView(stroke)

                row.addView(stroke)
                row.addView(candidate)
                mTableLayout!!.addView(
                    row,
                    tableCreateParam(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        val size = mWordList!!.size
        val start = mWordCount

        val t = findViewById<View>(R.id.user_dictionary_position_indicator) as TextView
        if (size <= MAX_LIST_WORD_COUNT) {
            (mLeftButton!!.parent as View).visibility = View.GONE
        } else {
            (mLeftButton!!.parent as View).visibility = View.VISIBLE
            val last = (start + MAX_LIST_WORD_COUNT)
            t.text =
                (start + 1).toString() + " - " + min(last.toDouble(), size.toDouble())

            mLeftButton!!.isEnabled = start != 0
            mRightButton!!.isEnabled = last < size
        }

        val selectedId =
            mSelectedViewID - (if ((MAX_WORD_COUNT < mSelectedViewID)) MAX_WORD_COUNT else 0)

        for (i in 0 until MAX_LIST_WORD_COUNT) {
            if ((size - 1) < (start + i)) {
                if ((0 < i) && (selectedId == (i + 1))) {
                    mTableLayout!!.findViewById<View>(i).requestFocus()
                }

                ((mTableLayout!!.findViewById<View>(i + 1)).parent as View).visibility =
                    View.GONE
                continue
            }
            var wnnWordGet = mSortData[start + i]
            val len_stroke = wnnWordGet!!.stroke!!.length
            val len_candidate = wnnWordGet.candidate!!.length
            if (len_stroke == 0 || len_candidate == 0) {
                break
            }

            if (selectedId == i + 1) {
                mTableLayout!!.findViewById<View>(i + 1).requestFocus()
            }

            var text = mTableLayout!!.findViewById<View>(i + 1) as TextView
            text.text = wnnWordGet.stroke
            text = mTableLayout!!.findViewById<View>(i + 1 + MAX_WORD_COUNT) as TextView
            text.text = wnnWordGet.candidate
            (text.parent as View).visibility = View.VISIBLE
        }
        mTableLayout!!.requestLayout()
    }

    companion object {
        /** Focusing view  */
        private var sFocusingView: View? = null

        /** Focusing pair view  */
        private var sFocusingPairView: View? = null

        /** The viewID which was selected previously  */
        private var sBeforeSelectedViewID = -1

        /** The time of previous action  */
        private var sJustBeforeActionTime: Long = -1

        /**
         * Whether the x large mode.
         *
         * @return      `true` if x large; `false` if not x large.
         */
        /** for isXLarge  */
        var isXLarge: Boolean = false
            private set
    }
}
