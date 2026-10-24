package ee.oyatl.ime.fusion

import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.text.InputType

internal fun readClipboardCandidateText(
    clipboardManager: ClipboardManager,
    inputType: Int
): String? {
    if(isSensitiveInputType(inputType)) return null

    val description = try {
        clipboardManager.primaryClipDescription
    } catch(_: SecurityException) {
        return null
    } ?: return null

    if(isSensitiveClip(description)) return null
    val isText = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
        description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
    if(!isText) return null

    val clip = try {
        clipboardManager.primaryClip
    } catch(_: SecurityException) {
        return null
    } ?: return null
    if(clip.itemCount == 0) return null

    val text = clip.getItemAt(0).text?.toString() ?: return null
    return text.takeUnless { it.isBlank() }
}

internal fun isSensitiveInputType(inputType: Int): Boolean {
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    return when(inputClass) {
        InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}

private fun isSensitiveClip(description: ClipDescription): Boolean {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    return description.extras?.getBoolean(SENSITIVE_CLIP_EXTRA, false) == true
}

private const val SENSITIVE_CLIP_EXTRA = "android.content.extra.IS_SENSITIVE"
