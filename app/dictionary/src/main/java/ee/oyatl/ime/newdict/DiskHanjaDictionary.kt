package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskHanjaDictionary(input: InputStream): DiskDictionary {
    private val data = ByteBuffer.wrap(input.readBytes())

    fun get(index: Int): HanjaDictionary.Entry {
        var p = data.getInt(index * 4)
        val hangul = getChars(data, p)
        p += hangul.length*2 + 2
        val hanja = getChars(data, p)
        p += hanja.length*2 + 2
        val frequency = data.getInt(p)
        p += 4
        val extra = getChars(data, p)
        p += extra.length*2 + 2
        return HanjaDictionary.Entry(hangul, hanja, frequency, extra)
    }
}