package ee.oyatl.ime.theme

import androidx.annotation.DrawableRes
import ee.oyatl.ime.fusion.keyboard.core.SpecialKeyType
import ee.oyatl.ime.keyboard.R

object SpecialKeyIcon {
    @DrawableRes fun iconFor(specialKeyType: SpecialKeyType): Int? {
        return when(specialKeyType) {
            SpecialKeyType.LeftShift -> R.drawable.keyic_shift
            SpecialKeyType.RightShift -> R.drawable.keyic_shift
            SpecialKeyType.Language -> R.drawable.keyic_language
            SpecialKeyType.Symbol -> R.drawable.keyic_option
            SpecialKeyType.Numbers -> R.drawable.keyic_numbers
            SpecialKeyType.Return -> R.drawable.keyic_return
            SpecialKeyType.Delete -> R.drawable.keyic_delete
            SpecialKeyType.Space -> R.drawable.keyic_space
            SpecialKeyType.Left -> R.drawable.keyic_left
            SpecialKeyType.Right -> R.drawable.keyic_right
            else -> null
        }
    }
}