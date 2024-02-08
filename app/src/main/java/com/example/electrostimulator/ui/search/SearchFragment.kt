package com.example.electrostimulator.ui.search

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.electrostimulator.App
import com.example.electrostimulator.MainActivity
import com.example.electrostimulator.R
import com.example.electrostimulator.databinding.FragmentSearchBinding

class SearchFragment : Fragment(), SearchAdapter.Callback {

    private lateinit var binding: FragmentSearchBinding

    private val searchAdapter = SearchAdapter()

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory((requireActivity().application as App).adapterProvider)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.devicesRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        searchAdapter.addCallback(this)

        binding.startScanBtn.setOnClickListener {
            viewModel.isScanActive = !viewModel.isScanActive
            if (viewModel.isScanActive) {
                checkLocationAndStartScan.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                binding.startScanBtn.setBackgroundResource(R.drawable.background_round_button_green)
            } else {
                viewModel.stopScan()
                binding.startScanBtn.setBackgroundResource(R.drawable.background_round_button_gray)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        subscribeToViewModel()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopScan()
    }

    private fun subscribeToViewModel() {
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            searchAdapter.update(devices)
        }
    }

    override fun onItemClick(device: BtDeviceInfo) {
        showSpinner()
        val action =
            SearchFragmentDirections.actionSearchFragmentToMainFragment(device.deviceAddress)
        findNavController().navigate(action)
        viewModel.stopScan()
    }

    private fun showSpinner() {
        val activity = activity as MainActivity
        val progressBar = activity.findViewById<ProgressBar>(R.id.spinner)
        progressBar.visibility = View.VISIBLE
    }

    private val checkLocationAndStartScan = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startScan()
        }
    }
}