package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskStringDictionary(input: InputStream): DiskDictionary {
    private val data = ByteBuffer.wrap(input.readBytes())

    fun get(index: Int): String {
        var p = data.getInt(index * 4)
        val text = getChars(data, p)
        p += text.length*2 + 2
        return text
    }
}