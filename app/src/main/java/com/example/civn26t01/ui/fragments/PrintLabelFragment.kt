package com.example.civn26t01.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civn26t01.R
import com.example.civn26t01.core.constants.BundleKeys
import com.example.civn26t01.data.models.ChiyodaInfo
import com.example.civn26t01.data.remote.RetrofitClient
import com.example.civn26t01.databinding.FragmentPrintLabelBinding
import com.example.civn26t01.domain.models.Box
import com.example.civn26t01.ui.adapter.PackingAdapter
import com.example.civn26t01.ui.utils.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.abs

class PrintLabelFragment: Fragment() {
    companion object {
        private const val TAG = "PrintLabelFragment"
    }

    private var _binding: FragmentPrintLabelBinding? = null
    private val binding get() = _binding!!

    private val listBoxes = mutableListOf<Box>()
    private var qty: Double = 0.0 // Changed from Int to Double to support decimal quantities
    private var packingType: String = ""
    private var wono: String = ""
    private var entryDate: String = ""
    private var wonoComplete: Boolean = false
    private var printer: String = ""
    private lateinit var packingAdapter: PackingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrintLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Override soft input mode
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // Get arguments
        qty = arguments?.getDouble(BundleKeys.EXTRA_QTY) ?: 0.0 // Changed from getInt to getDouble
        packingType = arguments?.getString(BundleKeys.EXTRA_PACKING_TYPE, "").orEmpty()
        wono = arguments?.getString(BundleKeys.EXTRA_WONO, "").orEmpty()
        entryDate = arguments?.getString(BundleKeys.EXTRA_DATE, "").orEmpty()
        wonoComplete = arguments?.getBoolean(BundleKeys.EXTRA_WONO_COMPLETE, false) ?: false
        printer = arguments?.getString(BundleKeys.EXTRA_PRINTER_NAME, "").orEmpty()
        binding.edtNumberOfCompleted.setText(qty.toString())

