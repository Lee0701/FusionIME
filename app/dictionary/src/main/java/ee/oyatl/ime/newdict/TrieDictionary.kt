package ee.oyatl.ime.newdict

import java.io.DataOutputStream

class TrieDictionary(
    private val root: Node = Node()
): MutableDictionary<String, List<Int>>, WritableDictionary<String, List<Int>> {


    override fun get(key: String): List<Int> {
        var p = root
        for(c in key) {
            p = p.children.get(c) ?: return emptyList()
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

    fun sorted(): TrieDictionary {
        return TrieDictionary(root.sorted())
    }

    override fun write(os: DataOutputStream) {
        val rootAddress = root.write(os)
        os.writeInt(rootAddress)
    }

    data class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        val entries: MutableList<Int> = mutableListOf()
    ) {
        fun sorted(): Node {
            return Node(
                children.mapValues { (_, v) -> v.sorted() }.toSortedMap(),
                entries.sorted().toMutableList()
            )
        }

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
