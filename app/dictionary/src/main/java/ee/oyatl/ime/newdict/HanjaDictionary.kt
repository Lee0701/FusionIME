package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class HanjaDictionary
    : MutableDictionary<Int, HanjaDictionary.Entry>, WritableDictionary<Int, HanjaDictionary.Entry> {
    val entries: MutableMap<Int, Entry> = mutableMapOf()

    override fun get(key: Int): Entry? {
        return entries[key]
    }

    override fun insert(key: Int, value: Entry) {
        entries += key to value
    }

    override fun write(os: DataOutputStream) {
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