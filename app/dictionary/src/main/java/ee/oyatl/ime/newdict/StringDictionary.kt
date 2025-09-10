package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class StringDictionary
    : MutableDictionary<Int, String>, WritableDictionary<Int, String> {
    val entries: MutableMap<Int, String> = mutableMapOf()

    override fun get(key: Int): String? {
        return entries[key]
    }

    override fun insert(key: Int, value: String) {
        entries += key to value
    }

    override fun write(os: DataOutputStream) {
        val bytes = ByteArrayOutputStream()
        val content = DataOutputStream(bytes)
        val length = entries.keys.max() + 1
        (0 until length).forEach { index ->
            os.writeInt(length * 4 + content.size())
            content.writeChars(entries[index] ?: "")
            content.writeShort(0)
        }
        os.write(bytes.toByteArray())
    }
}