package ee.oyatl.ime.keyboard.popup

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import ee.oyatl.ime.keyboard.DefaultKeyboardView
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.TouchMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MoreKeysPopup(
    keyboardView: KeyboardView,
    key: KeyboardView.Key,
    private val candidates: List<Int>,
    override val listener: SelectionPopup.Listener
): SelectionPopup {
    private val parent: View = keyboardView.view
    private val window = PopupWindow(parent.context)

    private val keyboardParams: KeyboardParams
    private val contentView: KeyboardView

    private val itemWidth: Int
    private val popupX: Int
    private val popupY: Int
    private val popupWidth: Int
    private val popupHeight: Int
    private var selectedIndex = 0

    override val view: View get() = contentView.view
    override val isShown: Boolean get() = window.isShowing

    init {
        val keyCenterX = key.rect.centerX()
        val keyTop = keyboardView.rect.top - keyboardView.location[1] + key.location[1]
        val availableWidth = parent.width.takeIf { it > 0 }
            ?: parent.resources.displayMetrics.widthPixels
        popupHeight = max(key.rect.height(), keyboardView.view.resources.getDimensionPixelSize(R.dimen.more_keys_popup_height))
        itemWidth = min(max(1, key.rect.width()), max(1, availableWidth / candidates.size))
        popupWidth = itemWidth * candidates.size
        val parentLeft = keyboardView.location[0]
        val parentRight = parentLeft + availableWidth
        val maxPopupLeft = max(parentLeft, parentRight - popupWidth)
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
        popupY = keyTop - popupHeight

        keyboardParams = KeyboardParams(
            screenMode = KeyboardState.ScreenMode.MoreKeys,
            height = popupHeight,
            splitWidth = 0,
            soundFeedback = false,
            hapticFeedback = false,
            vibrationDuration = 0L,
            touchMode = TouchMode.Seek,
            soundVolume = 0f,
            flickSensitivity = 0,
            longPressDelay = 0,
            shiftLockDelay = 0,
            shiftAutoRelease = false,
            repeatDelay = 0,
            repeatInterval = 0,
            previewPopups = false
        )
        contentView = DefaultKeyboardView(parent.context, null).also {
            it.keyboard = Keyboard(listOf(
                candidates.mapIndexed { i, candidate ->
                    Keyboard.KeyItem.Key(
                        i,
                        candidate.toChar().toString()
                    )
                }
            ), keyboardParams)
            it.labels = candidates
                .mapIndexed { i, codePoint -> i to KeyLabel.Default(codePoint.toChar().toString()) }
                .toMap()
        }

        window.contentView = view
        window.width = popupWidth
        window.height = popupHeight
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.isClippingEnabled = false
        window.isTouchable = false
        window.isOutsideTouchable = false
    }

    override fun selectAt(rawX: Int, rawY: Int) {
        candidates.indices.flatMap(contentView::findKeys).forEach { it.onReleased() }
        val x = (rawX - contentView.location[0]).coerceIn(0, popupWidth - 1)
        val y = (rawY - contentView.location[1]).coerceIn(0, popupHeight - 1)
        val key = contentView.findKey(x, y) ?: return
        key.onPressed()
        selectedIndex = key.keyCode
    }

    override fun show() {
        window.showAtLocation(parent, Gravity.TOP or Gravity.LEFT, popupX, popupY)
        contentView.findKeys(selectedIndex).firstOrNull()?.onPressed()
    }

    override fun hide() {
        window.dismiss()
        val codePoint = candidates.getOrNull(selectedIndex)
        if(codePoint != null) listener.onSelect(codePoint)
    }

    override fun update() {
        if(window.isShowing) window.update(popupX, popupY, window.width, window.height)
    }
}
