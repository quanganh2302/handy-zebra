package com.example.civn26t01.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.civn26t01.R
import com.example.civn26t01.core.settings.SettingsManager
import com.example.civn26t01.databinding.FragmentHomeBinding
import com.example.civn26t01.helper.SettingsLiveData

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDeviceName()
        setupButtons()
        observeSettings()
    }

    private fun setupDeviceName() {
        // Load device name lần đầu
        val deviceName = SettingsManager.getDeviceName(requireContext())
        binding.tvDeviceName.text = deviceName
    }

    private fun observeSettings() {
        // Auto update khi settings thay đổi
        SettingsLiveData.getInstance(requireContext()).observe(viewLifecycleOwner) { settings ->
            binding.tvDeviceName.text = settings.deviceName
        }
    }

    private fun setupButtons() {
        binding.btnKiemHang.setOnClickListener{
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateMasterLabelFragment()).commit()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}