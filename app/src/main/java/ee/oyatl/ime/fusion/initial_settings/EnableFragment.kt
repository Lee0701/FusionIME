package ee.oyatl.ime.fusion.initial_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ee.oyatl.ime.fusion.FusionIMEService
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.FragmentInitialSettingsEnableBinding

class EnableFragment: Fragment() {
    lateinit var binding: FragmentInitialSettingsEnableBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInitialSettingsEnableBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.enable.setOnClickListener {
            context?.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        binding.next.setOnClickListener {
            findNavController().navigate(R.id.action_enable_to_select)
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtons()
    }

    private fun updateButtons() {
        val enabled = checkMethodEnabled()
        binding.enable.isEnabled = !enabled
        binding.next.isEnabled = enabled
    }

    private fun checkMethodEnabled(): Boolean {
        val context = context ?: return false
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val found = imm.enabledInputMethodList.find { it.packageName == context.packageName }
        return found != null
    }
}