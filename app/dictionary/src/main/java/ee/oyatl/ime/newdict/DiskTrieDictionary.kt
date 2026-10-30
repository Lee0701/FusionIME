package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

open class DiskTrieDictionary(input: InputStream)
    : DiskDictionary<String, List<Int>> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: String): List<Int> {
        var p = getNode(key)
        if(p == -1) return emptyList()
        val children = data.getShort(p)
        p += 2 + children*6
        val entries = data.getShort(p)
        p += 2
        return (0 until entries).map {
            val index = data.getInt(p)
            p += 4
            index
        }
    }

    open fun getNode(key: String): Int {
        // root
        var p = data.getInt(data.capacity() - 4)
        for(c in key) {
            p = binarySearch(p, c)
            if(p == -1) return -1
        }
        return p
    }

    fun linearSearch(node: Int, key: Char): Int {
        val children = data.getShort(node)
        val p = node + 2
        for(i in 0 until children) {
            val ch = data.getChar(p + i*6)
            if(ch == key) {
                return data.getInt(p + i*6 + 2)
            }
        }
        return -1
    }

    fun binarySearch(node: Int, key: Char): Int {
        val children = data.getShort(node)
        val p = node + 2
        var l = 0
        var r = children - 1
        while(l <= r) {
            val m = l + (r - l) / 2
            val ch = data.getChar(p + m*6)
            if(ch < key) l = m + 1
            else if(ch > key) r = m - 1
            else return data.getInt(p + m*6 + 2)
        }
        return -1
    }
}
