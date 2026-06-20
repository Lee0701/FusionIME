package ee.oyatl.ime.fusion

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import androidx.preference.PreferenceManager
import kotlin.math.roundToInt

object DimensionUtil {
    fun getOrientationSuffix(context: Context): String {
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val suffix = if(landscape) "_landscape" else "_portrait"
        return suffix
    }

    fun getOrientationInteger(context: Context, key: String): Float {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val suffix = getOrientationSuffix(context)
        @SuppressLint("DiscouragedApi")
        val defaultId = context.resources.getIdentifier("${key}${suffix}_default", "integer", context.packageName)
        val defaultValue = context.resources.getInteger(defaultId).toFloat()
        val value = preference.getFloat("${key}${suffix}", defaultValue)
        return value
    }

    fun getOrientationBoolean(context: Context, key: String): Boolean {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val suffix = getOrientationSuffix(context)
        @SuppressLint("DiscouragedApi")
        val defaultId = context.resources.getIdentifier("${key}${suffix}_default", "bool", context.packageName)
        val defaultValue = context.resources.getBoolean(defaultId)
        val value = preference.getBoolean("${key}${suffix}", defaultValue)
        return value
    }

    fun getKeyboardHeight(context: Context): Int {
        val rowHeightDIP = getOrientationInteger(context, "keyboard_height")
        val height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rowHeightDIP, context.resources.displayMetrics) * 4).roundToInt()
        return height
    }
}