package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskStringDictionary(input: InputStream)
    : DiskDictionary<Int, String> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: Int): String {
        var p = data.getInt(key * 4)
        val text = DiskDictionary.getChars(data, p)
        p += text.length*2 + 2
        return text
    }
}