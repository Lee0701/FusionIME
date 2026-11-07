package ee.oyatl.ime.fusion.cjkv

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque

data class CjkvDictionaryEntry(
    val codePoint: Int,
    val japaneseOn: String? = null,
    val japaneseKun: String? = null,
    val mandarin: String? = null,
    val cantonese: String? = null,
    val korean: String? = null,
    val vietnamese: String? = null,
    val simplifiedVariants: String? = null,
    val traditionalVariants: String? = null,
    val japaneseNewVariant: String? = null,
    val japaneseOldVariants: String? = null,
    val compatibilityVariant: String? = null,
    val sourceRegions: String? = null,
    val cangjie: String? = null,
    val fourCorner: String? = null,
    val koreanHunEum: String? = null,
    val radicalStrokes: String? = null,
    val totalStrokes: String? = null,
    val middleChinese: String? = null,
    val fanqie: String? = null,
    val semanticVariants: String? = null,
    val specializedSemanticVariants: String? = null,
    val zVariants: String? = null,
    val definition: String? = null,
)

enum class CjkvReadingField {
    JapaneseOn,
    JapaneseKun,
    Mandarin,
    Cantonese,
    Korean,
    Vietnamese,
}

data class CjkvResolvedReading(
    val value: String?,
    val sourceCodePoint: Int? = null,
)

/** The four rows that say which script form a character is in. */
enum class CjkvFormField {
    Simplified,
    Traditional,
    JapaneseNew,
    JapaneseOld,
}

/**
 * A character shown in a form row. [via] is set when it was reached through
 * another linked character rather than listed for this one: Unihan records
 * 譲 → 讓 (旧字体) and 讓 → 让 (簡体字) but never 譲 → 让, so 譲's 簡体字
 * row shows 让 via 讓.
 */
data class CjkvFormVariant(
    val text: String,
    val via: Int? = null,
)

data class CjkvResolvedEntry(
    val entry: CjkvDictionaryEntry,
    private val readingSources: Map<CjkvReadingField, Int> = emptyMap(),
    private val forms: Map<CjkvFormField, List<CjkvFormVariant>> = emptyMap(),
) {
    fun forms(field: CjkvFormField): List<CjkvFormVariant> =
        forms[field] ?: entry.form(field).orEmpty().map { CjkvFormVariant(it) }

    fun reading(field: CjkvReadingField): CjkvResolvedReading {
        return CjkvResolvedReading(entry.reading(field), readingSources[field])
    }

    val hasAnyReading: Boolean
        get() = CjkvReadingField.entries.any { entry.reading(it) != null }
}

