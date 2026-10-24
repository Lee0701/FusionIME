package ee.oyatl.ime.newdict

import java.io.InputStream

class CachedDiskTrieDictionary(
    input: InputStream
): DiskTrieDictionary(input) {
    val cache: MutableMap<String, Int> = mutableMapOf()

    override fun getNode(key: String): Int {
        return cache.getOrPut(key) { super.getNode(key) }
    }

    fun clearCache() {
        cache.clear()
    }
}