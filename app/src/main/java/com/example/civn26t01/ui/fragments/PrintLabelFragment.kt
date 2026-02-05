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

class PrintLabelFragment: Fragment() {
    companion object {
        private const val TAG = "PrintLabelFragment"
    }

    private var _binding: FragmentPrintLabelBinding? = null
    private val binding get() = _binding!!

    private val listBoxes = mutableListOf<Box>()
    private var qty: Int = 0
    private var packingType: String = ""
    private var wono: String = ""
    private var entryDate: String = ""
    private var wonoComplete: Boolean = false
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
        qty = arguments?.getInt(BundleKeys.EXTRA_QTY) ?: 0
        packingType = arguments?.getString(BundleKeys.EXTRA_PACKING_TYPE, "").orEmpty()
        wono = arguments?.getString(BundleKeys.EXTRA_WONO, "").orEmpty()
        entryDate = arguments?.getString(BundleKeys.EXTRA_DATE, "").orEmpty()
        wonoComplete = arguments?.getBoolean(BundleKeys.EXTRA_WONO_COMPLETE, false) ?: false
        binding.edtNumberOfCompleted.setText(qty.toString())

        setupRecyclerView()
        updateListTitle()
        setupButtons()
        setupKeyboardHandling()
        testApiConnection()
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
        val totalBoxes = listBoxes.sumOf { it.count }
        val totalProducts = listBoxes.sumOf { it.numberBox * it.count }

        if (totalBoxes == 0L) {
            binding.tvListTitle.setText(R.string.list_boxes)
        } else {
            binding.tvListTitle.text =
                getString(R.string.list_boxes) +
                        " $totalBoxes $packingType ($totalProducts ${getString(R.string.product)})"
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

        val productsPerBox = productPerBox.toIntOrNull() ?: 0
        if (productsPerBox <= 0) return

        val boxCount = boxCountText.toLongOrNull()?.takeIf { it > 0 } ?: 1
        val existingIndex = listBoxes.indexOfFirst { it.numberBox == productsPerBox }

        if (existingIndex != -1) {
            listBoxes[existingIndex].count += boxCount
            packingAdapter.notifyItemChanged(existingIndex)
        } else {
            val newBox = Box(numberBox = productsPerBox, count = boxCount)
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
        val completedCount = binding.edtNumberOfCompleted.text.toString().toIntOrNull() ?: 0
        if (completedCount <= 0) {
            ToastManager.warning(requireContext(), getString(R.string.toast_invalid_completed_count))
            return
        }

        val packingQuantityText = binding.edtProductsPerBox.text.toString()
        if (packingQuantityText.isEmpty()) {
            ToastManager.warning(requireContext(), getString(R.string.enter_product_per_box))
            return
        }

        val packingQuantity = packingQuantityText.toIntOrNull()
        if (packingQuantity == null || packingQuantity <= 0) {
            ToastManager.warning(requireContext(), getString(R.string.toast_invalid_packing_quantity))
            return
        }

        autoGenerateBoxes(completedCount, packingQuantity)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun autoGenerateBoxes(totalItems: Int, itemsPerBox: Int) {
        listBoxes.clear()

        val fullBoxes = totalItems / itemsPerBox
        val remainingItems = totalItems % itemsPerBox

        if (fullBoxes > 0) {
            listBoxes.add(Box(numberBox = itemsPerBox, count = fullBoxes.toLong()))
        }

        if (remainingItems > 0) {
            listBoxes.add(Box(numberBox = remainingItems, count = 1))
        }

        packingAdapter.notifyDataSetChanged()
        updateListTitle()

        if (listBoxes.isNotEmpty()) {
            binding.rvBoxes.smoothScrollToPosition(listBoxes.size - 1)
        }

        binding.edtProductsPerBox.text?.clear()
        binding.etBoxCount.text?.clear()

        val message = if (remainingItems > 0) {
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
        val completedCount = binding.edtNumberOfCompleted.text.toString().toLongOrNull() ?: 0L
        val packingTypeCode = getPackingTypeCode(packingType)

        val chiyodaInfo = ChiyodaInfo(
            wono = wono,
            completedCount = completedCount,
            entryDate = entryDate,
            packingType = packingTypeCode,
            wonoComplete = wonoComplete,
            listBox = listBoxes.toList()
        )

        Log.d(TAG, "═══════════════════════════════════")
        Log.d(TAG, "📤 Submitting task to RPA:")
        Log.d(TAG, "WO No: ${chiyodaInfo.wono}")
        Log.d(TAG, "Completed Count: ${chiyodaInfo.completedCount}")
        Log.d(TAG, "Entry Date: ${chiyodaInfo.entryDate}")
        Log.d(TAG, "Packing Type: ${chiyodaInfo.packingType}")
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
        parentFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }
    private fun testApiConnection() {
        lifecycleScope.launch {
            try {
                // 1. Test status endpoint
                val apiService = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) {
                    apiService.getRpaStatus()
                }

                if (response.isSuccessful) {
                    val status = response.body()
                    Log.d(TAG, "✅ API Connected")
                    Log.d(TAG, "Ready: ${status?.ready}")
                    Log.d(TAG, "Buffered: ${status?.bufferedCount}")

                    ToastManager.success(requireContext(), "API Connected!")
                } else {
                    Log.e(TAG, "❌ HTTP ${response.code()}")
                    ToastManager.error(requireContext(), "HTTP ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection failed", e)
                ToastManager.error(requireContext(), "Connection failed: ${e.message}")
            }
        }
    }
    private fun setupButtons() {
        binding.btnAdd.setOnClickListener { addBox() }
        binding.btnAuto.setOnClickListener { handleAutoDistribute() }
        binding.btnCreate.setOnClickListener { printLabels() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = null
    }
}