package ee.oyatl.ime.fusion.zhuyin

import android.content.Context
import com.miyabi_hiroshi.app.libchewing_android_app_module.Chewing
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ChewingConverter {
    val chewing = Chewing()

    val dataFileList = listOf(
        "tsi.dat",
        "word.dat",
        "swkb.dat",
        "symbols.dat"
    )

    fun initialize(context: Context) {
        dataFileList.forEach { name ->
            val file = File(context.cacheDir, name)
            if(!file.exists()) {
                try {
                    val input = context.assets.open(name)
                    val output = FileOutputStream(file)
                    val buffer = ByteArray(input.available())
                    input.read(buffer)
                    input.close()
                    output.write(buffer)
                    output.close()
                } catch(ex: IOException) {
                    throw RuntimeException("Cannot copy chewing data file: $name")
                }
            }
        }
        chewing.connect(context.cacheDir.absolutePath)
        chewing.setChiEngMode(1)
        chewing.setPhraseChoiceRearward(1)
    }

    fun getSuggestions(codes: List<Int>): List<String> {
        codes.forEach { code -> chewing.handleDefault(code.toChar()) }
        chewing.candOpen()
        val bufferString = chewing.bufferStringStatic()
        val list = (0 until chewing.candTotalChoice()).map { i ->
            val candidate = chewing.candStringByIndexStatic(i)
            bufferString.dropLast(candidate.length) + candidate
        }
        chewing.candClose()
        chewing.cleanPreeditBuf()
        chewing.cleanBopomofoBuf()
        return list
    }

    fun destroy() {
        chewing.delete()
    }
}