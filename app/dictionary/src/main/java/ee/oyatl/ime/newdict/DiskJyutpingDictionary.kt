package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskJyutpingDictionary(input: InputStream): DiskDictionary<Int, JyutpingDictionary.Entry> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: Int): JyutpingDictionary.Entry {
        var p = data.getInt(key * 4)
        val hangul = DiskDictionary.getChars(data, p)
        p += hangul.length*2 + 2
        val hanja = DiskDictionary.getChars(data, p)
        p += hanja.length*2 + 2
        return JyutpingDictionary.Entry(hangul, hanja)
    }
}
