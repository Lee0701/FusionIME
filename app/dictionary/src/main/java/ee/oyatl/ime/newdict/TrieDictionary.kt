package ee.oyatl.ime.newdict

import java.io.DataOutputStream

class TrieDictionary
    : MutableDictionary<String, List<Int>>, WritableDictionary<String, List<Int>> {
    private val root = Node()

    override fun get(key: String): List<Int>? {
        var p = root
        for(c in key) {
            p = p.children.get(c) ?: return null
        }
        return p.entries
    }

    override fun insert(key: String, value: List<Int>) {
        var p = root
        for(c in key) {
            p = p.children.getOrPut(c) { Node() }
        }
        p.entries += value
    }

    override fun write(os: DataOutputStream) {
        val rootAddress = root.write(os)
        os.writeInt(rootAddress)
    }

    data class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        val entries: MutableList<Int> = mutableListOf()
    ) {
        fun write(os: DataOutputStream): Int {
            val childrenMap = children.mapValues { (c, node) ->
                node.write(os)
            }
            val start = os.size()
            os.writeShort(children.size)
            childrenMap.forEach { (c, address) ->
                os.writeChar(c.code)
                os.writeInt(address)
            }
            os.writeShort(entries.size)
            entries.forEach { entry ->
                os.writeInt(entry)
            }
            return start
        }
    }
}
