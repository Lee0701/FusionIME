package ee.oyatl.ime.fusion.keyboard.host

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetWriter
import ee.oyatl.ime.fusion.layout.preset.KoreanLayoutPresets
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class KeyboardLayoutExporter(
    val exportDir: String
): KeyboardLayoutPresetWriter() {
    override fun openOutput(name: String): OutputStream {
        return FileOutputStream(File(File(exportDir), "$name.yaml"))
    }
}

fun main(args: Array<String>) {
    val exportDir = File("app/src/main/assets/keyboard")
    if(exportDir.exists()) exportDir.deleteRecursively()
    exportDir.mkdirs()
    val exporter = KeyboardLayoutExporter(exportDir.path)
    exporter.save("hangul_2set_ks", KoreanLayoutPresets.hangul2SetKS())
}