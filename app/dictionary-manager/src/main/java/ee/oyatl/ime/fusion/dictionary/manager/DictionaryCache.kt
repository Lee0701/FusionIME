package ee.oyatl.ime.fusion.dictionary.manager

import androidx.annotation.RawRes
import ee.oyatl.ime.newdict.Dictionary

object DictionaryCache {
    private val cache: MutableMap<Int, Dictionary<*, *>> = mutableMapOf()
    fun <T: Dictionary<*, *>> get(@RawRes key: Int, init: () -> T): T {
        val dict = cache[key] ?: init()
        cache[key] = dict
        @Suppress("UNCHECKED_CAST")
        return dict as T
    }
}