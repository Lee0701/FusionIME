package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class JyutpingDictionary
    : MutableDictionary<Int, JyutpingDictionary.Entry>, WritableDictionary<Int, JyutpingDictionary.Entry> {
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
        val word: String,
        val romanization: String
    ) {
        fun write(os: DataOutputStream) {
            os.writeChars(word)
            os.writeShort(0)
            os.writeChars(romanization)
            os.writeShort(0)
        }
    }
}
