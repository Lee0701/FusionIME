package ee.oyatl.ime.viet

class ChuQuocNguTableConverter {
    val modeTable = mapOf(
        "q" to mapOf(),
        "t" to ChuQuocNguTable.TELEX
    )

    fun convert(text: String, mode: String): String {
        val table = modeTable[mode] ?: mapOf()
        val result = mutableListOf<String>()
        var i = 0
        while(i < text.length) {
            var found = false
            for(k in (0 .. 5).reversed()) {
                if(i + k > text.length) continue
                val s = text.substring(i, i + k)
                val r = table[s]
                if(r != null) {
                    result += r.toString()
                    i += k
                    found = true
                    break
                }
            }
            if(!found) {
                result += text[i].toString()
                i += 1
            }
        }
        return result.joinToString("")
    }
}