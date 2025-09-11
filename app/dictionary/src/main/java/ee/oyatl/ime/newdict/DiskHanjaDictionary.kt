package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskHanjaDictionary(input: InputStream): DiskDictionary<Int, HanjaDictionary.Entry> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: Int): HanjaDictionary.Entry {
        var p = data.getInt(key * 4)
        val hangul = DiskDictionary.getChars(data, p)
        p += hangul.length*2 + 2
        val hanja = DiskDictionary.getChars(data, p)
        p += hanja.length*2 + 2
        val frequency = data.getInt(p)
        p += 4
        val extra = DiskDictionary.getChars(data, p)
        p += extra.length*2 + 2
        return HanjaDictionary.Entry(hangul, hanja, frequency, extra)
    }
}