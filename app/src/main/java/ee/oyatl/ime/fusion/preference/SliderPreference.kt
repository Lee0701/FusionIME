package ee.oyatl.ime.fusion.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import ee.oyatl.ime.fusion.R

@SuppressLint("UseKtx")
class SliderPreference(
    context: Context,
    attrs: AttributeSet?,
): Preference(context, attrs) {

    private val valueFrom: Float
    private val valueTo: Float
    private val stepSize: Float

    private var valueSet: Boolean = false
    private var slider: Slider? = null

    var value: Float
        get() = getPersistedFloat(valueFrom)
        set(v) {
            persistFloat(v)
            notifyChanged()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference)
        valueFrom = a.getFloat(R.styleable.SliderPreference_valueFrom, 0f)
        valueTo = a.getFloat(R.styleable.SliderPreference_valueTo, 1f)
        stepSize = a.getFloat(R.styleable.SliderPreference_stepSize, 0f)
        a.recycle()

        layoutResource = R.layout.preference_multiline
        widgetLayoutResource = R.layout.preference_widget_frame
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetView = holder.itemView.findViewById<LinearLayout>(android.R.id.widget_frame)
        if(widgetView is ViewGroup) {
            widgetView.removeAllViews()
            val slider = LayoutInflater.from(context).inflate(R.layout.pref_slider_widget_content, null, false) as Slider
            try {
                slider.isEnabled = this.isEnabled
                slider.valueFrom = this.valueFrom
                slider.valueTo = this.valueTo
                slider.stepSize = this.stepSize
                slider.value = getPersistedFloat(valueFrom)
                slider.addOnChangeListener { _, value, _ ->
                    persistFloat(value)
                }
            } catch(ex: IllegalStateException) {
                ex.printStackTrace()
            }
            widgetView.addView(slider)
            this.slider = slider
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getFloat(index, valueFrom)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        setInitialValue(getPersistedFloat(defaultValue as? Float ?: valueFrom))
    }

    private fun setInitialValue(value: Float) {
        // Always persist/notify the first time.
        val changed = this.value != value
        if(changed || !this.valueSet) {
            this.valueSet = true
            this.value = value
            if(changed) {
                notifyChanged()
            }
        }
    }
}