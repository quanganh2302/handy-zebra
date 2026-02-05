package com.example.civn26t01.ui.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civn26t01.R
import com.example.civn26t01.databinding.FragmentDrawerBinding
import com.example.civn26t01.databinding.LayoutHeaderBinding
import com.example.civn26t01.ui.adapter.DrawerMenuAdapter

class DrawerFragment : Fragment(R.layout.fragment_drawer) {
    interface DrawerListener {
        fun onDrawerItemSelected(page: DrawerPage)
    }

    private var listener: DrawerListener? = null

    private var _binding : FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? DrawerListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = binding.recyclerDrawer

        val items = listOf(
            DrawerMenuItem(
                DrawerPage.HOME,
                R.drawable.ic_home,
                R.string.drawer_home
            ),
            DrawerMenuItem(
                DrawerPage.SETTINGS,
                R.drawable.ic_settings,
                R.string.drawer_settings
            ),
            DrawerMenuItem(
                DrawerPage.EXIT,
                R.drawable.ic_exit,
                R.string.drawer_exit
            )
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = DrawerMenuAdapter(items) {
            listener?.onDrawerItemSelected(it.page)
        }
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