        setupRecyclerView()
        updateListTitle()
        setupButtons()
        setupKeyboardHandling()
    }

    // ================= KEYBOARD HANDLING =================
    private fun setupKeyboardHandling() {
        binding.edtProductsPerBox.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.post {
                    binding.scrollView.smoothScrollTo(0, binding.formContainer.bottom)
                }
            }
        }

        binding.etBoxCount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.post {
                    binding.scrollView.smoothScrollTo(0, binding.formContainer.bottom)
                }
            }
        }
    }

    // ================= RECYCLER VIEW =================
    private fun setupRecyclerView() {
        packingAdapter = PackingAdapter(listBoxes) { position ->
            if (position in listBoxes.indices) {
                listBoxes.removeAt(position)
                packingAdapter.notifyItemRemoved(position)
                updateListTitle()
            }
        }

        binding.rvBoxes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packingAdapter
        }
    }

    private fun updateListTitle() {
        val totalBoxes = listBoxes.sumOf { it.boxCount }
        val totalProducts = listBoxes.sumOf { it.countInBox * it.boxCount } // Now summing Doubles correctly

        if (totalBoxes == 0) {
            binding.tvListTitle.setText(R.string.list_boxes)
        } else {
            binding.tvListTitle.text = getString(
                R.string.list_boxes_format,
                totalBoxes,
                packingType,
                totalProducts,
                getString(R.string.product)
            )
        }
    }

    // ================= HANDLE ADD BOX =================
    private fun addBox() {
        val productPerBox = binding.edtProductsPerBox.text.toString()
        val boxCountText = binding.etBoxCount.text.toString()

        if (productPerBox.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.enter_product_per_box))
            return
        }

        val productsPerBox = productPerBox.toDoubleOrNull() ?: 0.0 // Changed from toIntOrNull to toDoubleOrNull
        if (productsPerBox <= 0.0) return

        val boxCount = boxCountText.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val existingIndex = listBoxes.indexOfFirst { abs(it.countInBox - productsPerBox) < 0.0001 } // Added tolerance check for Double comparison

        if (existingIndex != -1) {
            listBoxes[existingIndex].boxCount += boxCount
            packingAdapter.notifyItemChanged(existingIndex)
        } else {
            val newBox = Box(countInBox = productsPerBox, boxCount = boxCount)
            listBoxes.add(newBox)
            packingAdapter.notifyItemInserted(listBoxes.size - 1)
            binding.rvBoxes.smoothScrollToPosition(listBoxes.size - 1)
        }

        binding.edtProductsPerBox.text?.clear()
        binding.etBoxCount.text?.clear()
        updateListTitle()
    }

    // ================= HANDLE AUTO DISTRIBUTE =================
    private fun handleAutoDistribute() {
        val completedCount = binding.edtNumberOfCompleted.text.toString().toDoubleOrNull() ?: 0.0 // Changed from toIntOrNull to toDoubleOrNull
        if (completedCount <= 0.0) {
            ToastManager.warning(requireContext(), getString(R.string.toast_invalid_completed_count))
            return
        }

        val packingQuantityText = binding.edtProductsPerBox.text.toString().trim()

        // Nếu không nhập số sp/thùng → mặc định gói tất cả vào 1 thùng
        val packingQuantity = if (packingQuantityText.isEmpty()) {
            Log.d(TAG, "handleAutoDistribute | no input → default 1 box with $completedCount items")
            completedCount
        } else {
            val parsed = packingQuantityText.toDoubleOrNull() // Changed from toIntOrNull to toDoubleOrNull
            if (parsed == null || parsed <= 0.0) {
                ToastManager.warning(requireContext(), getString(R.string.toast_invalid_packing_quantity))
                return
            }
            parsed
        }

        autoGenerateBoxes(completedCount, packingQuantity)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun autoGenerateBoxes(totalItems: Double, itemsPerBox: Double) { // Changed parameters to Double
        listBoxes.clear()

        val fullBoxes = (totalItems / itemsPerBox).toInt() // Calculate integer part of boxes
        val remainingItems = totalItems % itemsPerBox

        if (fullBoxes > 0) {
            listBoxes.add(Box(countInBox = itemsPerBox, boxCount = fullBoxes))
        }

        // Use tolerance for remaining items check
        if (remainingItems > 0.0001) {
            listBoxes.add(Box(countInBox = remainingItems, boxCount = 1))
        }

        packingAdapter.notifyDataSetChanged()
        updateListTitle()

        if (listBoxes.isNotEmpty()) {
            binding.rvBoxes.smoothScrollToPosition(listBoxes.size - 1)
        }

        binding.edtProductsPerBox.text?.clear()
        binding.etBoxCount.text?.clear()

        val message = if (remainingItems > 0.0001) {
            getString(R.string.toast_boxes_created_with_remaining, fullBoxes, packingType, itemsPerBox, remainingItems)
        } else {
            getString(R.string.toast_boxes_created_full, fullBoxes, packingType, itemsPerBox)
        }

        ToastManager.success(requireContext(), message)
    }

    // ================= PRINT LABELS - CALL API =================
    private fun printLabels() {
        // 1. Validate
        if (listBoxes.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.toast_no_boxes))
            return
        }

        // 2. Prepare data
        val completedCount = binding.edtNumberOfCompleted.text.toString().toDoubleOrNull() ?: 0.0 // Changed from toLongOrNull to toDoubleOrNull

        // Kiểm tra tổng sp trong thùng có khớp completedCount không
        val totalInBoxes = listBoxes.sumOf { it.countInBox * it.boxCount.toDouble() } // Calculations in Double
        
        // Use tolerance check for equality
        if (abs(totalInBoxes - completedCount) > 0.0001) {
            Log.w(TAG, "printLabels | MISMATCH: totalInBoxes=$totalInBoxes ≠ completedCount=$completedCount")
            ToastManager.warning(
                requireContext(),
                getString(R.string.toast_boxes_total_mismatch, totalInBoxes, completedCount)
            )
            return
        }
        val packingTypeCode = getPackingTypeCode(packingType)

        val chiyodaInfo = ChiyodaInfo(
            wono = wono,
            completedCount = completedCount,
            entryDate = entryDate,
            packingType = packingTypeCode,
            wonoComplete = wonoComplete,
            listBox = listBoxes.toList(),
            printer = printer
        )

        Log.d(TAG, "═══════════════════════════════════")
        Log.d(TAG, "📤 Submitting task to RPA:")
        Log.d(TAG, "WO No: ${chiyodaInfo.wono}")
        Log.d(TAG, "Completed Count: ${chiyodaInfo.completedCount}")
        Log.d(TAG, "Entry Date: ${chiyodaInfo.entryDate}")
        Log.d(TAG, "Packing Type: ${chiyodaInfo.packingType}")
        Log.d(TAG, "Printer: ${chiyodaInfo.printer}")
        Log.d(TAG, "Wono Status: ${chiyodaInfo.wonoComplete}")
        Log.d(TAG, "Total Boxes: ${listBoxes.size}")
        Log.d(TAG, "═══════════════════════════════════")

        // 3. Show loading
        showLoading(true)

        // 4. Call API
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val apiService = RetrofitClient.getApiService(requireContext())
                    val response = apiService.executeChiyoda(chiyodaInfo)

                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        throw HttpException(response)
                    }
                }

                // 5. Handle success
                showLoading(false)

                if (result?.success == true) {
                    Log.d(TAG, "✅ Task submitted successfully: JobId=${result.jobId}")

                    ToastManager.success(
                        requireContext(),
                        result.message ?: getString(R.string.print_success)
                    )

                    // Show success dialog
                    showSuccessDialog(result.jobId)
                } else {
                    Log.e(TAG, "❌ API returned success=false: ${result?.error}")

                    ToastManager.error(
                        requireContext(),
                        result?.message ?: getString(R.string.print_failed)
                    )
                }

            } catch (e: IOException) {
                // Network error
                showLoading(false)
                Log.e(TAG, "❌ Network error", e)

                ToastManager.error(
                    requireContext(),
                    getString(R.string.error_network)
                )

            } catch (e: HttpException) {
                // HTTP error
                showLoading(false)
                val errorCode = e.code()
                Log.e(TAG, "❌ HTTP error: $errorCode", e)

                val errorMessage = when (errorCode) {
                    400 -> getString(R.string.error_bad_request)
                    503 -> getString(R.string.error_system_paused)
                    else -> getString(R.string.error_server, errorCode)
                }

                ToastManager.error(requireContext(), errorMessage)

            } catch (e: Exception) {
                // Other errors
                showLoading(false)
                Log.e(TAG, "❌ Unexpected error", e)

                ToastManager.error(
                    requireContext(),
                    getString(R.string.error_unexpected, e.message)
                )
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnCreate.isEnabled = !show
        binding.btnCreate.text = if (show) {
            getString(R.string.processing)
        } else {
            getString(R.string.print_label)
        }
    }

    private fun showSuccessDialog(jobId: String?) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.success)
            .setMessage(getString(R.string.task_submitted_message, jobId ?: "N/A"))
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                clearDataAndNavigateBack()
            }
            .show()
    }

    private fun getPackingTypeCode(displayName: String): Int {
        val packingTypes = resources.getStringArray(R.array.packing_types)
        val index = packingTypes.indexOf(displayName)
        return if (index >= 0) index else 0
    }

    private fun clearDataAndNavigateBack() {
        // Gửi tín hiệu cho CreateMasterLabelFragment để clear form
        parentFragmentManager.setFragmentResult(
            BundleKeys.CLEAR_DATA_REQUEST,
            Bundle().apply {
                putBoolean(BundleKeys.SHOULD_CLEAR, true)
            }
        )

        // Pop toàn bộ back stack → quay về CreateMasterLabelFragment
        parentFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }
    private fun setupButtons() {
        binding.btnAdd.setOnClickListener { addBox() }
        binding.btnAuto.setOnClickListener { handleAutoDistribute() }
        binding.btnCreate.setOnClickListener { printLabels() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        @Suppress("DEPRECATION")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = null
    }
}