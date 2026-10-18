package ee.oyatl.ime.keyboard

object LatinLongPressTable {
    val Default = LongPressTable(
        mapOf(
            'a'.code to listOf('à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā').map(Char::code),
            'c'.code to listOf('ç', 'ć', 'č').map(Char::code),
            'e'.code to listOf('è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę').map(Char::code),
            'i'.code to listOf('î', 'ï', 'í', 'ī', 'į', 'ì').map(Char::code),
            'l'.code to listOf('ł').map(Char::code),
            'n'.code to listOf('ñ', 'ń').map(Char::code),
            'o'.code to listOf('ô', 'ö', 'ò', 'ó', 'œ', 'ø', 'ō', 'õ').map(Char::code),
            's'.code to listOf('ß', 'ś', 'š').map(Char::code),
            'u'.code to listOf('û', 'ü', 'ù', 'ú', 'ū').map(Char::code),
            'y'.code to listOf('ÿ').map(Char::code),
            'z'.code to listOf('ž', 'ź', 'ż').map(Char::code),
            '1'.code to listOf('¹', '½', '⅓', '¼', '⅕', '⅙', '⅐', '⅛', '⅑', '⅒').map(Char::code),
            '2'.code to listOf('²', '⅔', '⅖').map(Char::code),
            '3'.code to listOf('³', '¾', '⅗', '⅜').map(Char::code),
            '4'.code to listOf('⁴', '⅘').map(Char::code),
            '5'.code to listOf('⁵', '⅚', '⅝').map(Char::code),
            '6'.code to listOf('⁶').map(Char::code),
            '7'.code to listOf('⁷').map(Char::code),
            '8'.code to listOf('⁸').map(Char::code),
            '9'.code to listOf('⁹').map(Char::code),
            '0'.code to listOf('⁰', 'ⁿ', '∅').map(Char::code),
            '#'.code to listOf('№').map(Char::code),
            '-'.code to listOf('_', '–', '—', '·', '†', '‡', '★', '±').map(Char::code),
            '('.code to listOf('<', '{', '[').map(Char::code),
            ')'.code to listOf('>', '}', ']').map(Char::code),
            '"'.code to listOf('“', '”', '„', '«', '»').map(Char::code),
            '\''.code to listOf('‘', '’', '‚', '‹', '›').map(Char::code),
            '!'.code to listOf('¡').map(Char::code),
            '?'.code to listOf('¿', '‽').map(Char::code),
            '.'.code to listOf('…', '!', '?', '\'', '/', ';', ':', '"', '%', '@').map(Char::code),
            '='.code to listOf('≠', '≈').map(Char::code),
            '^'.code to listOf('←', '↑', '↓', '→').map(Char::code),
            '<'.code to listOf('≤', '≪', '⟨').map(Char::code),
            '>'.code to listOf('≥', '≫', '⟩').map(Char::code),
            '/'.code to listOf('\\', '÷').map(Char::code),
            '%'.code to listOf('‰', '℅').map(Char::code),
            '&'.code to listOf('§').map(Char::code),
            '$'.code to listOf('€', '£', '¥', '₩', '¢').map(Char::code)
        ),
        uppercaseOverrides = mapOf(
            'ß'.code to 'ẞ'.code
        )
    )
}
