package ee.oyatl.ime.fusion.setup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.FragmentSetupSelectBinding
import ee.oyatl.ime.fusion.settings.InputModeSettingsFragment
import ee.oyatl.ime.fusion.util.InputMethodUtil
import org.json.JSONArray

class SelectFragment: Fragment(), ViewTreeObserver.OnWindowFocusChangeListener {
    lateinit var binding: FragmentSetupSelectBinding
    lateinit var preference: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupSelectBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preference = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val inputModeExists = JSONArray(preference.getString(InputModeSettingsFragment.PREF_KEY, "[]")).length() > 0
        binding.next.setText(
            if(inputModeExists) R.string.setup_wizard_finish
            else R.string.setup_wizard_next
        )

        binding.select.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        binding.next.setOnClickListener {
            if(inputModeExists) activity?.finish()
            else findNavController().navigate(R.id.action_select_to_languages)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(this)
    }

    override fun onDestroyView() {
        view?.viewTreeObserver?.removeOnWindowFocusChangeListener(this)
        super.onDestroyView()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        updateButtons()
    }

    override fun onResume() {
        super.onResume()
        updateButtons()
    }

    private fun updateButtons() {
        val selected = InputMethodUtil.checkMethodSelected(context ?: return)
        binding.select.isEnabled = !selected
        binding.next.isEnabled = selected
    }
}