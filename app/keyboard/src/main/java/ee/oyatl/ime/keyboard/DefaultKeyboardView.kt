package ee.oyatl.ime.keyboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.popup.Popup
import ee.oyatl.ime.keyboard.popup.PreviewPopup
import kotlin.math.min
import kotlin.math.roundToInt

class DefaultKeyboardView(
    private val binding: KbdKeyboardBinding,
    private val keys: Set<KeyContainer>,
    private val listener: Listener
): KeyboardViewManager {
    override val view: View get() = binding.root
    private val rect: Rect = Rect()
    private val location = IntArray(2)
    private val pointers: MutableMap<Int, Pointer> = mutableMapOf()

    init {
        view.viewTreeObserver.addOnGlobalLayoutListener {
            cacheKeys()
        }
        @SuppressLint("ClickableViewAccessibility")
        view.setOnTouchListener { view, event ->
            view.getGlobalVisibleRect(rect)
            val pointerId = event.getPointerId(event.actionIndex)
            val rawX =
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawX(event.actionIndex)
                else event.getX(event.actionIndex) + location[0]
            val rawY =
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawY(event.actionIndex)
                else event.getY(event.actionIndex) + location[1]
            val x = rawX.roundToInt() - location[0]
            val y = rawY.roundToInt() - location[1]

            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val key = findKey(x, y) ?: return@setOnTouchListener true
                    val popup =
                        if(listener.params.previewPopups && key.binding.label.text.isNotEmpty())
                            PreviewPopup(view.context)
                        else null
                    if(popup != null) {
                        popup.label = key.binding.label.text.toString()
                        popup.size = key.rect.width() to key.rect.height() * 2
                        val y = rect.top + key.location[1] - location[1] - key.rect.height()
                        popup.show(view, key.rect.left, y)
                    }
                    pointers += pointerId to Pointer(x, y, key, popup)
                    key.binding.root.isPressed = true
                    listener.onKeyDown(key.keyCode, 0)
                }
                MotionEvent.ACTION_MOVE -> {
                    val pointer = pointers[pointerId]
                    val key = pointer?.key ?: findKey(x, y) ?: return@setOnTouchListener true
                    if(!key.rect.contains(x, y)) key.binding.root.isPressed = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val pointer = pointers[pointerId]
                    val key = pointer?.key ?: findKey(x, y) ?: return@setOnTouchListener true
                    key.binding.root.isPressed = false
                    listener.onKeyUp(key.keyCode, 0)
                    pointer?.popup?.hide()
                    pointers -= pointerId
                }
            }
            true
        }
    }

    private fun cacheKeys() {
        val rect = Rect()
        view.getLocationOnScreen(location)
        view.getGlobalVisibleRect(rect)
        keys.forEach {
            it.binding.root.getGlobalVisibleRect(it.rect)
            it.binding.root.getLocationOnScreen(it.location)
            it.rect.offset(0, -rect.top)
        }
    }

    private fun findKey(x: Int, y: Int): KeyContainer? {
        return keys.find { key ->
            (x in key.rect.left until key.rect.right)
                    && (y in key.rect.top until key.rect.bottom)
        }
    }

    override fun setLabels(labels: Map<Int, String>) {
        keys.forEach {
            val label = labels[it.keyCode]
            if(label != null) it.binding.label.text = label
        }
    }

    override fun setIcons(icons: Map<Int, Int>) {
        keys.forEach {
            val icon = icons[it.keyCode]
            if(icon != null) it.binding.icon.setImageResource(icon)
        }
    }

    class Listener(
        context: Context,
        val listener: KeyboardListener,
        val params: KeyboardParams
    ): KeyboardListener {
        @RequiresApi(Build.VERSION_CODES.S)
        private val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        private val vibrator =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) vibratorManager.defaultVibrator
            else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        private val handler = Handler(Looper.getMainLooper())

        private var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released
            set(value) {
                field = value
                when(value) {
                    KeyboardState.Shift.Released -> {
                        listener.onKeyUp(KeyEvent.KEYCODE_CAPS_LOCK, metaState)
                        listener.onKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, metaState)
                    }
                    KeyboardState.Shift.Pressed -> {
                        listener.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, metaState)
                    }
                    KeyboardState.Shift.Locked -> {
                        listener.onKeyDown(KeyEvent.KEYCODE_CAPS_LOCK, metaState)
                    }
                }
            }
        private var shiftPressing: Boolean = false
        private var shiftTime: Long = 0
        private var inputWhileShifted: Boolean = false
        private var downTime: Long = 0

        private val metaState: Int get() = when(shiftState) {
            KeyboardState.Shift.Released -> 0
            KeyboardState.Shift.Pressed -> KeyEvent.META_SHIFT_ON
            KeyboardState.Shift.Locked -> KeyEvent.META_CAPS_LOCK_ON
        }

        override fun onKeyDown(keyCode: Int, metaState: Int) {
            if(params.vibrationDuration > 0) {
                vibrate(params.vibrationDuration)
            }
            if(params.soundVolume > 0f) {
                val fx = when(keyCode) {
                    KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                    KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                    KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                    else -> AudioManager.FX_KEYPRESS_STANDARD
                }
                audioManager.playSoundEffect(fx, params.soundVolume)
            }
            downTime = System.currentTimeMillis()
            when(keyCode) {
                KeyEvent.KEYCODE_DEL -> onDeletePressed(keyCode)
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftPressed(keyCode)
            }
        }

        override fun onKeyUp(keyCode: Int, metaState: Int) {
            when(keyCode) {
                KeyEvent.KEYCODE_DEL -> onDeleteReleased(keyCode)
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftReleased(keyCode)
                else -> {
                    listener.onKeyDown(keyCode, metaState)
                    listener.onKeyUp(keyCode, metaState)
                    autoReleaseShift()
                }
            }
            val diff = System.currentTimeMillis() - downTime
            if(params.vibrationDuration > 0) {
                val duration = params.vibrationDuration / 5f * min(diff / 100f, 1f)
                vibrate(duration.toLong())
            }
        }

        private fun repeat(code: Int) {
            listener.onKeyDown(code, metaState)
            listener.onKeyUp(code, metaState)
            handler.postDelayed({ repeat(code) }, params.repeatInterval.toLong())
        }

        private fun onDeletePressed(code: Int) {
            listener.onKeyDown(code, metaState)
            handler.postDelayed({ repeat(code) }, params.repeatDelay.toLong())
        }

        private fun onDeleteReleased(code: Int) {
            listener.onKeyUp(code, metaState)
            handler.removeCallbacksAndMessages(null)
        }

        private fun onShiftPressed(code: Int) {
            shiftPressing = true
            when(shiftState) {
                KeyboardState.Shift.Released -> {
                    shiftState = KeyboardState.Shift.Pressed
                }
                KeyboardState.Shift.Pressed -> {
                    if(params.shiftAutoRelease) {
                        val diff = System.currentTimeMillis() - shiftTime
                        if(diff < params.shiftLockDelay) shiftState = KeyboardState.Shift.Locked
                        else shiftState = KeyboardState.Shift.Released
                    } else {
                        shiftState = KeyboardState.Shift.Released
                    }
                }
                KeyboardState.Shift.Locked -> {
                    shiftState = KeyboardState.Shift.Released
                }
            }
        }

        private fun onShiftReleased(code: Int) {
            shiftPressing = false
            when(shiftState) {
                KeyboardState.Shift.Released -> {
                }
                KeyboardState.Shift.Pressed -> {
                    if(inputWhileShifted) shiftState = KeyboardState.Shift.Released
                    else shiftState = KeyboardState.Shift.Pressed
                }
                KeyboardState.Shift.Locked -> {
                }
            }
            shiftTime = System.currentTimeMillis()
            inputWhileShifted = false
        }

        private fun autoReleaseShift() {
            if(!params.shiftAutoRelease) return
            if(shiftState == KeyboardState.Shift.Pressed) {
                if(!shiftPressing) {
                    shiftState = KeyboardState.Shift.Released
                } else {
                    inputWhileShifted = true
                }
            }
        }

        @RequiresPermission(Manifest.permission.VIBRATE)
        fun vibrate(duration: Long) {
            if(duration == 0L) return
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                vibrator.vibrate(duration)
            }
        }
    }

    data class Pointer(
        val x: Int,
        val y: Int,
        val key: KeyContainer,
        val popup: Popup?
    )

    data class KeyContainer(
        val keyCode: Int,
        val binding: KbdKeyBinding
    ) {
        val rect: Rect = Rect()
        val location: IntArray = IntArray(2)
    }
}