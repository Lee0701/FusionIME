package ee.oyatl.ime.newdict

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class StringDictionary {
    val entries: MutableList<String> = mutableListOf()

    fun insert(text: String) {
        entries += text
    }

    fun write(os: DataOutputStream) {
        val bytes = ByteArrayOutputStream()
        val content = DataOutputStream(bytes)
        entries.forEach { text ->
            os.writeInt(entries.size*4 + content.size())
            content.writeChars(text)
            content.writeShort(0)
        }
        os.write(bytes.toByteArray())
    }

}