package ee.oyatl.ime.fusion

import android.content.res.AssetManager
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetLoader
import java.io.InputStream

class AssetsPresetLoader(
    val assets: AssetManager,
    val dirname: String
): KeyboardLayoutPresetLoader() {
    override fun openInput(name: String): InputStream {
        return assets.open("$dirname/$name.yaml")
    }
}