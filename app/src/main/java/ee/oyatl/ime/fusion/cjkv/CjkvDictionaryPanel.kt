package ee.oyatl.ime.fusion.cjkv

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.edit
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.CjkvDictionaryPanelBinding
import ee.oyatl.ime.fusion.layout.LayoutCangjie
import ee.oyatl.ime.fusion.mode.IMEModeSwitcher
import ee.oyatl.ime.theme.CandidateItemType
import ee.oyatl.ime.theme.KeyboardTheme
import java.util.Locale

internal object CjkvCangjieFormatter {
    private val rootByCode = LayoutCangjie.KEY_MAP_CANGJIE.entries.associate { (root, code) ->
        code.uppercaseChar() to root
    }

    fun format(value: String): String = value.map { character ->
        rootByCode[character.uppercaseChar()] ?: character
    }.joinToString("")
}

class CjkvDictionaryPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
): FrameLayout(context, attrs) {
    private val binding = CjkvDictionaryPanelBinding.inflate(LayoutInflater.from(context), this, true)
    // Kept out of the default preferences: FusionIMEService rebuilds every input
    // mode and returns to the first tab whenever those change.
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Called with the text of a tapped character or reading, to be typed into the editor. */
    var onTextSelected: ((String) -> Unit)? = null

    /** Looks up a long-pressed character; null when it cannot be looked up. */
    var onLookupRequested: ((String) -> CjkvDictionaryViewState?)? = null

    var onCloseRequested: (() -> Unit)? = null

    /** The character whose entry is shown, if any. */
    val currentCharacter: String?
        get() = (currentState as? CjkvDictionaryViewState.Found)?.character?.text

    /** Whether the panel grows to fit its entry; the host sizes it. */
    var expanded: Boolean
        get() = preferences.getBoolean(PREF_EXPANDED, false)
        private set(value) = preferences.edit { putBoolean(PREF_EXPANDED, value) }

    /** Called when the toggle is flipped or what is shown changes, so the host can resize. */
    var onLayoutNeedsFit: (() -> Unit)? = null

    /** The height the panel needs to show everything without scrolling, at its current width. */
    fun fittingHeight(): Int {
        val content = binding.dictionaryScroll.getChildAt(0) ?: return 0
        val header = binding.dictionaryHeader.height
        val width = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        content.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        return header + content.measuredHeight
    }

    // The Han characters of a word being looked up (製造), and which one is shown.
    private var wordCharacters: List<String> = emptyList()
    private var wordIndex = 0

    // Characters reached by long-pressing, so the back button can return to them.
    private val history = ArrayDeque<CjkvDictionaryViewState>()
    private var currentState: CjkvDictionaryViewState? = null
    private var currentPresentation = CjkvDictionaryPresentation.Default

    init {
        binding.dictionaryCharacter.setOnClickListener {
            binding.dictionaryCharacter.text?.toString()?.takeIf(String::isNotEmpty)?.let { onTextSelected?.invoke(it) }
        }
        tappableViews().forEach { it.movementMethod = LinkMovementMethod.getInstance() }
        binding.dictionaryClose.setOnClickListener { onCloseRequested?.invoke() }
        // Turned like the candidate list's expand button: 180° when expanded.
        binding.dictionaryExpand.rotation = if(expanded) 180f else 0f
        binding.dictionaryExpand.setOnClickListener {
            expanded = !expanded
            ObjectAnimator.ofFloat(binding.dictionaryExpand, "rotation", if(expanded) 180f else 0f).apply {
                duration = IMEModeSwitcher.EXPAND_COLLAPSE_DURATION
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
            onLayoutNeedsFit?.invoke()
        }
        binding.dictionaryWord.movementMethod = LinkMovementMethod.getInstance()
        binding.dictionaryBack.setOnClickListener {
            val previous = history.removeLastOrNull() ?: return@setOnClickListener
            show(previous, currentPresentation)
        }
    }

    private fun lookUp(text: String) {
        val state = onLookupRequested?.invoke(text) ?: return
        currentState?.let {
            history.addLast(it)
            if(history.size > MAX_HISTORY) history.removeFirst()
        }
        show(state, currentPresentation)
        binding.dictionaryScroll.scrollTo(0, 0)
    }

    private fun tappableViews(): List<TextView> = listOf(
        binding.dictionaryJapaneseOn,
        binding.dictionaryJapaneseKun,
        binding.dictionaryMandarin,
        binding.dictionaryCantonese,
        binding.dictionaryKorean,
        binding.dictionaryVietnamese,
        binding.dictionaryCangjie,
        binding.dictionaryMiddleChinese,
        binding.dictionaryFanqie,
        binding.dictionaryRadicalStrokes,
        binding.dictionarySimplified,
        binding.dictionaryTraditional,
        binding.dictionaryJapaneseNew,
        binding.dictionaryJapaneseOld,
        binding.dictionaryCompatibility,
        binding.dictionarySemanticVariants,
        binding.dictionarySpecializedSemanticVariants,
        binding.dictionaryZVariants,
    )

    /**
     * A span that types [text] when tapped, drawn like the surrounding text.
     * A single Han character can also be long-pressed to look it up.
     */
    private inner class TypeSpan(private val text: String): ClickableSpan(), CjkvTappableTextView.LongPressSpan {
        override val canLongPress: Boolean = isSingleHan(text)

        override fun onClick(widget: View) {
            onTextSelected?.invoke(text)
        }

        override fun onLongPress(widget: View) {
            lookUp(text)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }

    private fun isSingleHan(text: String): Boolean {
        if(text.isEmpty() || text.codePointCount(0, text.length) != 1) return false
        return Character.UnicodeScript.of(text.codePointAt(0)) == Character.UnicodeScript.HAN
    }

    /** Joins [items] with " · ", each one tappable to type [typed] of it. */
    private fun tappableList(items: List<String>, typed: (String) -> String = { it }): SpannableStringBuilder {
        return SpannableStringBuilder().apply {
            items.forEachIndexed { index, item ->
                if(index > 0) append(" · ")
                val start = length
                append(item)
                setSpan(TypeSpan(typed(item)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun applyTheme(theme: KeyboardTheme) {
        setBackgroundColor(context.getColor(theme.backgroundColor))
        val itemTheme = theme.candidatesTheme.itemThemes.getValue(CandidateItemType.Default)
        val foreground = context.getColor(itemTheme.foregroundColor)
        val labels = listOf(
            binding.dictionaryCodePoint,
            binding.dictionaryDefinitionLabel,
            binding.dictionaryJapaneseOnLabel,
            binding.dictionaryJapaneseKunLabel,
            binding.dictionaryMandarinLabel,
            binding.dictionaryCantoneseLabel,
            binding.dictionaryKoreanLabel,
            binding.dictionaryVietnameseLabel,
            binding.dictionaryCangjieLabel,
            binding.dictionaryFourCornerLabel,
            binding.dictionaryMiddleChineseLabel,
            binding.dictionaryFanqieLabel,
            binding.dictionaryRadicalStrokesLabel,
            binding.dictionaryTotalStrokesLabel,
            binding.dictionarySimplifiedLabel,
            binding.dictionaryTraditionalLabel,
            binding.dictionaryJapaneseNewLabel,
            binding.dictionaryJapaneseOldLabel,
            binding.dictionaryCompatibilityLabel,
            binding.dictionarySemanticVariantsLabel,
            binding.dictionarySpecializedSemanticVariantsLabel,
            binding.dictionaryZVariantsLabel,
        )
        val values = listOf(
            binding.dictionaryCharacter,
            binding.dictionaryStatus,
            binding.dictionaryNote,
            binding.dictionaryWord,
            binding.dictionaryDefinition,
            binding.dictionaryJapaneseOn,
            binding.dictionaryJapaneseKun,
            binding.dictionaryMandarin,
            binding.dictionaryCantonese,
            binding.dictionaryKorean,
            binding.dictionaryVietnamese,
            binding.dictionaryCangjie,
            binding.dictionaryFourCorner,
            binding.dictionaryMiddleChinese,
            binding.dictionaryFanqie,
            binding.dictionaryRadicalStrokes,
            binding.dictionaryTotalStrokes,
            binding.dictionarySimplified,
            binding.dictionaryTraditional,
            binding.dictionaryJapaneseNew,
            binding.dictionaryJapaneseOld,
            binding.dictionaryCompatibility,
            binding.dictionarySemanticVariants,
            binding.dictionarySpecializedSemanticVariants,
            binding.dictionaryZVariants,
        )
        labels.forEach {
            it.setTextColor(foreground)
            it.alpha = 0.72f
        }
        values.forEach { it.setTextColor(foreground) }
        // Tinted like the candidate list's expand button.
        val tint = ColorStateList.valueOf(foreground)
        binding.dictionaryBack.imageTintList = tint
        binding.dictionaryClose.imageTintList = tint
        binding.dictionaryExpand.imageTintList = tint
    }

    /** Shows [state] as a fresh lookup, forgetting characters reached by long-pressing. */
    fun render(
        state: CjkvDictionaryViewState,
        presentation: CjkvDictionaryPresentation = CjkvDictionaryPresentation.Default,
    ) {
        wordCharacters = emptyList()
        updateWord()
        history.clear()
        show(state, presentation)
    }

    /** Shows a word's characters in a row above the entry, starting with the first. */
    fun renderWord(word: String, presentation: CjkvDictionaryPresentation) {
        currentPresentation = presentation
        wordCharacters = word.codePoints().toArray()
            .filter { Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN }
            .map { String(Character.toChars(it)) }
        showWordCharacter(0)
        binding.dictionaryScroll.scrollTo(0, 0)
    }

    private fun showWordCharacter(index: Int) {
        val character = wordCharacters.getOrNull(index) ?: return
        val state = onLookupRequested?.invoke(character) ?: return
        wordIndex = index
        updateWord()
        history.clear()
        show(state, currentPresentation)
    }

    private fun updateWord() {
        // A single character needs no row of its own.
        if(wordCharacters.size < 2) {
            binding.dictionaryWord.visibility = View.GONE
            return
        }
        binding.dictionaryWord.visibility = View.VISIBLE
        binding.dictionaryWord.textLocale = Locale.forLanguageTag(
            currentPresentation.glyphOrder.firstOrNull()?.languageTag ?: "ja-JP"
        )
        binding.dictionaryWord.text = SpannableStringBuilder().apply {
            wordCharacters.forEachIndexed { index, character ->
                if(index > 0) append("  ")
                val start = length
                append(character)
                if(index == wordIndex) {
                    setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    setSpan(object: ClickableSpan() {
                        override fun onClick(widget: View) = showWordCharacter(index)
                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = false
                        }
                    }, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun show(state: CjkvDictionaryViewState, presentation: CjkvDictionaryPresentation) {
        post { onLayoutNeedsFit?.invoke() }
        currentState = state
        currentPresentation = presentation
        binding.dictionaryBack.visibility = if(history.isEmpty()) View.GONE else View.VISIBLE
        binding.dictionaryStatus.visibility = View.GONE
        binding.dictionaryContent.visibility = View.GONE
        when(state) {
            CjkvDictionaryViewState.Empty -> showStatus(R.string.cjkv_dictionary_empty)
            CjkvDictionaryViewState.Unavailable -> showStatus(R.string.cjkv_dictionary_unavailable)
            CjkvDictionaryViewState.SensitiveInput -> showStatus(R.string.cjkv_dictionary_sensitive)
            CjkvDictionaryViewState.DataError -> showStatus(R.string.cjkv_dictionary_data_error)
            is CjkvDictionaryViewState.NotHan -> showStatus(
                context.getString(
                    R.string.cjkv_dictionary_not_han,
                    state.text,
                    formatCodePoint(state.codePoint),
                )
            )
            is CjkvDictionaryViewState.Found -> showEntry(state, presentation)
        }
    }

    private fun showEntry(
        state: CjkvDictionaryViewState.Found,
        presentation: CjkvDictionaryPresentation,
    ) {
        binding.dictionaryContent.visibility = View.VISIBLE
        reorderReadings(presentation.languageOrder)
        binding.dictionaryCharacter.textLocale = Locale.forLanguageTag(
            presentation.glyphOrder.firstOrNull()?.languageTag ?: "ja-JP"
        )
        binding.dictionaryCharacter.text = state.character.text
        binding.dictionaryCodePoint.text = state.character.codePointLabel
        val resolved = state.entry
        val entry = resolved?.entry
        binding.dictionaryNote.visibility = if(resolved?.hasAnyReading == true) View.GONE else View.VISIBLE
        binding.dictionaryJapaneseOn.setReading(resolved?.reading(CjkvReadingField.JapaneseOn), "ja-JP")
        binding.dictionaryJapaneseKun.setReading(resolved?.reading(CjkvReadingField.JapaneseKun), "ja-JP")
        binding.dictionaryDefinition.setListValue(entry?.definition?.takeIf(String::isNotBlank)?.let { listOf(it) })
        binding.dictionaryMandarin.setMandarin(resolved?.reading(CjkvReadingField.Mandarin))
        binding.dictionaryCantonese.setReading(resolved?.reading(CjkvReadingField.Cantonese), "zh-HK")
        binding.dictionaryKorean.setKorean(resolved?.reading(CjkvReadingField.Korean), entry?.koreanHunEum)
        binding.dictionaryVietnamese.setReading(resolved?.reading(CjkvReadingField.Vietnamese), "vi-VN")
        binding.dictionaryCangjie.setListValue(entry?.cangjie?.let(CjkvCangjieFormatter::format), tappable = true)
        binding.dictionaryFourCorner.setListValue(entry?.fourCorner)
        binding.dictionaryMiddleChinese.setListValue(entry?.middleChinese, tappable = true)
        binding.dictionaryFanqie.setListValue(entry?.fanqie, "zh-TW", tappable = true)
        binding.dictionaryRadicalStrokes.text = describeRadicalStrokes(entry?.radicalStrokes)
        binding.dictionaryTotalStrokes.setListValue(entry?.totalStrokes)
        binding.dictionarySimplified.setForms(resolved?.forms(CjkvFormField.Simplified), "zh-CN")
        binding.dictionaryTraditional.setForms(resolved?.forms(CjkvFormField.Traditional), "zh-TW")
        binding.dictionaryJapaneseNew.setForms(resolved?.forms(CjkvFormField.JapaneseNew), "ja-JP")
        binding.dictionaryJapaneseOld.setForms(resolved?.forms(CjkvFormField.JapaneseOld), "ja-JP")
        binding.dictionaryCompatibility.setListValue(entry?.compatibilityVariant, tappable = true)
        binding.dictionarySemanticVariants.setListValue(entry?.semanticVariants, tappable = true)
        binding.dictionarySpecializedSemanticVariants.setListValue(entry?.specializedSemanticVariants, tappable = true)
        binding.dictionaryZVariants.setListValue(entry?.zVariants, tappable = true)
        hideEmptyRows()
    }

    /** Rows with nothing to show are hidden rather than filled with a dash. */
    private fun hideEmptyRows() {
        val missing = context.getString(R.string.cjkv_dictionary_missing)
        (listOf(binding.dictionaryDefinition, binding.dictionaryFourCorner, binding.dictionaryTotalStrokes) + tappableViews())
            .forEach { value ->
                (value.parent as? View)?.visibility =
                    if(value.text.isNullOrEmpty() || value.text.toString() == missing) View.GONE else View.VISIBLE
            }
    }

    private fun showStatus(message: Int) = showStatus(context.getString(message))

    private fun showStatus(message: String) {
        // The header stays for its close button; there is no character to show in it.
        binding.dictionaryCharacter.text = ""
        binding.dictionaryCodePoint.text = ""
        binding.dictionaryNote.visibility = View.GONE
        binding.dictionaryStatus.text = message
        binding.dictionaryStatus.visibility = View.VISIBLE
    }

    private fun TextView.setReading(
        reading: CjkvResolvedReading?,
        languageTag: String,
    ) {
        textLocale = Locale.forLanguageTag(languageTag)
        val items = reading?.value?.split(' ')?.filter(String::isNotEmpty)
        if(items.isNullOrEmpty()) {
            text = context.getString(R.string.cjkv_dictionary_missing)
            return
        }
        // Kun readings mark the okurigana boundary (た-べる); type the word without it.
        text = tappableList(items) { it.replace("-", "") }.apply { appendSource(reading) }
    }

    /** The reading was borrowed from a variant (间 reads as 間); show which, tappable. */
    private fun SpannableStringBuilder.appendSource(reading: CjkvResolvedReading) {
        val codePoint = reading.sourceCodePoint ?: return
        val variant = String(Character.toChars(codePoint))
        append("  ← ")
        val start = length
        append(variant)
        setSpan(TypeSpan(variant), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** A form row; a character reached through another one shows it (让 ← 讓). */
    private fun TextView.setForms(forms: List<CjkvFormVariant>?, languageTag: String) {
        textLocale = Locale.forLanguageTag(languageTag)
        if(forms.isNullOrEmpty()) {
            text = context.getString(R.string.cjkv_dictionary_missing)
            return
        }
        text = SpannableStringBuilder().apply {
            forms.forEachIndexed { index, form ->
                if(index > 0) append(" · ")
                var start = length
                append(form.text)
                setSpan(TypeSpan(form.text), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val via = form.via ?: return@forEachIndexed
                val viaText = String(Character.toChars(via))
                append(" ← ")
                start = length
                append(viaText)
                setSpan(TypeSpan(viaText), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    /** Pinyin followed by its zhuyin (zhòng ㄓㄨㄥˋ), each typed on its own. */
    private fun TextView.setMandarin(reading: CjkvResolvedReading?) {
        textLocale = Locale.forLanguageTag("zh-TW")
        val items = reading?.value?.split(' ')?.filter(String::isNotEmpty)
        if(items.isNullOrEmpty()) {
            text = context.getString(R.string.cjkv_dictionary_missing)
            return
        }
        text = SpannableStringBuilder().apply {
            items.forEachIndexed { index, pinyin ->
                if(index > 0) append(" · ")
                var start = length
                append(pinyin)
                setSpan(TypeSpan(pinyin), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val zhuyin = PinyinZhuyin.convert(pinyin) ?: return@forEachIndexed
                append(' ')
                start = length
                append(zhuyin)
                setSpan(TypeSpan(zhuyin), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            appendSource(reading)
        }
    }

    /** Korean hun/eum and readings in one row; see [CjkvKoreanReading]. */
    private fun TextView.setKorean(reading: CjkvResolvedReading?, hunEum: String?) {
        if(hunEum.isNullOrBlank()) {
            // Only readings, possibly borrowed from a variant (← 間).
            setReading(reading, "ko-KR")
            return
        }
        textLocale = Locale.forLanguageTag("ko-KR")
        val readings = reading?.value?.split(' ')?.filter(String::isNotEmpty).orEmpty()
        text = SpannableStringBuilder().apply {
            CjkvKoreanReading.segments(hunEum, readings).forEach { segment ->
                val start = length
                append(segment.text)
                segment.typed?.let { setSpan(TypeSpan(it), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            }
        }
    }

    private fun TextView.setListValue(value: String?, languageTag: String? = null, tappable: Boolean = false) {
        setListValue(value?.split(' ')?.filter(String::isNotEmpty), languageTag, tappable)
    }

    private fun TextView.setListValue(items: List<String>?, languageTag: String? = null, tappable: Boolean = false) {
        languageTag?.let { textLocale = Locale.forLanguageTag(it) }
        text = when {
            items.isNullOrEmpty() -> context.getString(R.string.cjkv_dictionary_missing)
            tappable -> tappableList(items)
            else -> items.joinToString(" · ")
        }
    }

    private fun reorderReadings(order: List<CjkvDictionaryLanguage>) {
        val groups = mapOf(
            CjkvDictionaryLanguage.Japanese to listOf(
                binding.dictionaryJapaneseOnRow,
                binding.dictionaryJapaneseKunRow,
            ),
            CjkvDictionaryLanguage.Mandarin to listOf(binding.dictionaryMandarinRow),
            CjkvDictionaryLanguage.Cantonese to listOf(binding.dictionaryCantoneseRow),
            CjkvDictionaryLanguage.Korean to listOf(
                binding.dictionaryKoreanRow,
            ),
            CjkvDictionaryLanguage.Vietnamese to listOf(binding.dictionaryVietnameseRow),
        )
        binding.dictionaryReadings.removeAllViews()
        (order + CjkvDictionaryLanguage.entries).distinct().forEach { language ->
            groups.getValue(language).forEach(binding.dictionaryReadings::addView)
        }
    }

    /** "里.2" -> "Radical 里 + 2", with the radical tappable. Older data has a number (166.2). */
    private fun describeRadicalStrokes(value: String?): CharSequence {
        if(value == null) return context.getString(R.string.cjkv_dictionary_missing)
        return SpannableStringBuilder().apply {
            value.split(' ').filter(String::isNotEmpty).forEachIndexed { index, item ->
                if(index > 0) append(" · ")
                val components = item.split('.', limit = 2)
                if(components.size != 2) {
                    append(item)
                    return@forEachIndexed
                }
                val (radical, strokes) = components
                val formatted = context.getString(R.string.cjkv_dictionary_radical_strokes_value, radical, strokes)
                val start = length
                append(formatted)
                val radicalStart = formatted.indexOf(radical)
                if(radicalStart >= 0 && !radical[0].isDigit()) {
                    setSpan(
                        TypeSpan(radical),
                        start + radicalStart,
                        start + radicalStart + radical.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
        }
    }

    private fun formatCodePoint(codePoint: Int): String = "U+%04X".format(codePoint)

    companion object {
        private const val PREFERENCES_NAME = "cjkv_dictionary_panel"
        private const val MAX_HISTORY = 20
        private const val PREF_EXPANDED = "cjkv_dictionary_expanded"
    }
}
