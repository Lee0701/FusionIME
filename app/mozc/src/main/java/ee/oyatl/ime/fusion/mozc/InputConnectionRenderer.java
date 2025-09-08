package ee.oyatl.ime.fusion.mozc;

import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.UnderlineSpan;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.mozc.android.inputmethod.japanese.KeycodeConverter;
import org.mozc.android.inputmethod.japanese.MozcLog;
import org.mozc.android.inputmethod.japanese.MozcUtil;
import org.mozc.android.inputmethod.japanese.model.SelectionTracker;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands;

import javax.annotation.Nullable;

public class InputConnectionRenderer {

    // Focused segment's attribute.
    static final CharacterStyle SPAN_CONVERT_HIGHLIGHT =
            new BackgroundColorSpan(0x66EF3566);

    // Background color span for non-focused conversion segment.
    // We don't create a static CharacterStyle instance since there are multiple segments at the same
    // time. Otherwise, segments except for the last one cannot have style.
    static final int CONVERT_NORMAL_COLOR = 0x19EF3566;

    // Cursor position.
    // Note that InputConnection seems not to be able to show cursor. This is a workaround.
    static final CharacterStyle SPAN_BEFORE_CURSOR =
            new BackgroundColorSpan(0x664DB6AC);

    // Background color span for partial conversion.
    static final CharacterStyle SPAN_PARTIAL_SUGGESTION_COLOR =
            new BackgroundColorSpan(0x194DB6AC);

    // Underline.
    static final CharacterStyle SPAN_UNDERLINE = new UnderlineSpan();

    private final InputConnection currentInputConnection;
    private final EditorInfo currentInputEditorInfo;

    public InputConnectionRenderer(InputConnection currentInputConnection, EditorInfo currentInputEditorInfo) {
        this.currentInputConnection = currentInputConnection;
        this.currentInputEditorInfo = currentInputEditorInfo;
    }

    // Track the selection.
    SelectionTracker selectionTracker = new SelectionTracker();

    /**
     * Updates InputConnection.
     *
     * @param command Output message. Rendering is based on this parameter.
     * @param keyEvent Trigger event for this calling. When direct input is
     *        needed, this event is sent to InputConnection.
     */
    public void renderInputConnection(ProtoCommands.Command command, @Nullable KeycodeConverter.KeyEventInterface keyEvent) {
        Preconditions.checkNotNull(command);

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }

        ProtoCommands.Output output = command.getOutput();
        if (!output.hasConsumed() || !output.getConsumed()) {
            maybeCommitText(output, inputConnection);
            sendKeyEvent(keyEvent);
            return;
        }

        // Meta key may invoke a command for Mozc server like SWITCH_INPUT_MODE session command. In this
        // case, the command is consumed by Mozc server and the application cannot get the key event.
        // To avoid such situation, we should send the key event back to application. b/13238551
        // The command itself is consumed by Mozc server, so we should NOT put a return statement here.
        if (keyEvent != null && keyEvent.getNativeEvent().isPresent()
                && KeycodeConverter.isMetaKey(keyEvent.getNativeEvent().get())) {
            sendKeyEvent(keyEvent);
        }

