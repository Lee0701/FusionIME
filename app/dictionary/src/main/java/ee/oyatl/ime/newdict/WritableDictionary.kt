package ee.oyatl.ime.newdict

import java.io.DataOutputStream

interface WritableDictionary<K, V>: Dictionary<K, V> {
    fun write(os: DataOutputStream)
}