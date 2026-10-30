package ee.oyatl.ime.dictionary.manager

import ee.oyatl.ime.newdict.Dictionary

object DictionaryCache {
    private val cache: MutableMap<Int, Dictionary<*, *>> = mutableMapOf()
    fun <T: Dictionary<*, *>> get(key: Int, init: () -> T): T {
        val dict = cache[key] ?: init()
        cache[key] = dict
        @Suppress("UNCHECKED_CAST")
        return dict as T
    }
}