package com.android.inputmethod.latin;

import com.android.inputmethod.latin.common.ComposedData;
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion;

import java.text.Normalizer;
import java.util.ArrayList;

/*
 * For Korean dictionary, there are too many cases of characters to store on dictionary, which makes it slow.
 * To solve that, Unicode normalization is used to decompose Hangul syllables into Hangul jamos.
 */
public class NormalizedDictionary extends Dictionary {

    private final Dictionary mDictionary;

    public NormalizedDictionary(Dictionary dictionary) {
        super(dictionary.mDictType, dictionary.mLocale);
        mDictionary = dictionary;
    }

    private String processInput(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD);
    }

    private String processOutput(String output) {
        return Normalizer.normalize(output, Normalizer.Form.NFC);
    }

    @Override
    public ArrayList<SuggestedWords.SuggestedWordInfo> getSuggestions(ComposedData composedData, NgramContext ngramContext, long proximityInfoHandle, SettingsValuesForSuggestion settingsValuesForSuggestion, int sessionId, float weightForLocale, float[] inOutWeightOfLangModelVsSpatialModel) {
        composedData = new ComposedData(composedData.mInputPointers, composedData.mIsBatchMode, processInput(composedData.mTypedWord));
        ArrayList<SuggestedWords.SuggestedWordInfo> suggestions = mDictionary.getSuggestions(composedData, ngramContext, proximityInfoHandle, settingsValuesForSuggestion, sessionId, weightForLocale, inOutWeightOfLangModelVsSpatialModel);
        ArrayList<SuggestedWords.SuggestedWordInfo> result = new ArrayList<>();
        for (SuggestedWords.SuggestedWordInfo info : suggestions) {
            result.add(new SuggestedWords.SuggestedWordInfo(processOutput(info.mWord), info.mPrevWordsContext,
                    info.mScore, info.mKindAndFlags, info.mSourceDict, info.mIndexOfTouchPointOfSecondWord, info.mAutoCommitFirstWordConfidence));
        }
        return result;
    }

    @Override
    public boolean isInDictionary(String word) {
        return mDictionary.isInDictionary(processInput(word));
    }

    @Override
    public int getFrequency(String word) {
        return mDictionary.getFrequency(processInput(word));
    }

    @Override
    public int getMaxFrequencyOfExactMatches(String word) {
        return mDictionary.getMaxFrequencyOfExactMatches(processInput(word));
    }

    @Override
    protected boolean same(char[] word, int length, String typedWord) {
        word = processInput(new String(word)).toCharArray();
        typedWord = processInput(typedWord);
        return mDictionary.same(word, length, typedWord);
    }

    @Override
    public void close() {
        mDictionary.close();
    }

    @Override
    public boolean isInitialized() {
        return mDictionary.isInitialized();
    }

    @Override
    public boolean shouldAutoCommit(SuggestedWords.SuggestedWordInfo candidate) {
        return mDictionary.shouldAutoCommit(candidate);
    }

    @Override
    public boolean isUserSpecific() {
        return mDictionary.isUserSpecific();
    }
}
