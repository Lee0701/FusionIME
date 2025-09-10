package ee.oyatl.ime.newdict

interface Dictionary<K, V> {
    fun get(key: K): V?
}