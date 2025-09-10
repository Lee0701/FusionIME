package ee.oyatl.ime.newdict

interface MutableDictionary<K, V>: Dictionary<K, V> {
    fun insert(key: K, value: V)
}