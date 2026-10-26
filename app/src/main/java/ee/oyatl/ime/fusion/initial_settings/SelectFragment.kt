package ee.oyatl.ime.fusion.initial_settings

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import ee.oyatl.ime.fusion.databinding.FragmentInitialSettingsSelectBinding

class SelectFragment: Fragment(), ViewTreeObserver.OnWindowFocusChangeListener {
    lateinit var binding: FragmentInitialSettingsSelectBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInitialSettingsSelectBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.select.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        binding.next.setOnClickListener {
            activity?.finish()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        updateButtons()
    }

    override fun onResume() {
        super.onResume()
        updateButtons()
    }

    private fun updateButtons() {
        val selected = checkMethodSelected()
        binding.select.isEnabled = !selected
        binding.next.isEnabled = selected
    }

    private fun checkMethodSelected(): Boolean {
        val context = context ?: return false
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val found = imm.enabledInputMethodList.find { it.packageName == context.packageName }
        val currentImeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return found != null && found.id == currentImeId
    }
}