/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.latin.suggestions

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.PunctuationSuggestions
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.settings.SettingsValues
import com.android.inputmethod.latin.utils.ResourceUtils
import com.android.inputmethod.latin.utils.ViewLayoutUtils
import kotlin.math.min

internal class SuggestionStripLayoutHelper(
    context: Context, attrs: AttributeSet?,
    defStyle: Int, wordViews: ArrayList<TextView>,
    dividerViews: ArrayList<View>, debugInfoViews: ArrayList<TextView>
) {
    val mPadding: Int
    val mDividerWidth: Int
    val mSuggestionsStripHeight: Int
    private val mSuggestionsCountInStrip: Int
    var mMoreSuggestionsRowHeight: Int
    var maxMoreSuggestionsRow: Int
        private set
    val mMinMoreSuggestionsWidth: Float
    val mMoreSuggestionsBottomGap: Int
    private var mMoreSuggestionsAvailable: Boolean = false

    // The index of these {@link ArrayList} is the position in the suggestion strip. The indices
    // increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
    // The position of the most important suggestion is in {@link #mCenterPositionInStrip}
    private val mWordViews: ArrayList<TextView>
    private val mDividerViews: ArrayList<View>
    private val mDebugInfoViews: ArrayList<TextView>

    private val mColorValidTypedWord: Int
    private val mColorTypedWord: Int
    private val mColorAutoCorrect: Int
    private val mColorSuggested: Int
    private val mAlphaObsoleted: Float
    private val mCenterSuggestionWeight: Float
    private val mCenterPositionInStrip: Int
    private val mTypedWordPositionWhenAutocorrect: Int
    private val mMoreSuggestionsHint: Drawable
    private val mSuggestionStripOptions: Int

    init {
        mWordViews = wordViews
        mDividerViews = dividerViews
        mDebugInfoViews = debugInfoViews

        val wordView: TextView = wordViews.get(0)
        val dividerView: View = dividerViews.get(0)
        mPadding = wordView.getCompoundPaddingLeft() + wordView.getCompoundPaddingRight()
        dividerView.measure(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        mDividerWidth = dividerView.getMeasuredWidth()

        val res: Resources = wordView.getResources()
        mSuggestionsStripHeight = res.getDimensionPixelSize(
            R.dimen.config_suggestions_strip_height
        )

        val a: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SuggestionStripView, defStyle, R.style.SuggestionStripView
        )
        mSuggestionStripOptions = a.getInt(
            R.styleable.SuggestionStripView_suggestionStripOptions, 0
        )
        mAlphaObsoleted = ResourceUtils.getFraction(
            a,
            R.styleable.SuggestionStripView_alphaObsoleted, 1.0f
        )
        mColorValidTypedWord = a.getColor(R.styleable.SuggestionStripView_colorValidTypedWord, 0)
        mColorTypedWord = a.getColor(R.styleable.SuggestionStripView_colorTypedWord, 0)
        mColorAutoCorrect = a.getColor(R.styleable.SuggestionStripView_colorAutoCorrect, 0)
        mColorSuggested = a.getColor(R.styleable.SuggestionStripView_colorSuggested, 0)
        mSuggestionsCountInStrip = a.getInt(
            R.styleable.SuggestionStripView_suggestionsCountInStrip,
            DEFAULT_SUGGESTIONS_COUNT_IN_STRIP
        )
        mCenterSuggestionWeight = ResourceUtils.getFraction(
            a,
            R.styleable.SuggestionStripView_centerSuggestionPercentile,
            DEFAULT_CENTER_SUGGESTION_PERCENTILE
        )
        maxMoreSuggestionsRow = a.getInt(
            R.styleable.SuggestionStripView_maxMoreSuggestionsRow,
            DEFAULT_MAX_MORE_SUGGESTIONS_ROW
        )
        mMinMoreSuggestionsWidth = ResourceUtils.getFraction(
            a,
            R.styleable.SuggestionStripView_minMoreSuggestionsWidth, 1.0f
        )
        a.recycle()

        mMoreSuggestionsHint = getMoreSuggestionsHint(
            res,
            res.getDimension(R.dimen.config_more_suggestions_hint_text_size),
            mColorAutoCorrect
        )
        mCenterPositionInStrip = mSuggestionsCountInStrip / 2
        // Assuming there are at least three suggestions. Also, note that the suggestions are
        // laid out according to script direction, so this is left of the center for LTR scripts
        // and right of the center for RTL scripts.
        mTypedWordPositionWhenAutocorrect = mCenterPositionInStrip - 1
        mMoreSuggestionsBottomGap = res.getDimensionPixelOffset(
            R.dimen.config_more_suggestions_bottom_gap
        )
        mMoreSuggestionsRowHeight = res.getDimensionPixelSize(
            R.dimen.config_more_suggestions_row_height
        )
    }

    private var moreSuggestionsHeight: Int = 0
        get() {
            return maxMoreSuggestionsRow * mMoreSuggestionsRowHeight + mMoreSuggestionsBottomGap
        }
        set(remainingHeight) {
            val currentHeight: Int = field
            if (currentHeight <= remainingHeight) {
                return
            }

            maxMoreSuggestionsRow = ((remainingHeight - mMoreSuggestionsBottomGap)
                    / mMoreSuggestionsRowHeight)
            field = remainingHeight
        }

    private fun getStyledSuggestedWord(
        suggestedWords: SuggestedWords,
        indexInSuggestedWords: Int
    ): CharSequence? {
        if (indexInSuggestedWords >= suggestedWords.size()) {
            return null
        }
        val word: String? = suggestedWords.getLabel(indexInSuggestedWords)
        // TODO: don't use the index to decide whether this is the auto-correction/typed word, as
        // this is brittle
        val isAutoCorrection: Boolean = suggestedWords.mWillAutoCorrect
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION
        val isTypedWordValid: Boolean = suggestedWords.mTypedWordValid
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD
        if (!isAutoCorrection && !isTypedWordValid) {
            return word
        }

        val spannedWord: Spannable = SpannableString(word)
        val options: Int = mSuggestionStripOptions
        if ((isAutoCorrection && (options and AUTO_CORRECT_BOLD) != 0)
            || (isTypedWordValid && (options and VALID_TYPED_WORD_BOLD) != 0)
        ) {
            addStyleSpan(spannedWord, BOLD_SPAN)
        }
        if (isAutoCorrection && (options and AUTO_CORRECT_UNDERLINE) != 0) {
            addStyleSpan(spannedWord, UNDERLINE_SPAN)
        }
        return spannedWord
    }

    /**
     * Convert an index of [SuggestedWords] to position in the suggestion strip.
     * @param indexInSuggestedWords the index of [SuggestedWords].
     * @param suggestedWords the suggested words list
     * @return Non-negative integer of the position in the suggestion strip.
     * Negative integer if the word of the index shouldn't be shown on the suggestion strip.
     */
    private fun getPositionInSuggestionStrip(
        indexInSuggestedWords: Int,
        suggestedWords: SuggestedWords
    ): Int {
        val settingsValues: SettingsValues? = Settings.instance.current
        val shouldOmitTypedWord: Boolean = shouldOmitTypedWord(
            suggestedWords.mInputStyle,
            settingsValues!!.mGestureFloatingPreviewTextEnabled,
            settingsValues.mShouldShowLxxSuggestionUi
        )
        return getPositionInSuggestionStrip(
            indexInSuggestedWords, suggestedWords.mWillAutoCorrect,
            settingsValues.mShouldShowLxxSuggestionUi && shouldOmitTypedWord,
            mCenterPositionInStrip, mTypedWordPositionWhenAutocorrect
        )
    }

    private fun getSuggestionTextColor(
        suggestedWords: SuggestedWords,
        indexInSuggestedWords: Int
    ): Int {
        // Use identity for strings, not #equals : it's the typed word if it's the same object
        val isTypedWord: Boolean = suggestedWords.getInfo(indexInSuggestedWords).isKindOf(
            SuggestedWordInfo.KIND_TYPED
        )

        val color: Int
        if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION
            && suggestedWords.mWillAutoCorrect
        ) {
            color = mColorAutoCorrect
        } else if (isTypedWord && suggestedWords.mTypedWordValid) {
            color = mColorValidTypedWord
        } else if (isTypedWord) {
            color = mColorTypedWord
        } else {
            color = mColorSuggested
        }
        if (suggestedWords.mIsObsoleteSuggestions && !isTypedWord) {
            return applyAlpha(color, mAlphaObsoleted)
        }
        return color
    }

    /**
     * Layout suggestions to the suggestions strip. And returns the start index of more
     * suggestions.
     *
     * @param suggestedWords suggestions to be shown in the suggestions strip.
     * @param stripView the suggestions strip view.
     * @param placerView the view where the debug info will be placed.
     * @return the start index of more suggestions.
     */
    fun layoutAndReturnStartIndexOfMoreSuggestions(
        context: Context,
        suggestedWords: SuggestedWords,
        stripView: ViewGroup,
        placerView: ViewGroup
    ): Int {
        if (suggestedWords.isPunctuationSuggestions) {
            return layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
                suggestedWords as PunctuationSuggestions, stripView
            )
        }

        val wordCountToShow: Int = suggestedWords.getWordCountToShow(
            Settings.instance.current!!.mShouldShowLxxSuggestionUi
        )
        val startIndexOfMoreSuggestions: Int = setupWordViewsAndReturnStartIndexOfMoreSuggestions(
            suggestedWords, mSuggestionsCountInStrip
        )
        val centerWordView: TextView = mWordViews.get(mCenterPositionInStrip)
        val stripWidth: Int = stripView.getWidth()
        val centerWidth: Int = getSuggestionWidth(mCenterPositionInStrip, stripWidth)
        if (wordCountToShow == 1 || getTextScaleX(
                centerWordView.getText(), centerWidth,
                centerWordView.getPaint()
            ) < MIN_TEXT_XSCALE
        ) {
            // Layout only the most relevant suggested word at the center of the suggestion strip
            // by consolidating all slots in the strip.
            val countInStrip: Int = 1
            mMoreSuggestionsAvailable = (wordCountToShow > countInStrip)
            layoutWord(context, mCenterPositionInStrip, stripWidth - mPadding)
            stripView.addView(centerWordView)
            setLayoutWeight(centerWordView, 1.0f, ViewGroup.LayoutParams.MATCH_PARENT)
            if (SuggestionStripView.DBG) {
                layoutDebugInfo(mCenterPositionInStrip, placerView, stripWidth)
            }
            val lastIndex: Int? = centerWordView.getTag() as Int?
            return (if (lastIndex == null) 0 else lastIndex) + 1
        }

        val countInStrip: Int = mSuggestionsCountInStrip
        mMoreSuggestionsAvailable = (wordCountToShow > countInStrip)
        @Suppress("unused") var x: Int = 0
        for (positionInStrip in 0 until countInStrip) {
            if (positionInStrip != 0) {
                val divider: View = mDividerViews.get(positionInStrip)
                // Add divider if this isn't the left most suggestion in suggestions strip.
                addDivider(stripView, divider)
                x += divider.getMeasuredWidth()
            }

            val width: Int = getSuggestionWidth(positionInStrip, stripWidth)
            val wordView: TextView = layoutWord(context, positionInStrip, width)
            stripView.addView(wordView)
            setLayoutWeight(
                wordView, getSuggestionWeight(positionInStrip),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            x += wordView.getMeasuredWidth()

            if (SuggestionStripView.DBG) {
                layoutDebugInfo(positionInStrip, placerView, x)
            }
        }
        return startIndexOfMoreSuggestions
    }

    /**
     * Format appropriately the suggested word in [.mWordViews] specified by
     * `positionInStrip`. When the suggested word doesn't exist, the corresponding
     * [TextView] will be disabled and never respond to user interaction. The suggested word
     * may be shrunk or ellipsized to fit in the specified width.
     *
     * The `positionInStrip` argument is the index in the suggestion strip. The indices
     * increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
     * The position of the most important suggestion is in [.mCenterPositionInStrip]. This
     * usually doesn't match the index in `suggedtedWords` -- see
     * [.getPositionInSuggestionStrip].
     *
     * @param positionInStrip the position in the suggestion strip.
     * @param width the maximum width for layout in pixels.
     * @return the [TextView] containing the suggested word appropriately formatted.
     */
    private fun layoutWord(context: Context, positionInStrip: Int, width: Int): TextView {
        val wordView: TextView = mWordViews.get(positionInStrip)
        val word: CharSequence = wordView.getText()
        if (positionInStrip == mCenterPositionInStrip && mMoreSuggestionsAvailable) {
            // TODO: This "more suggestions hint" should have a nicely designed icon.
            wordView.setCompoundDrawablesWithIntrinsicBounds(
                null, null, null, mMoreSuggestionsHint
            )
            // HACK: Align with other TextViews that have no compound drawables.
            wordView.setCompoundDrawablePadding(-mMoreSuggestionsHint.getIntrinsicHeight())
        } else {
            wordView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
        // {@link StyleSpan} in a content description may cause an issue of TTS/TalkBack.
        // Use a simple {@link String} to avoid the issue.
        wordView.setContentDescription(
            if (TextUtils.isEmpty(word))
                context.getResources().getString(R.string.spoken_empty_suggestion)
            else
                word.toString()
        )
        val text: CharSequence? = getEllipsizedTextWithSettingScaleX(
            word, width, wordView.getPaint()
        )
        val scaleX: Float = wordView.getTextScaleX()
        wordView.setText(text) // TextView.setText() resets text scale x to 1.0.
        wordView.setTextScaleX(scaleX)
        // A <code>wordView</code> should be disabled when <code>word</code> is empty in order to
        // make it unclickable.
        // With accessibility touch exploration on, <code>wordView</code> should be enabled even
        // when it is empty to avoid announcing as "disabled".
        wordView.setEnabled(
            !TextUtils.isEmpty(word)
                    || AccessibilityUtils.instance.isTouchExplorationEnabled()
        )
        return wordView
    }

    private fun layoutDebugInfo(
        positionInStrip: Int, placerView: ViewGroup,
        x: Int
    ) {
        val debugInfoView: TextView = mDebugInfoViews.get(positionInStrip)
        val debugInfo: CharSequence? = debugInfoView.getText()
        if (debugInfo == null) {
            return
        }
        placerView.addView(debugInfoView)
        debugInfoView.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val infoWidth: Int = debugInfoView.getMeasuredWidth()
        val y: Int = debugInfoView.getMeasuredHeight()
        ViewLayoutUtils.placeViewAt(
            debugInfoView, x - infoWidth, y, infoWidth, debugInfoView.getMeasuredHeight()
        )
    }

    private fun getSuggestionWidth(positionInStrip: Int, maxWidth: Int): Int {
        val paddings: Int = mPadding * mSuggestionsCountInStrip
        val dividers: Int = mDividerWidth * (mSuggestionsCountInStrip - 1)
        val availableWidth: Int = maxWidth - paddings - dividers
        return (availableWidth * getSuggestionWeight(positionInStrip)).toInt()
    }

    private fun getSuggestionWeight(positionInStrip: Int): Float {
        if (positionInStrip == mCenterPositionInStrip) {
            return mCenterSuggestionWeight
        }
        // TODO: Revisit this for cases of 5 or more suggestions
        return (1.0f - mCenterSuggestionWeight) / (mSuggestionsCountInStrip - 1)
    }

    private fun setupWordViewsAndReturnStartIndexOfMoreSuggestions(
        suggestedWords: SuggestedWords, maxSuggestionInStrip: Int
    ): Int {
        // Clear all suggestions first
        for (positionInStrip in 0 until maxSuggestionInStrip) {
            val wordView: TextView = mWordViews.get(positionInStrip)
            wordView.setText(null)
            wordView.setTag(null)
            // Make this inactive for touches in {@link #layoutWord(int,int)}.
            if (SuggestionStripView.DBG) {
                mDebugInfoViews.get(positionInStrip).setText(null)
            }
        }
        var count: Int = 0
        var indexInSuggestedWords: Int
        indexInSuggestedWords = 0
        while (indexInSuggestedWords < suggestedWords.size()
            && count < maxSuggestionInStrip
        ) {
            val positionInStrip: Int =
                getPositionInSuggestionStrip(indexInSuggestedWords, suggestedWords)
            if (positionInStrip < 0) {
                indexInSuggestedWords++
                continue
            }
            val wordView: TextView = mWordViews.get(positionInStrip)
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(indexInSuggestedWords)
            wordView.setText(getStyledSuggestedWord(suggestedWords, indexInSuggestedWords))
            wordView.setTextColor(getSuggestionTextColor(suggestedWords, indexInSuggestedWords))
            if (SuggestionStripView.DBG) {
                mDebugInfoViews.get(positionInStrip).setText(
                    suggestedWords.getDebugString(indexInSuggestedWords)
                )
            }
            count++
            indexInSuggestedWords++
        }
        return indexInSuggestedWords
    }

    private fun layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
        punctuationSuggestions: PunctuationSuggestions, stripView: ViewGroup
    ): Int {
        val countInStrip: Int = min(
            punctuationSuggestions.size().toDouble(),
            PUNCTUATIONS_IN_STRIP.toDouble()
        ).toInt()
        for (positionInStrip in 0 until countInStrip) {
            if (positionInStrip != 0) {
                // Add divider if this isn't the left most suggestion in suggestions strip.
                addDivider(stripView, mDividerViews.get(positionInStrip))
            }

            val wordView: TextView = mWordViews.get(positionInStrip)
            val punctuation: String? = punctuationSuggestions.getLabel(positionInStrip)
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(positionInStrip)
            wordView.setText(punctuation)
            wordView.setContentDescription(punctuation)
            wordView.setTextScaleX(1.0f)
            wordView.setCompoundDrawables(null, null, null, null)
            wordView.setTextColor(mColorAutoCorrect)
            stripView.addView(wordView)
            setLayoutWeight(wordView, 1.0f, mSuggestionsStripHeight)
        }
        mMoreSuggestionsAvailable = (punctuationSuggestions.size() > countInStrip)
        return countInStrip
    }

    fun layoutImportantNotice(
        importantNoticeStrip: View,
        importantNoticeTitle: String?
    ) {
        val titleView: TextView = importantNoticeStrip.findViewById<View>(
            R.id.important_notice_title
        ) as TextView
        val width: Int = (titleView.getWidth() - titleView.getPaddingLeft()
                - titleView.getPaddingRight())
        titleView.setTextColor(mColorAutoCorrect)
        titleView.setText(importantNoticeTitle) // TextView.setText() resets text scale x to 1.0.
        val titleScaleX: Float = getTextScaleX(importantNoticeTitle, width, titleView.getPaint())
        titleView.setTextScaleX(titleScaleX)
    }

    companion object {
        private const val DEFAULT_SUGGESTIONS_COUNT_IN_STRIP: Int = 3
        private const val DEFAULT_CENTER_SUGGESTION_PERCENTILE: Float = 0.40f
        private const val DEFAULT_MAX_MORE_SUGGESTIONS_ROW: Int = 2
        private const val PUNCTUATIONS_IN_STRIP: Int = 5
        private const val MIN_TEXT_XSCALE: Float = 0.70f

        private const val MORE_SUGGESTIONS_HINT: String = "\u2026"

        private val BOLD_SPAN: CharacterStyle = StyleSpan(Typeface.BOLD)
        private val UNDERLINE_SPAN: CharacterStyle = UnderlineSpan()

        // These constants are the flag values of
        // {@link R.styleable#SuggestionStripView_suggestionStripOptions} attribute.
        private const val AUTO_CORRECT_BOLD: Int = 0x01
        private const val AUTO_CORRECT_UNDERLINE: Int = 0x02
        private const val VALID_TYPED_WORD_BOLD: Int = 0x04

        private fun getMoreSuggestionsHint(
            res: Resources, textSize: Float,
            color: Int
        ): Drawable {
            val paint: Paint = Paint()
            paint.setAntiAlias(true)
            paint.setTextAlign(Align.CENTER)
            paint.setTextSize(textSize)
            paint.setColor(color)
            val bounds: Rect = Rect()
            paint.getTextBounds(MORE_SUGGESTIONS_HINT, 0, MORE_SUGGESTIONS_HINT.length, bounds)
            val width: Int = Math.round(bounds.width() + 0.5f)
            val height: Int = Math.round(bounds.height() + 0.5f)
            val buffer: Bitmap =
                Bitmap.createBitmap(width, (height * 3 / 2), Bitmap.Config.ARGB_8888)
            val canvas: Canvas = Canvas(buffer)
            canvas.drawText(MORE_SUGGESTIONS_HINT, (width / 2).toFloat(), height.toFloat(), paint)
            val bitmapDrawable: BitmapDrawable = BitmapDrawable(res, buffer)
            bitmapDrawable.setTargetDensity(canvas)
            return bitmapDrawable
        }

        @UsedForTesting
        fun shouldOmitTypedWord(
            inputStyle: Int,
            gestureFloatingPreviewTextEnabled: Boolean,
            shouldShowUiToAcceptTypedWord: Boolean
        ): Boolean {
            val omitTypedWord: Boolean = (inputStyle == SuggestedWords.INPUT_STYLE_TYPING)
                    || (inputStyle == SuggestedWords.INPUT_STYLE_TAIL_BATCH)
                    || (inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
                    && gestureFloatingPreviewTextEnabled)
            return shouldShowUiToAcceptTypedWord && omitTypedWord
        }

        @UsedForTesting
        fun getPositionInSuggestionStrip(
            indexInSuggestedWords: Int,
            willAutoCorrect: Boolean, omitTypedWord: Boolean,
            centerPositionInStrip: Int, typedWordPositionWhenAutoCorrect: Int
        ): Int {
            if (omitTypedWord) {
                if (indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD) {
                    // Ignore.
                    return -1
                }
                if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION) {
                    // Center in the suggestion strip.
                    return centerPositionInStrip
                }
                // If neither of those, the order in the suggestion strip is left of the center first
                // then right of the center, to both edges of the suggestion strip.
                // For example, center-1, center+1, center-2, center+2, and so on.
                val n: Int = indexInSuggestedWords
                val offsetFromCenter: Int = if ((n % 2) == 0) -(n / 2) else (n / 2)
                val positionInSuggestionStrip: Int = centerPositionInStrip + offsetFromCenter
                return positionInSuggestionStrip
            }
            val indexToDisplayMostImportantSuggestion: Int
            val indexToDisplaySecondMostImportantSuggestion: Int
            if (willAutoCorrect) {
                indexToDisplayMostImportantSuggestion =
                    SuggestedWords.INDEX_OF_AUTO_CORRECTION
                indexToDisplaySecondMostImportantSuggestion =
                    SuggestedWords.INDEX_OF_TYPED_WORD
            } else {
                indexToDisplayMostImportantSuggestion = SuggestedWords.INDEX_OF_TYPED_WORD
                indexToDisplaySecondMostImportantSuggestion =
                    SuggestedWords.INDEX_OF_AUTO_CORRECTION
            }
            if (indexInSuggestedWords == indexToDisplayMostImportantSuggestion) {
                // Center in the suggestion strip.
                return centerPositionInStrip
            }
            if (indexInSuggestedWords == indexToDisplaySecondMostImportantSuggestion) {
                // Center-1.
                return typedWordPositionWhenAutoCorrect
            }
            // If neither of those, the order in the suggestion strip is right of the center first
            // then left of the center, to both edges of the suggestion strip.
            // For example, Center+1, center-2, center+2, center-3, and so on.
            val n: Int = indexInSuggestedWords + 1
            val offsetFromCenter: Int = if ((n % 2) == 0) -(n / 2) else (n / 2)
            val positionInSuggestionStrip: Int = centerPositionInStrip + offsetFromCenter
            return positionInSuggestionStrip
        }

        private fun applyAlpha(color: Int, alpha: Float): Int {
            val newAlpha: Int = (Color.alpha(color) * alpha).toInt()
            return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        private fun addDivider(stripView: ViewGroup, dividerView: View) {
            stripView.addView(dividerView)
            val params: LinearLayout.LayoutParams =
                dividerView.getLayoutParams() as LinearLayout.LayoutParams
            params.gravity = Gravity.CENTER
        }

        fun setLayoutWeight(v: View, weight: Float, height: Int) {
            val lp: ViewGroup.LayoutParams = v.getLayoutParams()
            if (lp is LinearLayout.LayoutParams) {
                val llp: LinearLayout.LayoutParams = lp
                llp.weight = weight
                llp.width = 0
                llp.height = height
            }
        }

        private fun getTextScaleX(
            text: CharSequence?, maxWidth: Int,
            paint: TextPaint
        ): Float {
            paint.setTextScaleX(1.0f)
            val width: Int = getTextWidth(text, paint)
            if (width <= maxWidth || maxWidth <= 0) {
                return 1.0f
            }
            return maxWidth / width.toFloat()
        }

        private fun getEllipsizedTextWithSettingScaleX(
            text: CharSequence?, maxWidth: Int, paint: TextPaint
        ): CharSequence? {
            if (text == null) {
                return null
            }
            val scaleX: Float = getTextScaleX(text, maxWidth, paint)
            if (scaleX >= MIN_TEXT_XSCALE) {
                paint.setTextScaleX(scaleX)
                return text
            }

            // <code>text</code> must be ellipsized with minimum text scale x.
            paint.setTextScaleX(MIN_TEXT_XSCALE)
            val hasBoldStyle: Boolean = hasStyleSpan(text, BOLD_SPAN)
            val hasUnderlineStyle: Boolean = hasStyleSpan(text, UNDERLINE_SPAN)
            // TextUtils.ellipsize erases any span object existed after ellipsized point.
            // We have to restore these spans afterward.
            val ellipsizedText: CharSequence = TextUtils.ellipsize(
                text, paint, maxWidth.toFloat(), TextUtils.TruncateAt.MIDDLE
            )
            if (!hasBoldStyle && !hasUnderlineStyle) {
                return ellipsizedText
            }
            val spannableText: Spannable = if ((ellipsizedText is Spannable))
                ellipsizedText
            else
                SpannableString(ellipsizedText)
            if (hasBoldStyle) {
                addStyleSpan(spannableText, BOLD_SPAN)
            }
            if (hasUnderlineStyle) {
                addStyleSpan(spannableText, UNDERLINE_SPAN)
            }
            return spannableText
        }

        private fun hasStyleSpan(
            text: CharSequence?,
            style: CharacterStyle
        ): Boolean {
            if (text is Spanned) {
                return text.getSpanStart(style) >= 0
            }
            return false
        }

        private fun addStyleSpan(text: Spannable, style: CharacterStyle) {
            text.removeSpan(style)
            text.setSpan(style, 0, text.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        private fun getTextWidth(text: CharSequence?, paint: TextPaint): Int {
            if (TextUtils.isEmpty(text)) {
                return 0
            }
            val length: Int = text!!.length
            val widths: FloatArray = FloatArray(length)
            val count: Int
            val savedTypeface: Typeface = paint.getTypeface()
            try {
                paint.setTypeface(getTextTypeface(text))
                count = paint.getTextWidths(text, 0, length, widths)
            } finally {
                paint.setTypeface(savedTypeface)
            }
            var width: Int = 0
            for (i in 0 until count) {
                width += Math.round(widths.get(i) + 0.5f)
            }
            return width
        }

        private fun getTextTypeface(text: CharSequence?): Typeface {
            return if (hasStyleSpan(text, BOLD_SPAN)) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }
}
