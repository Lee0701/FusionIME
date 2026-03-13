package com.android.inputmethod.latin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

import com.android.inputmethod.event.Event;
import com.android.inputmethod.keyboard.KeyboardActionListener;
import com.android.inputmethod.keyboard.KeyboardSwitcher;
import com.android.inputmethod.latin.common.InputPointers;
import com.android.inputmethod.latin.inputlogic.InputLogic;
import com.android.inputmethod.latin.permissions.PermissionsManager;
import com.android.inputmethod.latin.settings.Settings;
import com.android.inputmethod.latin.suggestions.SuggestionStripView;
import com.android.inputmethod.latin.suggestions.SuggestionStripViewAccessor;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public interface ILatinIME extends KeyboardActionListener,
        SuggestionStripView.Listener, SuggestionStripViewAccessor,
        DictionaryFacilitator.DictionaryInitializationListener,
        PermissionsManager.PermissionsResultCallback {
    String TAG = LatinIME.class.getSimpleName();
    boolean TRACE = false;

    int PERIOD_FOR_AUDIO_AND_HAPTIC_FEEDBACK_IN_KEY_REPEAT = 2;
    int PENDING_IMS_CALLBACK_DURATION_MILLIS = 800;
    long DELAY_WAIT_FOR_DICTIONARY_LOAD_MILLIS = TimeUnit.SECONDS.toMillis(2);
    long DELAY_DEALLOCATE_MEMORY_MILLIS = TimeUnit.SECONDS.toMillis(10);

    /**
     * A broadcast intent action to hide the software keyboard.
     */
    String ACTION_HIDE_SOFT_INPUT =
            "com.android.inputmethod.latin.HIDE_SOFT_INPUT";

    /**
     * A custom permission for external apps to send {@link #ACTION_HIDE_SOFT_INPUT}.
     */
    String PERMISSION_HIDE_SOFT_INPUT =
            "com.android.inputmethod.latin.HIDE_SOFT_INPUT";

    /**
     * The name of the scheme used by the Package Manager to warn of a new package installation,
     * replacement or removal.
     */
    String SCHEME_PACKAGE = "package";

    final class HideSoftInputReceiver extends BroadcastReceiver {
        private final InputMethodService mIms;

        public HideSoftInputReceiver(InputMethodService ims) {
            mIms = ims;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_HIDE_SOFT_INPUT.equals(action)) {
                mIms.requestHideSelf(0 /* flags */);
            } else {
                Log.e(TAG, "Unexpected intent " + intent);
            }
        }
    }

    Context getContext();

    LatinIME.UIHandler getHandler();

    InputLogic getInputLogic();

    Settings getSettings();

    KeyboardSwitcher getKeyboardSwitcher();

    boolean isInputViewShown();

    EditorInfo getCurrentInputEditorInfo();

    Resources getResources();

    InputConnection getCurrentInputConnection();

    void onCreate();

    // Note that this method is called from a non-UI thread.
    @Override
    void onUpdateMainDictionaryAvailability(final boolean isMainDictionaryAvailable);

    void resetDictionaryFacilitatorIfNecessary();

    /**
     * Reset suggest by loading the main dictionary of the current locale.
     */
    /* package private */ void resetSuggestMainDict();

    void onDestroy();

    void onConfigurationChanged(final Configuration conf);

    void onInitializeInterface();

    View onCreateInputView();

    void setInputView(final View view);

    void setCandidatesView(final View view);

    void onStartInput(final EditorInfo editorInfo, final boolean restarting);

    void onStartInputView(final EditorInfo editorInfo, final boolean restarting);

    void onFinishInputView(final boolean finishingInput);

    void onFinishInput();

    void onCurrentInputMethodSubtypeChanged(final InputMethodSubtype subtype);

    void onWindowShown();

    void onWindowHidden();

    void onFinishInputInternal();

    void onFinishInputViewInternal(final boolean finishingInput);

    void onUpdateSelection(final int oldSelStart, final int oldSelEnd,
                                  final int newSelStart, final int newSelEnd,
                                  final int composingSpanStart, final int composingSpanEnd);

    /**
     * This is called when the user has clicked on the extracted text view,
     * when running in fullscreen mode.  The default implementation hides
     * the suggestions view when this happens, but only if the extracted text
     * editor has a vertical scroll bar because its text doesn't fit.
     * Here we override the behavior due to the possibility that a re-correction could
     * cause the suggestions strip to disappear and re-appear.
     */
    void onExtractedTextClicked();

    /**
     * This is called when the user has performed a cursor movement in the
     * extracted text view, when it is running in fullscreen mode.  The default
     * implementation hides the suggestions view when a vertical movement
     * happens, but only if the extracted text editor has a vertical scroll bar
     * because its text doesn't fit.
     * Here we override the behavior due to the possibility that a re-correction could
     * cause the suggestions strip to disappear and re-appear.
     */
    void onExtractedCursorMovement(final int dx, final int dy);

    void hideWindow();

    void onDisplayCompletions(final CompletionInfo[] applicationSpecifiedCompletions);

    void onComputeInsets(final InputMethodService.Insets outInsets);

    void startShowingInputView(final boolean needsToLoadKeyboard);

    void stopShowingInputView();

    boolean onShowInputRequested(final int flags, final boolean configChange);

    boolean onEvaluateInputViewShown();

    boolean onEvaluateFullscreenMode();

    void updateFullscreenMode();

    int getCurrentAutoCapsState();

    int getCurrentRecapitalizeState();

    /**
     * @param codePoints code points to get coordinates for.
     * @return x,y coordinates for this keyboard, as a flattened array.
     */
    int[] getCoordinatesForCurrentKeyboard(final int[] codePoints);

    // Callback for the {@link SuggestionStripView}, to call when the important notice strip is
    // pressed.
    @Override
    void showImportantNoticeContents();

    @Override
    void onRequestPermissionsResult(boolean allGranted);

    void displaySettingsDialog();

    @Override
    boolean onCustomRequest(final int requestCode);

    void switchLanguage(final InputMethodSubtype subtype);

    // TODO: Revise the language switch key behavior to make it much smarter and more reasonable.
    void switchToNextSubtype();

    // Implementation of {@link KeyboardActionListener}.
    @Override
    void onCodeInput(final int codePoint, final int x, final int y,
                            final boolean isKeyRepeat);

    // This method is for testability of LatinIME, but also in the future it should
    // completely replace #onCodeInput.
    void onEvent(@Nonnull final Event event);

    // Called from PointerTracker through the KeyboardActionListener interface
    @Override
    void onTextInput(final String rawText);

    @Override
    void onStartBatchInput() ;

    @Override
    void onUpdateBatchInput(final InputPointers batchPointers);

    @Override
    void onEndBatchInput(final InputPointers batchPointers);

    @Override
    void onCancelBatchInput();

    /**
     * To be called after the InputLogic has gotten a chance to act on the suggested words by the
     * IME for the full gesture, possibly updating the TextView to reflect the first suggestion.
     * <p>
     * This method must be run on the UI Thread.
     * @param suggestedWords suggested words by the IME for the full gesture.
     */
    void onTailBatchInputResultShown(final SuggestedWords suggestedWords);

    // This method must run on the UI Thread.
    void showGesturePreviewAndSuggestionStrip(@Nonnull final SuggestedWords suggestedWords,
                                              final boolean dismissGestureFloatingPreviewText);

    // Called from PointerTracker through the KeyboardActionListener interface
    @Override
    void onFinishSlidingInput();

    // Called from PointerTracker through the KeyboardActionListener interface
    @Override
    void onCancelInput();

    boolean hasSuggestionStripView();

    // TODO[IL]: Move this out of LatinIME.
    void getSuggestedWords(final int inputStyle, final int sequenceNumber,
                                  final Suggest.OnGetSuggestedWordsCallback callback);

    @Override
    void showSuggestionStrip(final SuggestedWords suggestedWords);

    // Called from {@link SuggestionStripView} through the {@link SuggestionStripView#Listener}
    // interface
    @Override
    void pickSuggestionManually(final SuggestedWords.SuggestedWordInfo suggestionInfo);

    // This will show either an empty suggestion strip (if prediction is enabled) or
    // punctuation suggestions (if it's disabled).
    @Override
    void setNeutralSuggestionStrip();

    // Callback of the {@link KeyboardActionListener}. This is called when a key is depressed;
    // release matching call is {@link #onReleaseKey(int,boolean)} below.
    @Override
    void onPressKey(final int primaryCode, final int repeatCount,
                           final boolean isSinglePointer);

    // Callback of the {@link KeyboardActionListener}. This is called when a key is released;
    // press matching call is {@link #onPressKey(int,int,boolean)} above.
    @Override
    void onReleaseKey(final int primaryCode, final boolean withSliding);

    // Hooks for hardware keyboard
    boolean onKeyDown(final int keyCode, final KeyEvent keyEvent);

    boolean onKeyUp(final int keyCode, final KeyEvent keyEvent);

    // onKeyDown and onKeyUp are the main events we are interested in. There are two more events
    // related to handling of hardware key events that we may want to implement in the future:
    // boolean onKeyLongPress(final int keyCode, final KeyEvent event);
    // boolean onKeyMultiple(final int keyCode, final int count, final KeyEvent event);

    void launchSettings(final String extraEntryValue);

    void dumpDictionaryForDebug(final String dictName);

    void debugDumpStateAndCrashWithException(final String context);

    boolean shouldSwitchToOtherInputMethods();

    boolean shouldShowLanguageSwitchKey();

    boolean enableHardwareAcceleration();

}
