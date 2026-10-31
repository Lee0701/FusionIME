package ee.oyatl.ime.fusion.keyboard.host

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetWriter
import ee.oyatl.ime.fusion.layout.preset.CangjieLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.JapaneseLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.JyutpingLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.KoreanLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.PinyinLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.SymbolLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.VietLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.ZhuyinLayoutPresets
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class KeyboardLayoutGenerator(
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
    val exporter = KeyboardLayoutGenerator(exportDir.path)
    presets.forEach { (name, preset) -> exporter.save(name, preset) }
}

fun main(args: Array<String>) {
    val dirname =
        if(args.size == 1) args[0]
        else "app/src/main/assets/keyboard"

    export("$dirname/latin", mapOf(
        "qwerty" to LatinLayoutPresets.qwerty(),
        "qwerty_spanish" to LatinLayoutPresets.spanishQwerty(),
        "azerty" to LatinLayoutPresets.azerty(),
        "qwertz" to LatinLayoutPresets.qwertz(),
        "qwerz_german" to LatinLayoutPresets.germanQwertz(),
        "dvorak" to LatinLayoutPresets.dvorak(),
        "colemak" to LatinLayoutPresets.colemak()
    ))

    export("$dirname/pinyin", mapOf(
        "pinyin_qwerty" to PinyinLayoutPresets.pinyin(LatinLayoutPresets.qwerty(false)),
        "pinyin_qwerty_semicolon" to PinyinLayoutPresets.pinyin(LatinLayoutPresets.qwerty(true))
    ))

    export("$dirname/zhuyin", mapOf(
        "bopomofo" to ZhuyinLayoutPresets.bopomofo()
    ))

    export("$dirname/cangjie", mapOf(
        "cangjie" to CangjieLayoutPresets.cangjie(),
        "dayi3" to CangjieLayoutPresets.dayi3()
    ))

    export("$dirname/jyutping", mapOf(
        "jyutping_qwerty" to JyutpingLayoutPresets.jyutping(LatinLayoutPresets.qwerty())
    ))

    export("$dirname/japanese", mapOf(
        "romaji_qwerty" to JapaneseLayoutPresets.romajiQwertyCompatible(LatinLayoutPresets.qwerty(true)),
        "kana_12key" to JapaneseLayoutPresets.kana12Key(),
        "godan" to JapaneseLayoutPresets.godan(),
        "kana_jis" to JapaneseLayoutPresets.jis()
    ))

    export("$dirname/korean", mapOf(
        "hangul_2set_ks" to KoreanLayoutPresets.hangul2SetKS(),
        "hangul_3set_390" to KoreanLayoutPresets.hangul3Set390(),
        "hangul_3set_391" to KoreanLayoutPresets.hangul3Set391(),
        "hangul_3set_391_strict" to KoreanLayoutPresets.hangul3Set391Strict(),
        "hangul_old_2set" to KoreanLayoutPresets.hangulOld2Set(),
        "hangul_old_3set_393" to KoreanLayoutPresets.hangul3Set393()
    ))

    export("$dirname/vietnamese", mapOf(
        "qwerty" to VietLayoutPresets.chuQuocNgu(LatinLayoutPresets.qwerty(), false),
        "qwerty_nom" to VietLayoutPresets.chuQuocNgu(LatinLayoutPresets.qwerty(), true)
    ))

    export("$dirname/symbol", mapOf(
        "number" to SymbolLayoutPresets.number(),
        "symbol_g" to SymbolLayoutPresets.symbolG(),
        "symbol_n1" to SymbolLayoutPresets.symbolN1(),
        "symbol_n2" to SymbolLayoutPresets.symbolN2(),
        "symbol_o1" to SymbolLayoutPresets.symbolO1(),
        "symbol_o2" to SymbolLayoutPresets.symbolO2(),
        "symbol_chinese" to SymbolLayoutPresets.symbolChinese()
    ))
}