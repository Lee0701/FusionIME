package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class HanjaDictionary {
    val entries: MutableList<Entry> = mutableListOf()

    fun insert(hangul: String, hanja: String, frequency: Int, extra: String) {
        entries += Entry(hangul, hanja, frequency, extra)
    }

    fun write(os: DataOutputStream) {
        val bytes = ByteArrayOutputStream()
        val content = DataOutputStream(bytes)
        entries.forEach { entry ->
            os.writeInt(entries.size*4 + content.size())
            entry.write(content)
        }
        os.write(bytes.toByteArray())
    }

    data class Entry(
        val hangul: String,
        val hanja: String,
        val frequency: Int,
        val extra: String
    ) {
        fun write(os: DataOutputStream) {
            os.writeChars(hangul)
            os.writeShort(0)
            os.writeChars(hanja)
            os.writeShort(0)
            os.writeInt(frequency)
            os.writeChars(extra)
            os.writeShort(0)
        }
    }
}