package ee.oyatl.ime.keyboard.popup

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.PopupPreviewBinding

class PreviewPopup(
    private val parent: View
): Popup {
    private val window: PopupWindow = PopupWindow(parent.context, null)
    private val binding = PopupPreviewBinding.inflate(LayoutInflater.from(
        ContextThemeWrapper(parent.context, R.style.Theme_FusionIME_Keyboard_Popup)))
    override val view: View get() = binding.root
    override val isShown: Boolean get() = window.isShowing

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

    var position: Pair<Int, Int> = 0 to 0

    override fun show() {
        window.setBackgroundDrawable(null)
        window.isTouchable = false
        window.contentView = view
        view.scaleY = 1f
        view.translationY = 0f
        val (x, y) = this.position
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

    override fun update() {
        val (x, y) = this.position
        window.dismiss()
        window.showAtLocation(parent, Gravity.TOP or Gravity.LEFT, x, y)
    }
}