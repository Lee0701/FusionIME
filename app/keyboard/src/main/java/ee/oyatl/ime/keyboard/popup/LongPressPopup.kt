package ee.oyatl.ime.keyboard.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.AttrRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LongPressPopup(
    private val parent: View,
    key: KeyboardView.Key,
    private val candidates: List<Int>,
    keyPreviewPosition: Pair<Int, Int>
): SelectionPopup {
    private val themedContext = ContextThemeWrapper(
        parent.context,
        R.style.Theme_FusionIME_Keyboard_Popup
    )
    private val window = PopupWindow(themedContext)
    private val candidateViews = mutableListOf<AppCompatTextView>()
    private val itemWidth: Int
    private val popupX: Int
    private val popupY: Int
    private var selectedIndex = 0

    override val view = LinearLayout(themedContext).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    override val isShown: Boolean get() = window.isShowing
    override val selectedCodePoint: Int?
        get() = candidates.getOrNull(selectedIndex)

    init {
        val parentLocation = IntArray(2).also(parent::getLocationOnScreen)
        val availableWidth = parent.width.takeIf { it > 0 }
            ?: parent.resources.displayMetrics.widthPixels
        val popupHeight = max(key.rect.height(), dp(48))
        itemWidth = min(max(1, key.rect.width()), max(1, availableWidth / candidates.size))
        val popupWidth = itemWidth * candidates.size
        val parentLeft = parentLocation[0]
        val parentRight = parentLeft + availableWidth
        val maxPopupLeft = max(parentLeft, parentRight - popupWidth)
        val keyCenterX = keyPreviewPosition.first + key.rect.width() / 2
        val preferredAnchor = candidates.lastIndex / 2
        val anchorIndex = candidates.indices
            .filter { index ->
                val x = keyCenterX - index * itemWidth - itemWidth / 2
                x in parentLeft..maxPopupLeft
            }
            .minByOrNull { index -> abs(index - preferredAnchor) }
            ?: preferredAnchor
        val alignedX = keyCenterX - anchorIndex * itemWidth - itemWidth / 2
        popupX = alignedX.coerceIn(parentLeft, maxPopupLeft)
        selectedIndex = anchorIndex
        val keyTop = keyPreviewPosition.second + key.rect.height()
        popupY = keyTop - popupHeight

        view.background = roundedBackground(resolveColor(R.attr.backgroundColor))
        candidates.forEach { codePoint ->
            candidateViews += AppCompatTextView(themedContext).apply {
                text = String(Character.toChars(codePoint))
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTextColor(resolveColor(R.attr.foregroundColor))
                layoutParams = LinearLayout.LayoutParams(itemWidth, popupHeight)
                view.addView(this)
            }
        }
        updateSelection()

        window.contentView = view
        window.width = popupWidth
        window.height = popupHeight
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.isClippingEnabled = false
        window.isTouchable = false
        window.isOutsideTouchable = false
    }

    override fun selectAt(rawX: Int, rawY: Int) {
        if(candidates.isEmpty()) return
        val nextIndex = ((rawX - popupX) / itemWidth).coerceIn(candidates.indices)
        if(nextIndex != selectedIndex) {
            selectedIndex = nextIndex
            updateSelection()
        }
    }

    override fun show() {
        window.showAtLocation(parent, Gravity.TOP or Gravity.LEFT, popupX, popupY)
    }

    override fun hide() {
        window.dismiss()
    }

    override fun update() {
        if(window.isShowing) window.update(popupX, popupY, window.width, window.height)
    }

    private fun updateSelection() {
        val selectedColor = ColorUtils.setAlphaComponent(
            resolveColor(R.attr.hintTextColor),
            96
        )
        candidateViews.forEachIndexed { index, candidateView ->
            candidateView.background =
                if(index == selectedIndex) roundedBackground(selectedColor)
                else ColorDrawable(Color.TRANSPARENT)
        }
    }

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(6).toFloat()
        }
    }

    private fun resolveColor(@AttrRes attr: Int): Int {
        val value = TypedValue()
        check(themedContext.theme.resolveAttribute(attr, value, true))
        return if(value.resourceId != 0) {
            ContextCompat.getColor(themedContext, value.resourceId)
        } else {
            value.data
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            parent.resources.displayMetrics
        ).toInt()
    }
}
