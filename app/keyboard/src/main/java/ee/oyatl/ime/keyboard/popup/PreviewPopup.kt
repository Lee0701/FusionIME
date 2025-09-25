package ee.oyatl.ime.keyboard.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.PopupPreviewBinding

class PreviewPopup(context: Context): Popup {
    private val window: PopupWindow = PopupWindow(context, null)
    private val binding = PopupPreviewBinding.inflate(LayoutInflater.from(
        ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Popup)))
    override val view: View get() = binding.root

    var label: String
        get() = binding.label.text.toString()
        set(v) {
            binding.label.text = v
        }

    var size: Pair<Int, Int>
        get() = window.width to window.height
        set(v) {
            val (width, height) = v
            window.width = width
            window.height = height
        }

    override fun show(parent: View, x: Int, y: Int) {
        window.setBackgroundDrawable(null)
        window.isTouchable = false
        window.contentView = view
        window.showAtLocation(parent, Gravity.TOP or Gravity.LEFT, x, y)
    }

    override fun hide() {
        view.animate()
            .scaleY(0.8f)
            .translationY(view.height * 0.1f)
            .setDuration(100)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { window.dismiss() }
    }
}