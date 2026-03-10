package com.example.civn26t01.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.civn26t01.R
import com.example.civn26t01.core.constants.BundleKeys
import com.example.civn26t01.data.models.MasterLabelData
import com.example.civn26t01.databinding.FragmentCreateMasterLabelBinding
import com.example.civn26t01.core.scanner.ScanEvent
import com.example.civn26t01.core.scanner.ScanViewModel
import com.example.civn26t01.ui.custom.DateInputView
import com.example.civn26t01.ui.utils.ToastManager
import com.example.civn26t01.domain.validation.MasterLabelValid
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.civn26t01.core.settings.SettingsManager
import android.widget.ArrayAdapter

class CreateMasterLabelFragment: Fragment(R.layout.fragment_create_master_label) {
    companion object {
        private const val TAG = "CreateMasterLabelFragment"
    }

    private var _binding: FragmentCreateMasterLabelBinding? = null
    private val binding get() = _binding!!

    // Get shared ViewModel from Activity
    private val scanViewModel: ScanViewModel by activityViewModels()

    private lateinit var dateInputView: DateInputView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateMasterLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateInputView = binding.dateInputView

        setupButtons()
        observeScanEvents()
        arguments?.getString(BundleKeys.EXTRA_WONO)?.let {
            binding.edtWoNo.setText(it)
        }

        parentFragmentManager.setFragmentResultListener(
            BundleKeys.CLEAR_DATA_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(BundleKeys.SHOULD_CLEAR, false)) {
                clearAllInputFields()
            }
        }
        
        setupPrinterDropdown()
    }

    private fun setupPrinterDropdown() {
        val printers = SettingsManager.getPrinters(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, printers)
        binding.tvPrinter.setAdapter(adapter)

        // Select the first printer by default
        if (printers.isNotEmpty()) {
            binding.tvPrinter.setText(printers[0], false)
        }
    }

    // ================= SCAN OBSERVER =================

    private fun observeScanEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scanViewModel.scanEvents.collect { event ->
                    handleScanEvent(event)
                }
            }
        }
    }

    private fun handleScanEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.Success -> {
                Log.d(TAG, "Scan success: ${event.data}, type: ${event.codeType}")
                val sanitized = sanitizeScanData(event.data)
                if (MasterLabelValid.isValid(sanitized)) {
                    binding.edtWoNo.setText(sanitized)
                    ToastManager.success(
                        requireContext(),
                        getString(R.string.success)
                    )
                } else {
                    ToastManager.info(
                        requireContext(),
                        getString(R.string.scan_not_master)
                    )
                }
            }

            ScanEvent.Timeout -> {
                Log.w(TAG, "Scan timeout")
                ToastManager.info(requireContext(), getString(R.string.timeout_scan))
            }

            ScanEvent.Alert -> {
                Log.w(TAG, "Scan alert")
                ToastManager.info(requireContext(), getString(R.string.ocr_scan))
            }

            is ScanEvent.Failed -> {
                Log.e(TAG, "Scan failed: ${event.reason}")
                ToastManager.error(requireContext(), event.reason)
            }

            ScanEvent.Canceled -> {
                Log.d(TAG, "Scan canceled")
            }
        }
    }

    // ================= CREATE MASTER LABEL =================

    private fun createMasterLabel() {
        val wono = binding.edtWoNo.text?.toString()?.trim().orEmpty()
        val qtyText = binding.edtQty.text?.toString()?.trim().orEmpty()
        val dateText = dateInputView.getDate().trim()

        if (wono.isEmpty()) {
            ToastManager.info(requireContext(), getString(R.string.validate_wono))
            return
        }

        val qty = qtyText.toIntOrNull()
        if (qty == null || qty <= 0) {
            ToastManager.info(requireContext(), getString(R.string.error_qty_invalid))
            return
        }

        if (dateText.isEmpty()) {
            ToastManager.info(requireContext(), getString(R.string.error_date_empty))
            return
        }
        
        val selectedPrinter = binding.tvPrinter.text.toString()
        if (selectedPrinter.isEmpty()) {
            ToastManager.info(requireContext(), "Please select a printer") // Or add to strings.xml later if needed. Use generic for now.
            return
        }

        val localDate = runCatching {
            LocalDate.parse(
                dateText,
                DateTimeFormatter
                    .ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
            )
        }.getOrNull()

        if (localDate == null) {
            ToastManager.error(requireContext(), getString(R.string.error_date_invalid))
            return
        }

        val isoDate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val masterLabel = MasterLabelData(
            wono = wono,
            date = isoDate,
            qty = qty
        )

        Log.d(TAG, "Create master label: $masterLabel")
        goToCompare(masterLabel)
    }

    private fun goToCompare(master: MasterLabelData) {
        val fragment = CompareFragment().apply {
            arguments = Bundle().apply {
                putString(BundleKeys.EXTRA_WONO, master.wono)
                putString(BundleKeys.EXTRA_DATE, master.date)
                putInt(BundleKeys.EXTRA_QTY, master.qty)
                putString(BundleKeys.EXTRA_PRINTER_NAME, binding.tvPrinter.text.toString())
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ================= UTIL =================

    private fun clearAllInputFields() {
        binding.edtWoNo.setText("")
        binding.edtQty.setText("")
        dateInputView.clearDate()
    }

    private fun sanitizeScanData(data: String): String {
        return data.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "")
            .take(50)
    }

    private fun setupButtons() {
        binding.btnCreate.setOnClickListener {
            createMasterLabel()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}