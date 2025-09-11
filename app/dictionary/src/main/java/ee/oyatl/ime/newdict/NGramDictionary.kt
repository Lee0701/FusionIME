package ee.oyatl.ime.newdict

import java.io.DataOutputStream

class NGramDictionary: MutableDictionary<List<Int>, Map<Int, Int>>, WritableDictionary<List<Int>, Map<Int, Int>> {
    private val root = Node()

    override fun get(key: List<Int>): Map<Int, Int> {
        var p = root
        for(i in key) {
            p = p.children.get(i) ?: return emptyMap()
        }
        return p.children
            .map { (key, node) -> key to node.frequency }
            .filter { (key, freq) -> freq > 0 }
            .toMap()
    }

    override fun insert(key: List<Int>, value: Map<Int, Int>) {
        var p = root
        for(i in key) {
            p = p.children.getOrPut(i) { Node() }
        }
        value.forEach { (i, freq) -> p.children += i to Node(freq) }
    }

    override fun write(os: DataOutputStream) {
        val rootAddress = root.write(os)
        os.writeInt(rootAddress)
    }

    data class Node(
        val frequency: Int = 0,
        val children: MutableMap<Int, Node> = mutableMapOf()
    ) {
        fun write(os: DataOutputStream): Int {
            val childrenMap = children.mapValues { (i, node) ->
                node.write(os)
            }
            val start = os.size()
            os.writeInt(frequency)
            os.writeInt(children.size)
            childrenMap.forEach { (i, address) ->
                os.writeInt(i)
                os.writeInt(address)
            }
            return start
        }
    }
}