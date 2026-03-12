package ee.oyatl.ime.fusion.preference

import android.content.Context
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import ee.oyatl.ime.fusion.databinding.DialogSelectKeystrokeBinding
import org.json.JSONException
import org.json.JSONObject

class KeyStrokePreference(
    context: Context,
    attrs: AttributeSet
): Preference(context, attrs) {
    private lateinit var binding: DialogSelectKeystrokeBinding
    var keyCode: Int = 0
        set(v) {
            field = v
            binding.keyName.setText(KeyEvent.keyCodeToString(keyCode))
        }

    override fun onClick() {
        val inflater = LayoutInflater.from(context)
        binding = DialogSelectKeystrokeBinding.inflate(inflater)

        val keyStroke = KeyStroke.parse(getPersistedString("")) ?: KeyStroke()
        keyCode = keyStroke.keyCode
        binding.keyShift.isChecked = keyStroke.shift
        binding.keyControl.isChecked = keyStroke.control
        binding.keyAlt.isChecked = keyStroke.alt
        binding.keyMeta.isChecked = keyStroke.meta

        binding.keyCode.addTextChangedListener { text ->
            keyCode = text?.toString()?.toIntOrNull() ?: return@addTextChangedListener
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(binding.root)
            .setOnKeyListener { _, code, event ->
                if(event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener true
                if(event.keyCode == KeyEvent.KEYCODE_DEL) binding.keyCode.text?.clear()
                keyCode = code
                binding.keyShift.isChecked = event.isShiftPressed
                binding.keyControl.isChecked = event.isCtrlPressed
                binding.keyAlt.isChecked = event.isAltPressed
                binding.keyMeta.isChecked = event.isMetaPressed
                true
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val keyStroke = KeyStroke(
                    keyCode = keyCode,
                    shift = binding.keyShift.isChecked,
                    control = binding.keyControl.isChecked,
                    alt = binding.keyAlt.isChecked,
                    meta = binding.keyMeta.isChecked
                )
                persistString(keyStroke.stringify())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
        dialog.show()
        super.onClick()
    }

    data class KeyStroke(
        val keyCode: Int = 0,
        val shift: Boolean = false,
        val control: Boolean = false,
        val alt: Boolean = false,
        val meta: Boolean = false
    ) {
        fun stringify(): String {
            val result = JSONObject()
            result.put("keyCode", keyCode)
            result.put("shift", shift)
            result.put("control", control)
            result.put("alt", alt)
            result.put("meta", meta)
            return result.toString()
        }

        fun matches(keyEvent: KeyEvent): Boolean {
            return keyEvent.keyCode == keyCode
                    && keyEvent.isShiftPressed == shift
                    && keyEvent.isCtrlPressed == control
                    && keyEvent.isAltPressed == alt
                    && keyEvent.isMetaPressed == meta
        }

        companion object {
            fun parse(json: String): KeyStroke? {
                try {
                    val obj = JSONObject(json)
                    return KeyStroke(
                        keyCode = obj.optInt("keyCode"),
                        shift = obj.optBoolean("shift"),
                        control = obj.optBoolean("control"),
                        alt = obj.optBoolean("alt"),
                        meta = obj.optBoolean("meta")
                    )
                } catch(_: JSONException) {
                    return null
                }
            }
        }
    }
}