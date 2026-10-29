package ee.oyatl.ime.fusion.keyboard.host

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetWriter
import ee.oyatl.ime.fusion.layout.preset.CangjieLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.JapaneseLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.JyutpingLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.KoreanLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.PinyinLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.VietLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.ZhuyinLayoutPresets
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

fun export(dirname: String, presets: Map<String, KeyboardLayoutPreset>) {
    val exportDir = File(dirname)
    if(exportDir.exists()) exportDir.deleteRecursively()
    exportDir.mkdirs()
    val exporter = KeyboardLayoutExporter(exportDir.path)
    presets.forEach { (name, preset) -> exporter.save(name, preset) }
}

fun main(args: Array<String>) {
    val dirname = "app/src/main/assets/keyboard"

    export("$dirname", mapOf(
        "hangul_2set_ks" to KoreanLayoutPresets.hangul2SetKS(),
        "hangul_3set_390" to KoreanLayoutPresets.hangul3Set390(),
        "hangul_3set_391" to KoreanLayoutPresets.hangul3Set391(),
        "hangul_3set_391_strict" to KoreanLayoutPresets.hangul3Set391Strict(),
        "hangul_old_2set" to KoreanLayoutPresets.hangulOld2Set(),
        "hangul_old_3set_393" to KoreanLayoutPresets.hangul3Set393()
    ))
}