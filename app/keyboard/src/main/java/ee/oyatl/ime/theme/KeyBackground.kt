package ee.oyatl.ime.theme

import androidx.annotation.DrawableRes
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.keyboard.R

object KeyBackground {
    @DrawableRes fun forMerge(merge: KeyboardConfiguration.Item.TemplateKey.Merge? = null): Int {
        return when(merge) {
            KeyboardConfiguration.Item.TemplateKey.Merge.Up -> R.drawable.key_bg_bottom
            KeyboardConfiguration.Item.TemplateKey.Merge.Down -> R.drawable.key_bg_top
            KeyboardConfiguration.Item.TemplateKey.Merge.None -> R.drawable.key_bg
            else -> R.drawable.key_bg
        }
    }
}