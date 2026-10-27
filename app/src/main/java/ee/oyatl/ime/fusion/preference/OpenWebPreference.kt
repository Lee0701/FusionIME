package ee.oyatl.ime.fusion.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference
import ee.oyatl.ime.fusion.R

@SuppressLint("UseKtx")
class OpenWebPreference(
    context: Context,
    attrs: AttributeSet
): Preference(context, attrs) {
    val url: String

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OpenWebPreference)
        url = a.getString(R.styleable.OpenWebPreference_url).orEmpty()
        a.recycle()
    }

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(this.url.toUri())
        context.startActivity(intent)
    }
}