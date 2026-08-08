package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.ranges.until

class DiskBSTDictionary(input: InputStream) {
    private val data = ByteBuffer.wrap(input.readBytes())

    fun search(key: Int): List<Int> {
        return search(
            data.getInt(data.capacity() - 4),
            key
        )
    }

    fun search(p: Int, key: Int): List<Int> {
        if(p == -1) return emptyList()
        val k = data.getInt(p)
        if(k > key) return search(data.getInt(p + 4), key)
        else if(k < key) return search(data.getInt(p + 8), key)
        val entries = data.getShort(p + 12)
        return (0 until entries).map { i ->
            data.getInt(p + 14 + i*4)
        }
    }
}
