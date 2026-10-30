package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

open class DiskNGramDictionary(
    input: InputStream
): DiskDictionary<List<Int>, Map<Int, Int>> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: List<Int>): Map<Int, Int> {
        var p = getNode(key)
        if(p == -1) return emptyMap()
        p += 4
        val children = data.getInt(p)
        p += 4
        val result = mutableMapOf<Int, Int>()
        for(i in 0 until children) {
            val index = data.getInt(p + i*8)
            val addr = data.getInt(p + i*8 + 4)
            result += index to data.getInt(addr)
        }
        return result
    }

    open fun getNode(key: List<Int>): Int {
        // root
        var p = data.getInt(data.capacity() - 4)
        for(k in key) {
            p = binarySearch(p, k)
            if(p == -1) return -1
        }
        return p
    }

    fun linearSearch(node: Int, key: Int): Int {
        val children = data.getInt(node + 4)
        val p = node + 4 + 4
        for(i in 0 until children) {
            val k = data.getInt(p + i*8)
            if(k == key) {
                return data.getInt(p + i*8 + 4)
            }
        }
        return -1
    }

    fun binarySearch(node: Int, key: Int): Int {
        val children = data.getInt(node + 4)
        val p = node + 4 + 4
        var l = 0
        var r = children - 1
        while(l <= r) {
            val m = l + (r - l) / 2
            val k = data.getInt(p + m*8)
            if(k < key) l = m + 1
            else if(k > key) r = m - 1
            else return data.getInt(p + m*8 + 4)
        }
        return -1
    }
}