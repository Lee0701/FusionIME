package ee.oyatl.ime.keyboard.listener

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlin.math.min

open class FeedbackListener(
    context: Context,
    private val listener: KeyboardListener,
    private val soundVolume: Float = 1f,
    private val vibrationDuration: Long = 10
): KeyboardListener {
    @RequiresApi(Build.VERSION_CODES.S)
    private val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    private val vibrator =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) vibratorManager.defaultVibrator
        else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var downTime: Long = 0

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

    override fun onKeyDown(code: Int) {
        if(vibrationDuration > 0) {
            vibrate(vibrationDuration)
        }
        if(soundVolume > 0f) {
            val fx = when(code) {
                KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audioManager.playSoundEffect(fx, soundVolume)
        }
        downTime = System.currentTimeMillis()
        listener.onKeyDown(code)
    }

    override fun onKeyUp(code: Int) {
        listener.onKeyUp(code)
        val diff = System.currentTimeMillis() - downTime
        if(vibrationDuration > 0) {
            val duration = vibrationDuration / 5f * min(diff / 100f, 1f)
            vibrate(duration.toLong())
        }
    }

    class Repeatable(
        context: Context,
        private val listener: RepeatableKeyListener.Listener,
        soundVolume: Float = 1f,
        vibrationDuration: Long = 10,
        private val repeatVibrationDuration: Long = vibrationDuration / 2
    ): FeedbackListener(context, listener, soundVolume, vibrationDuration), RepeatableKeyListener.Listener {
        override fun onKeyRepeat(code: Int) {
            if(repeatVibrationDuration > 0) {
                vibrate(repeatVibrationDuration)
            }
            listener.onKeyRepeat(code)
        }
    }
}