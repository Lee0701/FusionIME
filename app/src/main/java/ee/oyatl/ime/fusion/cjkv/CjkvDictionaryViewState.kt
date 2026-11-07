package ee.oyatl.ime.fusion.cjkv

sealed interface CjkvDictionaryViewState {
    data class Found(
        val character: CursorHanCharacter,
        val entry: CjkvResolvedEntry?,
    ): CjkvDictionaryViewState

    data class NotHan(
        val text: String,
        val codePoint: Int,
    ): CjkvDictionaryViewState

    data object Empty: CjkvDictionaryViewState
    data object Unavailable: CjkvDictionaryViewState
    data object SensitiveInput: CjkvDictionaryViewState
    data object DataError: CjkvDictionaryViewState
}
