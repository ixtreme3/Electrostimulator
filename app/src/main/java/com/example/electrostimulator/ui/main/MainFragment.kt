package com.example.electrostimulator.ui.main

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.electrostimulator.App
import com.example.electrostimulator.R
import com.example.electrostimulator.databinding.FragmentMainBinding
import io.github.muddz.styleabletoast.StyleableToast

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val args: MainFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((requireActivity().application as App).adapterProvider)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.establishBleConnection(args.deviceAddress)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        setOnClickListeners()
        setEditTextListeners()
        subscribeToViewModel()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.isConnected && viewModel.isConnectionLost.value == false) {
            viewModel.terminateBleConnection()
            StyleableToast.makeText(
                requireContext(), "Соединение разорвано",
                Toast.LENGTH_LONG, R.style.red_toast
            ).show()
        }
    }

    private fun setEditTextListeners() {
        binding.durationValue.addTextChangedListener(createTextWatcher { currentDuration ->
            if (currentDuration?.isNotEmpty() == true &&
                currentDuration.toIntOrNull() != viewModel.deviceParams.value?.duration
            ) {
                binding.applyButton.backgroundTintList =
                    ColorStateList.valueOf(requireContext().getColor(R.color.fancy_yellow))
            }
        })

        binding.frequencyValue.addTextChangedListener(createTextWatcher { currentFrequency ->
            if (currentFrequency?.isNotEmpty() == true &&
                currentFrequency.toIntOrNull() != viewModel.deviceParams.value?.frequency
            ) {
                binding.applyButton.backgroundTintList =
                    ColorStateList.valueOf(requireContext().getColor(R.color.fancy_yellow))
            }
        })
    }

    private fun createTextWatcher(afterTextChanged: (String?) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                afterTextChanged(s?.toString())
            }
        }
    }

    private fun setOnClickListeners() {
        fun setOnClickListener(arrowView: View, valueView: EditText, increment: Int) {
            arrowView.setOnClickListener {
                if (valueView.text.toString().isNotBlank()) {
                    val newValue = valueView.text.toString().toInt() + increment
                    if (newValue >= 0) {
                        valueView.setText(newValue.toString())
                        binding.applyButton.backgroundTintList =
                            ColorStateList.valueOf(requireContext().getColor(R.color.fancy_yellow))
                    }
                }
            }
        }

        binding.apply {
            setOnClickListener(frequencyArrowUp, frequencyValue, 1)
            setOnClickListener(frequencyArrowDown, frequencyValue, -1)
            setOnClickListener(durationArrowUp, durationValue, 1)
            setOnClickListener(durationArrowDown, durationValue, -1)

            applyButton.setOnClickListener {
                val frequencyValueText = frequencyValue.text.toString()
                val durationValueText = durationValue.text.toString()

                if (frequencyValueText.isNotEmpty() && durationValueText.isNotEmpty()) {
                    val currentParams = StimulatorParams(
                        frequencyValueText.toInt(),
                        durationValueText.toInt(),
                        modeSwitch.isChecked
                    )
                    viewModel.updateStimulatorParams(currentParams)
                } else {
                    StyleableToast.makeText(
                        requireContext(),
                        "Заполните все поля",
                        Toast.LENGTH_SHORT,
                        R.style.red_toast
                    ).show()
                }
            }

            backButton.setOnClickListener {
                hideSpinner()
                parentFragmentManager.popBackStack()
            }

            modeSwitch.setOnClickListener {
                binding.applyButton.backgroundTintList =
                    ColorStateList.valueOf(requireContext().getColor(R.color.fancy_yellow))
            }

            resetButton.setOnClickListener {
                frequencyValue.setText("0")
                durationValue.setText("0")
                modeSwitch.isChecked = false
                applyButton.backgroundTintList =
                    ColorStateList.valueOf(requireContext().getColor(R.color.dark_gray))
            }
        }
    }

    private fun subscribeToViewModel() {
        viewModel.isConnectionReady.observe(viewLifecycleOwner) { isReady ->
            Log.e(TAG, "isConnectionReady: $isReady")
            if (isReady) {
                binding.connectionStateDot.setBackgroundResource(R.drawable.background_green_dot)
                viewModel.requestStimulatorParams()
            }
        }

        viewModel.isConnectionLost.observe(viewLifecycleOwner) { isLost ->
            if (isLost) {
                binding.connectionStateDot.setBackgroundResource(R.drawable.background_red_dot)
                StyleableToast.makeText(
                    requireContext(),
                    "Соединение потеряно",
                    Toast.LENGTH_LONG,
                    R.style.error_toast
                ).show()
                hideSpinner()
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.paramsUpdated.observe(viewLifecycleOwner) { areUpdated ->
            if (areUpdated) {
                viewModel.paramsUpdated.postValue(false)
                binding.applyButton.backgroundTintList =
                    ColorStateList.valueOf(requireContext().getColor(R.color.fancy_green))
                StyleableToast.makeText(
                    requireContext(),
                    "Изменения успешно внесены",
                    Toast.LENGTH_SHORT,
                    R.style.success_toast
                ).show()
            }
        }

        viewModel.deviceParams.observe(viewLifecycleOwner) { params ->
            binding.frequencyValue.setText(params.frequency.toString())
            binding.durationValue.setText(params.duration.toString())
            binding.modeSwitch.isChecked = params.explorationMode
            hideSpinner()
        }
    }

    private fun hideSpinner() {
        val progressBar = requireActivity().findViewById<ProgressBar>(R.id.spinner)
        progressBar.visibility = View.GONE
    }

    companion object {
        const val TAG = "MainFragment"
    }
}
