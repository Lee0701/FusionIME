package ee.oyatl.ime.fusion.layout

object LatinLongPressTable {
    val DEFAULT = mapOf(
        'a'.code to listOf('à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā'),
        'c'.code to listOf('ç', 'ć', 'č'),
        'e'.code to listOf('è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę'),
        'i'.code to listOf('î', 'ï', 'í', 'ī', 'į', 'ì'),
        'l'.code to listOf('ł'),
        'n'.code to listOf('ñ', 'ń'),
        'o'.code to listOf('ô', 'ö', 'ò', 'ó', 'œ', 'ø', 'ō', 'õ'),
        's'.code to listOf('ß', 'ś', 'š'),
        'u'.code to listOf('û', 'ü', 'ù', 'ú', 'ū'),
        'y'.code to listOf('ÿ'),
        'z'.code to listOf('ž', 'ź', 'ż'),
        '1'.code to listOf('¹', '½', '⅓', '¼', '⅕', '⅙', '⅐', '⅛', '⅑', '⅒'),
        '2'.code to listOf('²', '⅔', '⅖'),
        '3'.code to listOf('³', '¾', '⅗', '⅜'),
        '4'.code to listOf('⁴', '⅘'),
        '5'.code to listOf('⁵', '⅚', '⅝'),
        '6'.code to listOf('⁶'),
        '7'.code to listOf('⁷'),
        '8'.code to listOf('⁸'),
        '9'.code to listOf('⁹'),
        '0'.code to listOf('⁰', 'ⁿ', '∅'),
        '#'.code to listOf('№'),
        '-'.code to listOf('_', '–', '—', '·', '†', '‡', '★', '±'),
        '('.code to listOf('<', '{', '['),
        ')'.code to listOf('>', '}', ']'),
        '"'.code to listOf('“', '”', '„', '«', '»'),
        '\''.code to listOf('‘', '’', '‚', '‹', '›'),
        '!'.code to listOf('¡'),
        '?'.code to listOf('¿', '‽'),
        '.'.code to listOf('…', '!', '?', '\'', '/', ';', ':', '"', '%', '@'),
        '='.code to listOf('≠', '≈'),
        '^'.code to listOf('←', '↑', '↓', '→'),
        '<'.code to listOf('≤', '≪', '⟨'),
        '>'.code to listOf('≥', '≫', '⟩'),
        '/'.code to listOf('\\', '÷'),
        '%'.code to listOf('‰', '℅'),
        '&'.code to listOf('§'),
        '$'.code to listOf('€', '£', '¥', '₩', '¢')
    )

    val DEFAULT_UPPERCASE_OVERRIDE = mapOf(
        'ß'.code to 'ẞ'.code
    )
}