class CjkvDictionary private constructor(bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN)
    private val entryCount: Int
    private val dataStart: Int

    init {
        require(buffer.limit() >= HEADER_SIZE) { "CJKV dictionary header is truncated" }
        require(buffer.getInt(0) == MAGIC) { "Invalid CJKV dictionary magic" }
        require(buffer.getInt(Int.SIZE_BYTES) == FORMAT_VERSION) { "Unsupported CJKV dictionary format" }
        entryCount = buffer.getInt(Int.SIZE_BYTES * 2)
        require(entryCount >= 0) { "Invalid CJKV dictionary entry count" }
        dataStart = HEADER_SIZE + entryCount * INDEX_ENTRY_SIZE
        require(dataStart in HEADER_SIZE..buffer.limit()) { "CJKV dictionary index is truncated" }
    }

    fun lookup(codePoint: Int): CjkvDictionaryEntry? {
        var low = 0
        var high = entryCount - 1
        while(low <= high) {
            val middle = (low + high).ushr(1)
            val indexPosition = HEADER_SIZE + middle * INDEX_ENTRY_SIZE
            val indexedCodePoint = buffer.getInt(indexPosition)
            when {
                indexedCodePoint < codePoint -> low = middle + 1
                indexedCodePoint > codePoint -> high = middle - 1
                else -> return decodeEntry(codePoint, buffer.getInt(indexPosition + Int.SIZE_BYTES))
            }
        }
        return null
    }

    fun lookupResolved(codePoint: Int): CjkvResolvedEntry? {
        val direct = lookup(codePoint) ?: return null
        var merged = direct
        val readingSources = mutableMapOf<CjkvReadingField, Int>()
        val visited = mutableSetOf(codePoint)
        val queue = ArrayDeque<Pair<Int, Int>>()
        direct.relatedCodePoints().forEach { queue.add(it to 1) }

        while(queue.isNotEmpty() && visited.size < MAX_VARIANT_VISITS) {
            val (candidateCodePoint, depth) = queue.removeFirst()
            if(!visited.add(candidateCodePoint)) continue
            val candidate = lookup(candidateCodePoint) ?: continue
            CjkvReadingField.entries.forEach { field ->
                if(merged.reading(field) == null && candidate.reading(field) != null) {
                    merged = merged.withReading(field, candidate.reading(field))
                    readingSources[field] = candidateCodePoint
                }
            }
            if(depth < MAX_VARIANT_DEPTH) {
                candidate.relatedCodePoints().forEach { queue.add(it to depth + 1) }
            }
        }
        return CjkvResolvedEntry(merged, readingSources, resolveForms(direct))
    }

    /**
     * Fills the form rows across the Chinese and Japanese systems, one link deep.
     *
     * Unihan links forms only within a system: 譲 → 讓 is Japanese, 讓 → 让 is
     * Chinese. The hop goes only through the full form (繁体字 or 旧字体),
     * which both systems share; a reduced form can be an unrelated character in
     * the other system (Japanese 芸 for 藝 is also Chinese 芸 yún), so it is
     * never crossed. From the full form, its forms in the other system are
     * added (譲 → 讓 → 让), and the full form itself goes in the row it plays in
     * that system (譲's 繁体字 row gets 讓).
     */
    private fun resolveForms(direct: CjkvDictionaryEntry): Map<CjkvFormField, List<CjkvFormVariant>> {
        val self = String(Character.toChars(direct.codePoint))
        val rows = CjkvFormField.entries.associateWith { field ->
            direct.form(field).orEmpty().map { CjkvFormVariant(it) }.toMutableList()
        }
        fun add(field: CjkvFormField, variant: CjkvFormVariant) {
            val row = rows.getValue(field)
            if(variant.text != self && row.none { it.text == variant.text }) row += variant
        }
        val crossings = listOf(
            // via 繁体字: the Japanese forms of the traditional character
            CjkvFormField.Traditional to listOf(CjkvFormField.JapaneseNew, CjkvFormField.JapaneseOld),
            // via 旧字体: the Chinese forms of the old character
            CjkvFormField.JapaneseOld to listOf(CjkvFormField.Simplified, CjkvFormField.Traditional),
        )
        crossings.forEach { (link, otherSystem) ->
            direct.form(link).orEmpty().filter { it != self }.forEach { full ->
                val codePoint = full.codePointAt(0)
                val other = lookup(codePoint) ?: return@forEach
                otherSystem.forEach { field ->
                    // The full form plays this role if it has a form on the other side (讓 → 让).
                    if(other.form(field.counterpart).orEmpty().any { it != full }) add(field, CjkvFormVariant(full))
                    other.form(field).orEmpty().filter { it != full }.forEach { add(field, CjkvFormVariant(it, codePoint)) }
                }
            }
        }
        return rows
    }

    private fun decodeEntry(codePoint: Int, offset: Int): CjkvDictionaryEntry {
        val position = dataStart + offset
        require(position in dataStart until buffer.limit()) { "Invalid CJKV dictionary record offset" }
        val record = buffer.duplicate().order(ByteOrder.BIG_ENDIAN).apply { this.position(position) }
        require(record.remaining() >= Int.SIZE_BYTES) { "CJKV dictionary record is truncated" }
        val mask = record.int
        val fields = arrayOfNulls<String>(FIELD_COUNT)
        for(index in fields.indices) {
            if(mask and (1 shl index) == 0) continue
            val byteCount = readUnsignedShort(record)
            require(byteCount <= record.remaining()) { "CJKV dictionary field is truncated" }
            val bytes = ByteArray(byteCount)
            record.get(bytes)
            fields[index] = bytes.toString(Charsets.UTF_8)
        }
        return CjkvDictionaryEntry(
            codePoint = codePoint,
            japaneseOn = fields[0],
            japaneseKun = fields[1],
            mandarin = fields[2],
            cantonese = fields[3],
            korean = fields[4],
            vietnamese = fields[5],
            simplifiedVariants = fields[6],
            traditionalVariants = fields[7],
            japaneseNewVariant = fields[8],
            japaneseOldVariants = fields[9],
            compatibilityVariant = fields[10],
            sourceRegions = fields[11],
            cangjie = fields[12],
            fourCorner = fields[13],
            koreanHunEum = fields[14],
            radicalStrokes = fields[15],
            totalStrokes = fields[16],
            middleChinese = fields[17],
            fanqie = fields[18],
            semanticVariants = fields[19],
            specializedSemanticVariants = fields[20],
            zVariants = fields[21],
            definition = fields[22],
        )
    }

    private fun readUnsignedShort(source: ByteBuffer): Int {
        require(source.remaining() >= Short.SIZE_BYTES) { "CJKV dictionary record is truncated" }
        return source.short.toInt() and 0xFFFF
    }

    companion object {
        private const val ASSET_NAME = "unihan_cjkv.bin"
        private const val MAGIC = 0x434A4B56
        private const val FORMAT_VERSION = 4
        private const val HEADER_SIZE = Int.SIZE_BYTES * 3
        private const val INDEX_ENTRY_SIZE = Int.SIZE_BYTES * 2
        private const val FIELD_COUNT = 23
        private const val MAX_VARIANT_DEPTH = 2
        private const val MAX_VARIANT_VISITS = 32

        fun load(context: Context): CjkvDictionary {
            val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
            return CjkvDictionary(bytes)
        }

        internal fun fromBytes(bytes: ByteArray): CjkvDictionary = CjkvDictionary(bytes)
    }
}

