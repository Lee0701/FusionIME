package ee.oyatl.ime.keyboard.listener

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class FeedbackListener(
    context: Context,
    private val listener: KeyboardListener,
    private val duration: Long = 10
): KeyboardListener {
    @RequiresApi(Build.VERSION_CODES.S)
    private val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    private val vibrator =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) vibratorManager.defaultVibrator
        else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrate(duration: Long) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(duration)
        }
    }

    override fun onKeyDown(code: Int) {
        vibrate(duration)
        listener.onKeyDown(code)
    }

    override fun onKeyUp(code: Int) {
        listener.onKeyUp(code)
    }
}