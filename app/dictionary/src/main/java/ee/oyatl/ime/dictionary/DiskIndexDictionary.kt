package ee.oyatl.ime.dictionary

import java.io.InputStream
import java.nio.ByteBuffer

class DiskIndexDictionary(input: InputStream) {
    private val data = ByteBuffer.wrap(input.readBytes())

    fun search(key: String): List<Int> {
        // root
        var p = data.getInt(data.capacity() - 4)
        for(c in key) {
            // children count
            val children = data.getChar(p).code
            for(i in 0 until children) {
                val ch = data.getChar(p + 2 + i*6)
                val addr = data.getInt(p + 2 + i*6 + 2)
                if(ch == c) {
                    p = addr
                    break
                } else if(i == children - 1) {
                    return listOf()
                }
            }
        }
        val children = data.getChar(p).code
        p += 2 + children*6
        val entries = data.getChar(p).code
        p += 2
        return (0 until entries).map {
            val entry = data.getChar(p).code
            p += 2
            entry
        }
    }

    fun search(key: List<Int>): List<Int> {
        return search(key.map { it.toChar() }.joinToString(""))
    }
}
