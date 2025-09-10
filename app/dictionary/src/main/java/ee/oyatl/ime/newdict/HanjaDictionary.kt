package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class HanjaDictionary {
    val entries: MutableMap<Int, Entry> = mutableMapOf()

    fun insert(index: Int, content: Entry) {
        entries += index to content
    }

    fun write(os: DataOutputStream) {
        val bytes = ByteArrayOutputStream()
        val content = DataOutputStream(bytes)
        val length = entries.keys.max() + 1
        (0 until length).forEach { index ->
            os.writeInt(length * 4 + content.size())
            entries[index]?.write(content)
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