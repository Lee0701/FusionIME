package ee.oyatl.ime.fusion

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class KeyEventUtil(
    val currentInputConnection: InputConnection,
    val currentInputEditorInfo: EditorInfo
) {

    /**
     * Send the given key event code (as defined by [KeyEvent]) to the
     * current input connection is a key down + key up event pair.  The sent
     * events have [KeyEvent.FLAG_SOFT_KEYBOARD]
     * set, so that the recipient can identify them as coming from a software
     * input method, and
     * [KeyEvent.FLAG_KEEP_TOUCH_MODE], so
     * that they don't impact the current touch mode of the UI.
     *
     *
     * Note that it's discouraged to send such key events in normal operation;
     * this is mainly for use with [android.text.InputType.TYPE_NULL] type
     * text fields, or for non-rich input methods. A reasonably capable software
     * input method should use the
     * [android.view.inputmethod.InputConnection.commitText] family of methods
     * to send text to an application, rather than sending key events.
     *
     * @param keyEventCode The raw key code to send, as defined by
     * [KeyEvent].
     */
    fun sendDownUpKeyEvents(keyEventCode: Int) {
        val ic = currentInputConnection
        val eventTime = SystemClock.uptimeMillis()
        ic.sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        ic.sendKeyEvent(
            KeyEvent(
                eventTime, SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    /**
     * Ask the input target to execute its default action via
     * [ InputConnection.performEditorAction()][InputConnection.performEditorAction].
     *
     *
     * For compatibility, this method does not execute a custom action even if [ ][EditorInfo.actionLabel] is set. The implementor should directly call
     * [InputConnection.performEditorAction()][InputConnection.performEditorAction] with
     * [EditorInfo.actionId] if they want to execute a custom action.
     *
     * @param fromEnterKey If true, this will be executed as if the user had
     * pressed an enter key on the keyboard, that is it will *not*
     * be done if the editor has set [ EditorInfo.IME_FLAG_NO_ENTER_ACTION][EditorInfo.IME_FLAG_NO_ENTER_ACTION].  If false, the action will be
     * sent regardless of how the editor has set that flag.
     *
     * @return Returns a boolean indicating whether an action has been sent.
     * If false, either the editor did not specify a default action or it
     * does not want an action from the enter key.  If true, the action was
     * sent (or there was no input connection at all).
     */
    fun sendDefaultEditorAction(fromEnterKey: Boolean): Boolean {
        val ei: EditorInfo = currentInputEditorInfo
        if ((!fromEnterKey || (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0)
            && (ei.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE
        ) {
            // If the enter key was pressed, and the editor has a default
            // action associated with pressing enter, then send it that
            // explicit action instead of the key event.
            val ic = currentInputConnection
            ic.performEditorAction(ei.imeOptions and EditorInfo.IME_MASK_ACTION)
            return true
        }

        return false
    }

    /**
     * Send the given UTF-16 character to the current input connection.  Most
     * characters will be delivered simply by calling
     * [InputConnection.commitText()][InputConnection.commitText] with
     * the character; some, however, may be handled different.  In particular,
     * the enter character ('\n') will either be delivered as an action code
     * or a raw key event, as appropriate.  Consider this as a convenience
     * method for IMEs that do not have a full implementation of actions; a
     * fully complying IME will decide of the right action for each event and
     * will likely never call this method except maybe to handle events coming
     * from an actual hardware keyboard.
     *
     * @param charCode The UTF-16 character code to send.
     */
    fun sendKeyChar(charCode: Char) {
        when (charCode) {
            '\n' -> if (!sendDefaultEditorAction(true)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }

            else ->                 // Make sure that digits go through any text watcher on the client side.
                if (charCode in '0'..'9') {
                    sendDownUpKeyEvents(charCode.code - '0'.code + KeyEvent.KEYCODE_0)
                } else {
                    val ic = currentInputConnection
                    ic.commitText(charCode.toString(), 1)
                }
        }
    }

}