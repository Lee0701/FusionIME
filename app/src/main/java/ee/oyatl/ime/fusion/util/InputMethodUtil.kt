package ee.oyatl.ime.fusion.util

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object InputMethodUtil {
    fun checkMethodEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val found = imm.enabledInputMethodList.find { it.packageName == context.packageName }
        return found != null
    }

    fun checkMethodSelected(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val found = imm.enabledInputMethodList.find { it.packageName == context.packageName }
        val currentImeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return found != null && found.id == currentImeId
    }
}