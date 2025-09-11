package ee.oyatl.ime.newdict

import java.io.InputStream
import java.nio.ByteBuffer

class DiskNGramDictionary(
    input: InputStream
): DiskDictionary<List<Int>, Map<Int, Int>> {
    override val data: ByteBuffer = ByteBuffer.wrap(input.readBytes())

    override fun get(key: List<Int>): Map<Int, Int> {
        var p = data.getInt(data.capacity() - 4)
        for(ind in key) {
            p += 4
            val children = data.getInt(p)
            p += 4
            for(i in 0 until children) {
                val index = data.getInt(p + i*8)
                val addr = data.getInt(p + i*8 + 4)
                if(index == ind) {
                    p = addr
                    break
                } else if(i == children - 1) {
                    return emptyMap()
                }
            }
        }
        val result = mutableMapOf<Int, Int>()
        p += 4
        val children = data.getInt(p)
        p += 4
        for(i in 0 until children) {
            val index = data.getInt(p + i*8)
            val addr = data.getInt(p + i*8 + 4)
            result += index to data.getInt(addr)
        }
        return result
    }
}