private fun CjkvDictionaryEntry.reading(field: CjkvReadingField): String? {
    return when(field) {
        CjkvReadingField.JapaneseOn -> japaneseOn
        CjkvReadingField.JapaneseKun -> japaneseKun
        CjkvReadingField.Mandarin -> mandarin
        CjkvReadingField.Cantonese -> cantonese
        CjkvReadingField.Korean -> korean
        CjkvReadingField.Vietnamese -> vietnamese
    }
}

private fun CjkvDictionaryEntry.withReading(field: CjkvReadingField, value: String?): CjkvDictionaryEntry {
    return when(field) {
        CjkvReadingField.JapaneseOn -> copy(japaneseOn = value)
        CjkvReadingField.JapaneseKun -> copy(japaneseKun = value)
        CjkvReadingField.Mandarin -> copy(mandarin = value)
        CjkvReadingField.Cantonese -> copy(cantonese = value)
        CjkvReadingField.Korean -> copy(korean = value)
        CjkvReadingField.Vietnamese -> copy(vietnamese = value)
    }
}

private fun CjkvDictionaryEntry.relatedCodePoints(): List<Int> {
    return listOfNotNull(
        compatibilityVariant,
        japaneseNewVariant,
        japaneseOldVariants,
        simplifiedVariants,
        traditionalVariants,
        semanticVariants,
        specializedSemanticVariants,
        zVariants,
    ).flatMap { variants ->
        variants.split(' ').mapNotNull { value ->
            value.takeIf(String::isNotEmpty)?.codePointAt(0)
        }
    }
}

private fun CjkvDictionaryEntry.form(field: CjkvFormField): List<String>? {
    val value = when(field) {
        CjkvFormField.Simplified -> simplifiedVariants
        CjkvFormField.Traditional -> traditionalVariants
        CjkvFormField.JapaneseNew -> japaneseNewVariant
        CjkvFormField.JapaneseOld -> japaneseOldVariants
    }
    return value?.split(' ')?.filter(String::isNotEmpty)
}

/** A character that has a form in [counterpart] is itself in this form. */
private val CjkvFormField.counterpart: CjkvFormField
    get() = when(this) {
        CjkvFormField.Simplified -> CjkvFormField.Traditional
        CjkvFormField.Traditional -> CjkvFormField.Simplified
        CjkvFormField.JapaneseNew -> CjkvFormField.JapaneseOld
        CjkvFormField.JapaneseOld -> CjkvFormField.JapaneseNew
    }
