package ee.oyatl.ime.fusion.initial_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.FragmentInitialSettingsWelcomeBinding

class WelcomeFragment: Fragment() {
    lateinit var binding: FragmentInitialSettingsWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInitialSettingsWelcomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.next.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_enable)
        }
    }
}