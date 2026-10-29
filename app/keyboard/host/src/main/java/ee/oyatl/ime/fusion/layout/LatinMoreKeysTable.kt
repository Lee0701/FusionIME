package ee.oyatl.ime.fusion.layout

object LatinMoreKeysTable {
    val DEFAULT = mapOf(
        'a' to listOf('à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā'),
        'c' to listOf('ç', 'ć', 'č'),
        'e' to listOf('è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę'),
        'i' to listOf('î', 'ï', 'í', 'ī', 'į', 'ì'),
        'l' to listOf('ł'),
        'n' to listOf('ñ', 'ń'),
        'o' to listOf('ô', 'ö', 'ò', 'ó', 'œ', 'ø', 'ō', 'õ'),
        's' to listOf('ß', 'ś', 'š'),
        'u' to listOf('û', 'ü', 'ù', 'ú', 'ū'),
        'y' to listOf('ÿ'),
        'z' to listOf('ž', 'ź', 'ż'),
        '1' to listOf('¹', '½', '⅓', '¼', '⅕', '⅙', '⅐', '⅛', '⅑', '⅒'),
        '2' to listOf('²', '⅔', '⅖'),
        '3' to listOf('³', '¾', '⅗', '⅜'),
        '4' to listOf('⁴', '⅘'),
        '5' to listOf('⁵', '⅚', '⅝'),
        '6' to listOf('⁶'),
        '7' to listOf('⁷'),
        '8' to listOf('⁸'),
        '9' to listOf('⁹'),
        '0' to listOf('⁰', 'ⁿ', '∅'),
        '#' to listOf('№'),
        '-' to listOf('_', '–', '—', '·', '†', '‡', '★', '±'),
        '(' to listOf('<', '{', '['),
        ')' to listOf('>', '}', ']'),
        '"' to listOf('“', '”', '„', '«', '»'),
        '\'' to listOf('‘', '’', '‚', '‹', '›'),
        '!' to listOf('¡'),
        '?' to listOf('¿', '‽'),
        '.' to listOf('…', '!', '?', '\'', '/', ';', ':', '"', '%', '@'),
        '=' to listOf('≠', '≈'),
        '^' to listOf('←', '↑', '↓', '→'),
        '<' to listOf('≤', '≪', '⟨'),
        '>' to listOf('≥', '≫', '⟩'),
        '/' to listOf('\\', '÷'),
        '%' to listOf('‰', '℅'),
        '&' to listOf('§'),
        '$' to listOf('€', '£', '¥', '₩', '¢')
    )

    val DEFAULT_UPPERCASE_OVERRIDE = mapOf(
        'ß' to 'ẞ'
    )
}