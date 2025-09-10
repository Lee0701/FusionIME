package ee.oyatl.ime.newdict

import java.lang.StringBuilder
import java.nio.ByteBuffer

interface DiskDictionary {
    fun getChars(bb: ByteBuffer, idx: Int): String {
        val sb = StringBuilder()
        var i = 0
        while(true) {
            val c = bb.getChar(idx + i)
            if(c.code == 0) break
            sb.append(c)
            i += 2
        }
        return sb.toString()
    }
}