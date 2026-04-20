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
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.KeyboardParams
import kotlin.math.min

class KeyFeedbackManager(
    context: Context,
    val params: KeyboardParams
): KeyboardListener {
    @RequiresApi(Build.VERSION_CODES.S)
    private val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    @Suppress("DEPRECATION")
    private val vibrator =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) vibratorManager.defaultVibrator
        else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var downTime: Long = 0

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        downTime = System.currentTimeMillis()
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
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        val diff = System.currentTimeMillis() - downTime
        if(params.vibrationDuration > 0) {
            val duration = params.vibrationDuration / 5f * min(diff / 100f, 1f)
            vibrate(duration.toLong())
        }
    }

    override fun onReset() = Unit

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrate(duration: Long) {
        if(duration == 0L) return
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}