        // Here the key is consumed by the Mozc server.
        inputConnection.beginBatchEdit();
        try {
            maybeDeleteSurroundingText(output, inputConnection);
            maybeCommitText(output, inputConnection);
            setComposingText(command, inputConnection);
            maybeSetSelection(output, inputConnection);
            selectionTracker.onRender(
                    output.hasDeletionRange() ? output.getDeletionRange() : null,
                    output.hasResult() ? output.getResult().getValue() : null,
                    output.hasPreedit() ? output.getPreedit() : null);
        } finally {
            inputConnection.endBatchEdit();
        }
    }

    private static void maybeDeleteSurroundingText(ProtoCommands.Output output, InputConnection inputConnection) {
        if (!output.hasDeletionRange()) {
            return;
        }

        ProtoCommands.DeletionRange range = output.getDeletionRange();
        int leftRange = -range.getOffset();
        int rightRange = range.getLength() - leftRange;
        if (leftRange < 0 || rightRange < 0) {
            // If the range does not include the current position, do nothing
            // because Android's API does not expect such situation.
            MozcLog.w("Deletion range has unsupported parameters: " + range);
            return;
        }

        if (!inputConnection.deleteSurroundingText(leftRange, rightRange)) {
            MozcLog.e("Failed to delete surrounding text.");
        }
    }

    private static void maybeCommitText(ProtoCommands.Output output, InputConnection inputConnection) {
        if (!output.hasResult()) {
            return;
        }

        String outputText = output.getResult().getValue();
        if (outputText.isEmpty()) {
            // Do nothing for an empty result string.
            return;
        }

        int position = MozcUtil.CURSOR_POSITION_TAIL;
        if (output.getResult().hasCursorOffset()) {
            if (output.getResult().getCursorOffset()
                    == -outputText.codePointCount(0, outputText.length())) {
                position = MozcUtil.CURSOR_POSITION_HEAD;
            } else {
                MozcLog.e("Unsupported position: " + output.getResult());
            }
        }

        if (!inputConnection.commitText(outputText, position)) {
            MozcLog.e("Failed to commit text.");
        }
    }

    private void setComposingText(ProtoCommands.Command command, InputConnection inputConnection) {
        Preconditions.checkNotNull(command);
        Preconditions.checkNotNull(inputConnection);

        ProtoCommands.Output output = command.getOutput();
        if (!output.hasPreedit()) {
            // If preedit field is empty, we should clear composing text in the InputConnection
            // because Mozc server asks us to do so.
            // But there is special situation in Android.
            // On onWindowShown, SWITCH_INPUT_MODE command is sent as a step of initialization.
            // In this case we reach here with empty preedit.
            // As described above we should clear the composing text but if we do so
            // texts in selection range (e.g., URL in OmniBox) is always cleared.
            // To avoid from this issue, we don't clear the composing text if the input
            // is SWITCH_INPUT_MODE.
            ProtoCommands.Input input = command.getInput();
            if (input.getType() != ProtoCommands.Input.CommandType.SEND_COMMAND
                    || input.getCommand().getType() != ProtoCommands.SessionCommand.CommandType.SWITCH_INPUT_MODE) {
                if (!inputConnection.setComposingText("", 0)) {
                    MozcLog.e("Failed to set composing text.");
                }
            }
            return;
        }

        // Builds preedit expression.
        ProtoCommands.Preedit preedit = output.getPreedit();

        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (ProtoCommands.Preedit.Segment segment : preedit.getSegmentList()) {
            builder.append(segment.getValue());
        }

        // Set underline for all the preedit text.
        builder.setSpan(SPAN_UNDERLINE, 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Draw cursor if in composition mode.
        int cursor = preedit.getCursor();
        int spanFlags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING;
        if (output.hasAllCandidateWords()
                && output.getAllCandidateWords().hasCategory()
                && output.getAllCandidateWords().getCategory() == ProtoCandidateWindow.Category.CONVERSION) {
            int offsetInString = 0;
            for (ProtoCommands.Preedit.Segment segment : preedit.getSegmentList()) {
                int length = segment.getValue().length();
                builder.setSpan(
                        segment.hasAnnotation() && segment.getAnnotation() == ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
                                ? SPAN_CONVERT_HIGHLIGHT
                                : (CharacterStyle) new BackgroundColorSpan(CONVERT_NORMAL_COLOR),
                        offsetInString, offsetInString + length, spanFlags);
                offsetInString += length;
            }
        } else {
            // We cannot show system cursor inside preedit here.
            // Instead we change text style before the preedit's cursor.
            int cursorOffsetInString = builder.toString().offsetByCodePoints(0, cursor);
            if (cursor != builder.length()) {
                builder.setSpan(SPAN_PARTIAL_SUGGESTION_COLOR, cursorOffsetInString, builder.length(),
                        spanFlags);
            }
            if (cursor > 0) {
                builder.setSpan(SPAN_BEFORE_CURSOR, 0, cursorOffsetInString, spanFlags);
            }
        }

        // System cursor will be moved to the tail of preedit.
        // It triggers onUpdateSelection again.
        int cursorPosition = cursor > 0 ? MozcUtil.CURSOR_POSITION_TAIL : 0;
        if (!inputConnection.setComposingText(builder, cursorPosition)) {
            MozcLog.e("Failed to set composing text.");
        }
    }

    private void maybeSetSelection(ProtoCommands.Output output, InputConnection inputConnection) {
        if (!output.hasPreedit()) {
            return;
        }

        ProtoCommands.Preedit preedit = output.getPreedit();
        int cursor = preedit.getCursor();
        if (cursor == 0 || cursor == getPreeditLength(preedit)) {
            // The cursor is at the beginning/ending of the preedit. So we don't anything about the
            // caret setting.
            return;
        }

        int caretPosition = selectionTracker.getPreeditStartPosition();
        if (output.hasDeletionRange()) {
            caretPosition += output.getDeletionRange().getOffset();
        }
        if (output.hasResult()) {
            caretPosition += output.getResult().getValue().length();
        }
        if (output.hasPreedit()) {
            caretPosition += output.getPreedit().getCursor();
        }

        if (!inputConnection.setSelection(caretPosition, caretPosition)) {
            MozcLog.e("Failed to set selection.");
        }
    }

    private static int getPreeditLength(ProtoCommands.Preedit preedit) {
        int result = 0;
        for (int i = 0; i < preedit.getSegmentCount(); ++i) {
            result += preedit.getSegment(i).getValueLength();
        }
        return result;
    }

    private static KeyEvent createKeyEvent(
            KeyEvent original, long eventTime, int action, int repeatCount) {
        return new KeyEvent(
                original.getDownTime(), eventTime, action, original.getKeyCode(),
                repeatCount, original.getMetaState(), original.getDeviceId(), original.getScanCode(),
                original.getFlags());
    }

    /**
     * Sends the {@code KeyEvent}, which is not consumed by the mozc server.
     */
    void sendKeyEvent(KeycodeConverter.KeyEventInterface keyEvent) {
        if (keyEvent == null) {
            return;
        }

        int keyCode = keyEvent.getKeyCode();
        // Some keys have a potential to be consumed from mozc client.
        if (maybeProcessBackKey(keyCode) || maybeProcessActionKey(keyCode)) {
            // The key event is consumed.
            return;
        }

        // Following code is to fallback to target activity.
        Optional<KeyEvent> nativeKeyEvent = keyEvent.getNativeEvent();
        InputConnection inputConnection = getCurrentInputConnection();

        if (nativeKeyEvent.isPresent() && inputConnection != null) {
            // Meta keys are from this.onKeyDown/Up so fallback each time.
            if (KeycodeConverter.isMetaKey(nativeKeyEvent.get())) {
                inputConnection.sendKeyEvent(createKeyEvent(
                        nativeKeyEvent.get(), MozcUtil.getUptimeMillis(),
                        nativeKeyEvent.get().getAction(), nativeKeyEvent.get().getRepeatCount()));
                return;
            }

            // Other keys are from this.onKeyDown so create dummy Down/Up events.
            inputConnection.sendKeyEvent(createKeyEvent(
                    nativeKeyEvent.get(), MozcUtil.getUptimeMillis(), KeyEvent.ACTION_DOWN, 0));

            inputConnection.sendKeyEvent(createKeyEvent(
                    nativeKeyEvent.get(), MozcUtil.getUptimeMillis(), KeyEvent.ACTION_UP, 0));
            return;
        }

        // Otherwise, just delegates the key event to the connected application.
        // However space key needs special treatment because it is expected to produce space character
        // instead of sending ACTION_DOWN/UP pair.
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            inputConnection.commitText(" ", 0);
        } else {
            sendDownUpKeyEvents(keyCode);
        }
    }

    /**
     * @return true if the key event is consumed
     */
    private boolean maybeProcessBackKey(int keyCode) {
        if (keyCode != KeyEvent.KEYCODE_BACK || !isInputViewShown()) {
            return false;
        }

        // Special handling for back key event, to close the software keyboard or its subview.
        // First, try to hide the subview, such as the symbol input view or the cursor view.
        // If neither is shown, hideSubInputView would fail, then hide the whole software keyboard.
//        if (!viewManager.hideSubInputView()) {
//            requestHideSelf(0);
//        }
        return true;
    }

    private boolean maybeProcessActionKey(int keyCode) {
        // Handle the event iff the enter is pressed.
        if (keyCode != KeyEvent.KEYCODE_ENTER || !isInputViewShown()) {
            return false;
        }
        return sendEditorAction(true);
    }

    /**
     * Sends editor action to {@code InputConnection}.
     * <p>
     * The difference from {@link InputMethodService#sendDefaultEditorAction(boolean)} is
     * that if custom action label is specified {@code EditorInfo#actionId} is sent instead.
     */
    private boolean sendEditorAction(boolean fromEnterKey) {
        // If custom action label is specified (=non-null), special action id is also specified.
        // If there is no IME_FLAG_NO_ENTER_ACTION option, we should send the id to the InputConnection.
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo != null
                && (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && editorInfo.actionLabel != null) {
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.performEditorAction(editorInfo.actionId);
                return true;
            }
        }
        // No custom action label is specified. Fall back to default EditorAction.
        return sendDefaultEditorAction(fromEnterKey);
    }

    public void sendDownUpKeyEvents(int keyEventCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long eventTime = SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
        ic.sendKeyEvent(new KeyEvent(eventTime, SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }

    public boolean sendDefaultEditorAction(boolean fromEnterKey) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null &&
                (!fromEnterKey || (ei.imeOptions &
                        EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) &&
                (ei.imeOptions & EditorInfo.IME_MASK_ACTION) !=
                        EditorInfo.IME_ACTION_NONE) {
            // If the enter key was pressed, and the editor has a default
            // action associated with pressing enter, then send it that
            // explicit action instead of the key event.
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.performEditorAction(ei.imeOptions&EditorInfo.IME_MASK_ACTION);
            }
            return true;
        }

        return false;
    }

    boolean isInputViewShown() {
        return true;
    }

    public InputConnection getCurrentInputConnection() {
        return currentInputConnection;
    }

    public EditorInfo getCurrentInputEditorInfo() {
        return currentInputEditorInfo;
    }
}
