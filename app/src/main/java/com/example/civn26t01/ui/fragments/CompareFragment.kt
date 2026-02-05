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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civn26t01.R
import com.example.civn26t01.core.constants.BundleKeys
import com.example.civn26t01.data.MasterLabelData
import com.example.civn26t01.databinding.FragmentCompareLabelBinding
import com.example.civn26t01.data.PackingLabel
import com.example.civn26t01.core.scanner.ScanEvent
import com.example.civn26t01.core.scanner.ScanViewModel
import com.example.civn26t01.ui.adapter.RecyclerViewAdapter
import com.example.civn26t01.ui.utils.DateUiFormatter
import com.example.civn26t01.ui.utils.ToastManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.getValue
import kotlin.toString


class CompareFragment: Fragment() {
    companion object {
        private const val TAG = "CompareFragment"
    }
    private val scanViewModel: ScanViewModel by activityViewModels()

    private var _binding: FragmentCompareLabelBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecyclerViewAdapter

    private var masterLabel: MasterLabelData? = null
    private val scannedLabels = mutableListOf<PackingLabel>()
    private var isDialogShowing = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompareLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMasterLabelData()
        masterLabel?.let {binMasterLabel(it)}

        savedInstanceState?.getSerializable("SCANNED_LABELS")?.let {
        restored ->
            scannedLabels.clear()
            scannedLabels.addAll ( restored as List<PackingLabel>)
            adapter.notifyDataSetChanged()
            updateScannedLabelCount()
        }
        observeScanEvents()

        setupRecyclerView()
        setupButtons()
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
        Log.d(TAG, "Received scan event: $event")

        when (event) {
            is ScanEvent.Success -> handleScanSuccess(event.data)

            ScanEvent.Timeout ->
                ToastManager.info(requireContext(), getString(R.string.timeout_scan))

            ScanEvent.Alert ->
                ToastManager.info(requireContext(), getString(R.string.ocr_scan))

            is ScanEvent.Failed ->
                ToastManager.error(requireContext(), event.reason)

            ScanEvent.Canceled -> Unit
        }
    }

    private fun handleScanSuccess(rawData: String) {
        if (isDialogShowing) return
        Log.d(TAG, "HANDLE_SUCCESS | raw=$rawData")

        val packingLabel = PackingLabel.fromQrCodeData(rawData)
        if (packingLabel == null) {
            ToastManager.warning(requireContext(), getString(R.string.scan_not_packing))
            return
        }

        if (packingLabel.workOrderNo != masterLabel?.wono) {
            isDialogShowing = true
            ToastManager.warning(requireContext(), getString(R.string.not_match_wo_no))
            view?.postDelayed({ isDialogShowing = false }, 800)
            playWarningSound()
            return
        }

        if (packingLabel.date.toString() != masterLabel?.date) {
            isDialogShowing = true
            ToastManager.warning(requireContext(), getString(R.string.not_match_date))
            view?.postDelayed({ isDialogShowing = false }, 800)
            playWarningSound()
            return
        }

        addItemToList(packingLabel)
    }
    // ================= MASTER LABEL =================
    private fun setupMasterLabelData(){
        val isoDate = arguments?.getString(BundleKeys.EXTRA_DATE).orEmpty()

        masterLabel = MasterLabelData(
            wono = arguments?.getString(BundleKeys.EXTRA_WONO).orEmpty(),
            date = isoDate,
            qty = arguments?.getInt(BundleKeys.EXTRA_QTY) ?: 0
        )
    }
    private fun binMasterLabel(data: MasterLabelData)= with(binding){
        tvWoNoValue.text = data.wono
        val formattedDate = runCatching {
            val localDate = LocalDate.parse(data.date)
            DateUiFormatter.format(localDate)
        }.getOrNull()

        tvDateValue.text = formattedDate ?: data.date
        tvQtyValue.text = data.qty.toString()
    }

    // ================= RECYCLER VIEW =================

    private fun setupRecyclerView() {
        adapter = RecyclerViewAdapter(scannedLabels) { position ->
            if (position in scannedLabels.indices) {
                scannedLabels.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateScannedLabelCount()
            }
        }
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewItems.adapter = adapter
        updateScannedLabelCount()
    }
    private fun updateScannedLabelCount() {
        binding.bfRecyclerViewItems.text = getString(
            R.string.scanned_label_count_format,
            scannedLabels.size
        )
    }

    private fun addItemToList(label: PackingLabel) {
        val duplicated = scannedLabels.any {
            it.number == label.number && it.number != null
        }

        if (duplicated) {
            ToastManager.info(requireContext(), getString(R.string.label_already_scanned))
            return
        }

        val insertPosition = scannedLabels.size
        scannedLabels.add(label)
        adapter.notifyItemInserted(insertPosition)
        updateScannedLabelCount()
        binding.recyclerViewItems.scrollToPosition(insertPosition)
    }
    // ================= COMPARE =================
    private fun compareLabels() {
        if (scannedLabels.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.list_empty))
            return
        }

        val totalQuan = scannedLabels.sumOf { it.quantity ?: 0 }
        if (totalQuan == masterLabel?.qty) {
            ToastManager.success(requireContext(), getString(R.string.compare_success))
            showSuccessDialogWithOptions()
        } else {
            ToastManager.warning(requireContext(), getString(R.string.error_qty_invalid))
            playWarningSound()
        }
    }

    private fun showSuccessDialogWithOptions() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.success)
            .setMessage(R.string.compare_success)
            .setCancelable(false)
            .setPositiveButton(getText(R.string.create_chiyoda_label)) { dialog, _ ->
                dialog.dismiss()
                navigateToCreateBoxLabel()
            }
            .setNegativeButton(getText(R.string.back)) { dialog, _ ->
                dialog.dismiss()
                parentFragmentManager.setFragmentResult(
                    BundleKeys.CLEAR_DATA_REQUEST,
                    Bundle().apply {
                        putBoolean(BundleKeys.SHOULD_CLEAR, true)
                    }
                )
                parentFragmentManager.popBackStack()
            }
            .create()
        dialog.show()
    }
    private fun navigateToCreateBoxLabel() {
        val bundle = Bundle().apply {
            putString(BundleKeys.EXTRA_WONO, masterLabel?.wono)
            putString(BundleKeys.EXTRA_DATE, masterLabel?.date)
            putInt(BundleKeys.EXTRA_QTY, masterLabel?.qty ?: 0)
            putBoolean(BundleKeys.EXTRA_WONO_COMPLETE, false)
        }

        val fragment = CreateBoxLabelFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun playWarningSound(tone: Int = 15): Boolean {
        val onPeriod = 100
        val offPeriod = 100
        val repeatCount = 3

        return false
    }
    private fun setupButtons() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCompare.setOnClickListener {
            compareLabels()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(
            "SCANNED_LABELS",
            ArrayList(scannedLabels)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}