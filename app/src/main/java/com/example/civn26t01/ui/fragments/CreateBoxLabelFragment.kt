package com.example.civn26t01.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.civn26t01.R
import com.example.civn26t01.core.constants.BundleKeys
import com.example.civn26t01.data.models.MasterLabelData
import com.example.civn26t01.databinding.FragmentCreateBoxLabelBinding
import com.example.civn26t01.ui.utils.DateUiFormatter
import java.time.LocalDate

class CreateBoxLabelFragment: Fragment() {
    companion object {
        private const val TAG = "CreateBoxLabelFragment"
    }

    private var _binding: FragmentCreateBoxLabelBinding? = null
    private val binding get() = _binding!!

    private var masterLabel : MasterLabelData? = null
    private var wonoComplete: Boolean = false
    private var selectedPackingType: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBoxLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLabelData()
        masterLabel?.let{bindMasterLabel(it)}
        setupDropdown()
        setupCheckbox()
        setupButtons()
    }

    private fun setupLabelData() {
        val isoDate = arguments?.getString(BundleKeys.EXTRA_DATE).orEmpty()
        wonoComplete = arguments?.getBoolean(BundleKeys.EXTRA_WONO_COMPLETE, false) ?: false
        masterLabel = MasterLabelData(
            wono = arguments?.getString(BundleKeys.EXTRA_WONO).orEmpty(),
            date = isoDate,
            qty = arguments?.getDouble(BundleKeys.EXTRA_QTY) ?: 0.0
        )
    }

    private fun bindMasterLabel(data: MasterLabelData) = with(binding) {
        edtWoNo.setText(data.wono)

        val formattedDate = runCatching {
            val localDate = LocalDate.parse(data.date)
            DateUiFormatter.format(localDate)
        }.getOrNull()

        dateInputView.setDate(formattedDate ?: data.date)
        edtQty.setText(data.qty.toString())

        cbCompleted.isChecked = wonoComplete
    }

    private fun setupDropdown() {
        val packingTypes = resources.getStringArray(R.array.packing_types)

        // Dismiss dropdown trước
        binding.actvCategory.dismissDropDown()

        // Clear adapter cũ
        binding.actvCategory.setAdapter(null)

        // Clear text
        binding.actvCategory.setText("", false)

        // Đợi một chút để UI update
        binding.actvCategory.post {
            // Tạo adapter mới
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                packingTypes
            )

            binding.actvCategory.setAdapter(adapter)

            if (packingTypes.isNotEmpty()) {
                val defaultIndex = 3 // "Kiện"
                val safeIndex = if (defaultIndex < packingTypes.size) defaultIndex else 0
                binding.actvCategory.setText(packingTypes[safeIndex], false)
                selectedPackingType = packingTypes[safeIndex]
            }
        }

        binding.actvCategory.setOnClickListener {
            binding.actvCategory.showDropDown()
        }

        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedPackingType = packingTypes[position]
        }
    }

    private fun setupCheckbox() {
        binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            wonoComplete = isChecked
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            createBoxLabel()
        }
    }

    private fun createBoxLabel() {
        val printerName = arguments?.getString(BundleKeys.EXTRA_PRINTER_NAME).orEmpty()
        val bundle = Bundle().apply {
            putString(BundleKeys.EXTRA_WONO, masterLabel?.wono)
            putString(BundleKeys.EXTRA_DATE, masterLabel?.date)
            putDouble(BundleKeys.EXTRA_QTY, masterLabel?.qty ?: 0.0) // Changed from putInt to putDouble
            putString(BundleKeys.EXTRA_PACKING_TYPE, selectedPackingType)
            putBoolean(BundleKeys.EXTRA_WONO_COMPLETE, wonoComplete)
            putString(BundleKeys.EXTRA_PRINTER_NAME, printerName)
        }

        val fragment = PrintLabelFragment()
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}