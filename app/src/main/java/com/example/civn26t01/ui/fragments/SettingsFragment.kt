package com.example.civn26t01.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.civn26t01.R
import com.example.civn26t01.core.constants.BundleKeys
import com.example.civn26t01.core.settings.SettingsManager
import com.example.civn26t01.databinding.FragmentSettingsBinding
import com.example.civn26t01.helper.SettingsLiveData
import com.example.civn26t01.ui.utils.ToastManager

class SettingsFragment : Fragment() {
    companion object {
        private const val TAG = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentSettings()
        setupListeners()
        setupButtons()
        updateUrlPreview()
    }

    private fun loadCurrentSettings() {
        binding.edtDeviceName.setText(SettingsManager.getDeviceName(requireContext()))
        binding.edtServerIp.setText(SettingsManager.getServerIp(requireContext()))
        binding.edtServerPort.setText(SettingsManager.getServerPort(requireContext()))
        binding.switchHttps.isChecked = SettingsManager.getUseHttps(requireContext())

        // Load Printers
        val printers = SettingsManager.getPrinters(requireContext())
        if (printers.isNotEmpty()) binding.edtPrinter1.setText(printers[0])
        if (printers.size > 1) binding.edtPrinter2.setText(printers[1])
        if (printers.size > 2) binding.edtPrinter3.setText(printers[2])
    }

    private fun setupListeners() {
        // Real-time URL preview update
        binding.edtServerIp.doAfterTextChanged { updateUrlPreview() }
        binding.edtServerPort.doAfterTextChanged { updateUrlPreview() }
        binding.switchHttps.setOnCheckedChangeListener { _, _ -> updateUrlPreview() }
    }

    private fun updateUrlPreview() {
        val protocol = if (binding.switchHttps.isChecked) "https" else "http"
        val ip = binding.edtServerIp.text?.toString()?.trim().orEmpty()
        val port = binding.edtServerPort.text?.toString()?.trim().orEmpty()

        val url = if (ip.isNotEmpty() && port.isNotEmpty()) {
            "$protocol://$ip:$port"
        } else {
            getString(R.string.please_enter_ip_port)
        }

        binding.tvCurrentUrl.text = url
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnReset.setOnClickListener {
            resetToDefaults()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveSettings() {
        val deviceName = binding.edtDeviceName.text?.toString()?.trim().orEmpty()
        val ip = binding.edtServerIp.text?.toString()?.trim().orEmpty()
        val port = binding.edtServerPort.text?.toString()?.trim().orEmpty()
        
        val printer1 = binding.edtPrinter1.text?.toString()?.trim().orEmpty()
        val printer2 = binding.edtPrinter2.text?.toString()?.trim().orEmpty()
        val printer3 = binding.edtPrinter3.text?.toString()?.trim().orEmpty()

        // Validation
        if (deviceName.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.error_device_name_empty))
            binding.edtDeviceName.requestFocus()
            return
        }

        if (ip.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.error_server_ip_empty))
            binding.edtServerIp.requestFocus()
            return
        }

        if (port.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.error_server_port_empty))
            binding.edtServerPort.requestFocus()
            return
        }

        val portNumber = port.toIntOrNull()
        if (portNumber == null || portNumber !in 1..65535) {
            ToastManager.warning(requireContext(), getString(R.string.error_port_invalid))
            binding.edtServerPort.requestFocus()
            return
        }

        if (printer1.isEmpty() && printer2.isEmpty() && printer3.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.error_printer_1_empty))
            binding.edtPrinter1.requestFocus()
            return
        }

        val printers = listOf(printer1, printer2, printer3).filter { it.isNotEmpty() }

        // Save settings
        SettingsManager.setDeviceName(requireContext(), deviceName)
        SettingsManager.setServerIp(requireContext(), ip)
        SettingsManager.setServerPort(requireContext(), port)
        SettingsManager.setUseHttps(requireContext(), binding.switchHttps.isChecked)
        SettingsManager.setPrinters(requireContext(), printers)

        // Notify observers via LiveData
        SettingsLiveData.getInstance(requireContext()).refresh()

        Log.d(TAG, "Settings saved: $deviceName, $ip:$port, HTTPS: ${binding.switchHttps.isChecked}, Printers: $printers")
        ToastManager.success(requireContext(), getString(R.string.settings_saved_success))

        // Also send FragmentResult for backward compatibility
        parentFragmentManager.setFragmentResult(
            BundleKeys.SETTINGS_CHANGED,
            Bundle()
        )
    }

    private fun resetToDefaults() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.reset_settings_title)
            .setMessage(R.string.reset_settings_message)
            .setPositiveButton(R.string.reset) { dialog, _ ->
                SettingsManager.resetToDefaults(requireContext())
        
                // Reset Printers UI manually since resetToDefaults clears prefs but not UI immediately for printers
                binding.edtPrinter1.setText("")
                binding.edtPrinter2.setText("")
                binding.edtPrinter3.setText("")
                
                loadCurrentSettings()
                updateUrlPreview()

                // Notify observers
                SettingsLiveData.getInstance(requireContext()).refresh()

                ToastManager.success(requireContext(), getString(R.string.settings_reset_success))
                Log.d(TAG, "Settings reset to defaults")
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}