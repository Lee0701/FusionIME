package ee.oyatl.ime.keyboard.popup

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.PopupPreviewBinding

class PreviewPopup(
    keyboardView: KeyboardView,
    key: KeyboardView.Key,
): Popup {
    private val parent: View = keyboardView.view
    private val window: PopupWindow = PopupWindow(parent.context, null)
    private val binding = PopupPreviewBinding.inflate(LayoutInflater.from(
        ContextThemeWrapper(parent.context, R.style.Theme_FusionIME_Keyboard_Popup)))
    override val view: View get() = binding.root
    override val isShown: Boolean get() = window.isShowing

    var label: String = key.label
        set(value) {
            field = value
            update()
        }

    var position: Pair<Int, Int> =
        key.location[0] to keyboardView.rect.top - keyboardView.location[1] + key.location[1] - key.rect.height()
        set(value) {
            field = value
            update()
        }

    var size: Pair<Int, Int> = key.rect.width() to key.rect.height() * 2
        set(value) {
            field = value
            update()
        }

    init {
        update()
    }

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
        val (w, h) = size
        val (x, y) = position
        binding.label.text = label
        window.update(x, y, w, h)
    }
}