package com.example.civn26t01.ui.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.civn26t01.R
import com.example.civn26t01.databinding.LayoutHeaderBinding
import com.example.civn26t01.ui.utils.LanguageManager

class HeaderFragment : Fragment(R.layout.layout_header) {
    interface HeaderListener {
        fun onMenuClicked()
        fun onLanguageChanged(lang: String)

    }
        private var listener: HeaderListener? = null
        private var _binding: LayoutHeaderBinding? = null
        private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? HeaderListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutHeaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Init language
        val currentLang = LanguageManager.getLanguage(requireContext())
        updateLanguageUI(currentLang)

        setupButton();
    }

    // MORE UX FOR UI
    private fun updateLanguageUI(lang: String) {
        when (lang) {
            "en" -> {
                binding.underlineEn.visibility = View.VISIBLE
                binding.underlineVi.visibility = View.GONE

                binding.showFlagEn.alpha = 1.0f
                binding.showFlagVi.alpha = 0.6f
            }
            "vi" -> {
                binding.underlineVi.visibility = View.VISIBLE
                binding.underlineEn.visibility = View.GONE

                binding.showFlagVi.alpha = 1.0f
                binding.showFlagEn.alpha = 0.6f
            }
        }
    }

    fun updateConnectionStatus(isConnected: Boolean) {
        if (_binding == null) return
        val colorRes = if (isConnected) R.color.green else R.color.red
        binding.connectionIndicator.setCardBackgroundColor(requireContext().getColor(colorRes))
    }

    private fun setupButton() {
        binding.btnMenu.setOnClickListener {
            listener?.onMenuClicked()
        }
        binding.showFlagEn.setOnClickListener {
            val lang = LanguageManager.getLanguage(requireContext())
            if (lang != "en") {
                listener?.onLanguageChanged("en")
            }
        }
        binding.showFlagVi.setOnClickListener {
            val lang = LanguageManager.getLanguage(requireContext())
            if (lang != "vi") {
                listener?.onLanguageChanged("vi")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLang = LanguageManager.getLanguage(requireContext())
        updateLanguageUI(currentLang)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

}

