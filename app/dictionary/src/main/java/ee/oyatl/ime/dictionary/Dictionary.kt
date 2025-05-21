package ee.oyatl.ime.dictionary

import java.io.DataOutputStream

class Dictionary {

    private val root = Node()

    fun insert(key: String, entry: Entry) {
        var p = root
        for(c in key) {
            p = p.children.getOrPut(c) { Node() }
        }
        p.entries += entry
    }

    fun write(os: DataOutputStream) {
        val rootAddress = root.write(os)
        os.writeInt(rootAddress)
    }

    data class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        val entries: MutableList<Entry> = mutableListOf()
    ) {
        fun write(os: DataOutputStream): Int {
            val childrenMap = children.mapValues { (c, node) ->
                node.write(os)
            }
            val start = os.size()
            os.writeShort(children.size)
            childrenMap.forEach { (c, address) ->
                os.writeChar(c.toInt())
                os.writeInt(address)
            }
            os.writeShort(entries.size)
            entries.forEach { entry ->
                entry.write(os)
            }
            return start
        }
    }

    data class Entry(
        val result: String,
        val extra: String,
        val frequency: Int
    ) {
        fun write(os: DataOutputStream) {
            os.writeChars(result)
            os.writeShort(0)
            os.writeChars(extra)
            os.writeShort(0)
            os.writeShort(frequency)
        }
    }
}
