package ee.oyatl.ime.fusion.cjkv

enum class CjkvDictionaryLanguage {
    Japanese,
    Mandarin,
    Cantonese,
    Korean,
    Vietnamese,
}

enum class CjkvGlyphLocale(val languageTag: String) {
    Japanese("ja-JP"),
    SimplifiedChinese("zh-CN"),
    TraditionalChinese("zh-TW"),
    HongKongChinese("zh-HK"),
    Korean("ko-KR"),
    Vietnamese("vi-VN"),
}

data class CjkvDictionaryPresentation(
    val languageOrder: List<CjkvDictionaryLanguage>,
    val glyphOrder: List<CjkvGlyphLocale>,
) {
    companion object {
        val Default = CjkvDictionaryPresentation(
            languageOrder = CjkvDictionaryLanguage.entries,
            glyphOrder = CjkvGlyphLocale.entries,
        )
    }
}
