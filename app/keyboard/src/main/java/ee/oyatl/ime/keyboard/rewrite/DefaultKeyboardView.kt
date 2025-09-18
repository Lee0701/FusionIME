package ee.oyatl.ime.keyboard.rewrite

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import androidx.annotation.RequiresPermission
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import kotlin.math.min

class DefaultKeyboardView(
    private val binding: KbdKeyboardBinding,
    private val keys: Set<KeyContainer>
): KeyboardViewManager {
    override val view: View get() = binding.root

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
    ): KeyboardListener {    private val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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
                        listener.onKeyUp(KeyEvent.KEYCODE_CAPS_LOCK)
                        listener.onKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                    }
                    KeyboardState.Shift.Pressed -> {
                        listener.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT)
                    }
                    KeyboardState.Shift.Locked -> {
                        listener.onKeyDown(KeyEvent.KEYCODE_CAPS_LOCK)
                    }
                }
            }
        private var shiftPressing: Boolean = false
        private var shiftTime: Long = 0
        private var inputWhileShifted: Boolean = false
        private var downTime: Long = 0

        override fun onKeyDown(code: Int) {
            if(params.vibrationDuration > 0) {
                vibrate(params.vibrationDuration)
            }
            if(params.soundVolume > 0f) {
                val fx = when(code) {
                    KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                    KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                    KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                    else -> AudioManager.FX_KEYPRESS_STANDARD
                }
                audioManager.playSoundEffect(fx, params.soundVolume)
            }
            downTime = System.currentTimeMillis()
            when(code) {
                KeyEvent.KEYCODE_DEL -> onDeletePressed(code)
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftPressed(code)
            }
        }

        override fun onKeyUp(code: Int) {
            when(code) {
                KeyEvent.KEYCODE_DEL -> onDeleteReleased(code)
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftReleased(code)
                else -> {
                    listener.onKeyDown(code)
                    listener.onKeyUp(code)
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
            listener.onKeyDown(code)
            listener.onKeyUp(code)
            handler.postDelayed({ repeat(code) }, params.repeatInterval.toLong())
        }

        private fun onDeletePressed(code: Int) {
            listener.onKeyDown(code)
            handler.postDelayed({ repeat(code) }, params.repeatDelay.toLong())
        }

        private fun onDeleteReleased(code: Int) {
            listener.onKeyUp(code)
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

    data class KeyContainer(
        val keyCode: Int,
        val binding: KbdKeyBinding
    )
}