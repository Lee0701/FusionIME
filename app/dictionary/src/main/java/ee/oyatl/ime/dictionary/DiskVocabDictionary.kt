package ee.oyatl.ime.dictionary

import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class DiskVocabDictionary(input: InputStream) {
    private val data = ByteBuffer.wrap(input.readBytes())

    operator fun get(index: Int): Entry {
        val offset = data.getInt(index * Int.SIZE_BYTES)
        val result = DiskDictionary.getChars(data, offset)
        val frequency = data.getShort(offset + result.length * 2 + 2).toInt()
        return Entry(result, frequency)
    }

    data class Entry(
        val result: String,
        val frequency: Int
    )

    companion object {
        private const val MAX_LEN = 1 shl Short.SIZE_BITS
        fun write(output: OutputStream, data: List<Entry>) {
            val os = DataOutputStream(output)
            var offset = Int.SIZE_BYTES * MAX_LEN
            data.forEach { entry ->
                os.writeInt(offset)
                offset += entry.result.length * 2 + 2 + 2
            }
            (0 until (MAX_LEN - data.size)).forEach { _ ->
                os.writeInt(0)
            }
            data.forEach { entry ->
                os.writeChars(entry.result)
                os.writeShort(0)
                os.writeShort(entry.frequency)
            }
        }
    }
}