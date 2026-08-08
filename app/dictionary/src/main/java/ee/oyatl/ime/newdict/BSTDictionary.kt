package ee.oyatl.ime.newdict

import java.io.DataOutputStream

class BSTDictionary {

    private var root: Node? = null

    fun insert(key: Int, value: Int) {
        root = insert(root, key, value)
    }

    private fun insert(p: Node?, key: Int, value: Int): Node {
        if(p == null) return Node(key, mutableListOf(value))
        if(key > p.key) p.right = insert(p.right, key, value)
        else if(key < p.key) p.left = insert(p.left, key, value)
        else p.entries += value
        return p
    }

    fun write(os: DataOutputStream) {
        val rootAddress = root?.write(os) ?: -1
        os.writeInt(rootAddress)
    }

    data class Node(
        val key: Int,
        val entries: MutableList<Int>,
        var left: Node? = null,
        var right: Node? = null
    ) {
        fun write(os: DataOutputStream): Int {
            val leftAddress = left?.write(os) ?: -1
            val rightAddress = right?.write(os) ?: -1
            val start = os.size()
            os.writeInt(key)
            os.writeInt(leftAddress)
            os.writeInt(rightAddress)
            os.writeShort(entries.size)
            entries.forEach { entry ->
                os.writeInt(entry)
            }
            return start
        }
    }
}
