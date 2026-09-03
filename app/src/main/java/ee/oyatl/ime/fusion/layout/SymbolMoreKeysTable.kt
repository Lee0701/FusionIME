package ee.oyatl.ime.fusion.layout

object SymbolMoreKeysTable {
    val DEFAULT = mapOf(
        '1' to listOf('¹', '½', '⅓', '¼', '⅕', '⅙', '⅐', '⅛', '⅑', '⅒'),
        '2' to listOf('²', '⅔', '⅖'),
        '3' to listOf('³', '¾', '⅗', '⅜'),
        '4' to listOf('⁴', '⅘'),
        '5' to listOf('⁵', '⅚', '⅝'),
        '6' to listOf('⁶'),
        '7' to listOf('⁷', '⅞'),
        '8' to listOf('⁸'),
        '9' to listOf('⁹'),
        '0' to listOf('⁰', 'ⁿ', '∅'),

        '#' to listOf('№'),
        '$' to listOf('£', '¢', '€', '₹', '¥', '₱'),
        '-' to listOf('—', '_', '–', '·'),
        '+' to listOf('±'),
        '(' to listOf('[', '{', '<'),
        ')' to listOf(']', '}', '>'),

        '*' to listOf('★', '†', '‡'),
        '"' to listOf('„', '“', '”', '«', '»'),
        '\'' to listOf('‚', '‘', '’', '‹', '›'),
        '!' to listOf('¡'),
        '?' to listOf('¿', '‽'),

        '•' to listOf('♣', '♠', '♪', '♥', '♦'),
        'π' to listOf('Ω', 'Π', 'μ'),
        '§' to listOf('¶'),

        '^' to listOf('←', '↓', '↑', '→'),
        '°' to listOf('′', '″'),
        '=' to listOf('∞', '≠', '≈'),
        '{' to listOf('('),
        '}' to listOf(')'),

        '%' to listOf('‰', '℅')
    )
}