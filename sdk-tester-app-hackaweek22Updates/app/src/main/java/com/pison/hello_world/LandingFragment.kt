package com.pison.hello_world

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.pison.hello_world.databinding.LandingFragmentBinding

private const val TAG = "LANDING FRAGMENT"

class LandingFragment : Fragment() {

    private lateinit var viewModel: LandingViewModel
    private lateinit var binding: LandingFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.landing_fragment,
            container,
            false
        )
        viewModel = ViewModelProvider(this).get(LandingViewModel::class.java)

        binding.hapticsButton.isEnabled = false

        binding.hapticsButton.setOnClickListener {
            navigateToHapticsFragment()
        }

        viewModel.enableHapticsButton.observe(viewLifecycleOwner, { enableButton ->
            binding.hapticsButton.isEnabled = enableButton
        })

        viewModel.deviceConnected.observe(viewLifecycleOwner, { isDeviceConnected ->
            val connectedTextResId = if (isDeviceConnected) {
                R.string.device_status_connected
            } else {
                R.string.device_status_disconnected
            }
            clearViews()
            binding.deviceStatusText.text = getString(connectedTextResId)
        })

        viewModel.lockState.observe(viewLifecycleOwner, { lockState ->
            binding.lockStateText.text =
                if (lockState) getString(R.string.device_locked) else getString(R.string.device_unlocked)
        })

        viewModel.deviceStateBatteryReceived.observe(viewLifecycleOwner, { deviceState ->
            val batteryLevel = if (deviceState.battery < 0 || deviceState.battery > 100) {
                getString(R.string.battery_level_unknown)
            } else {
                getString(R.string.battery_level) + deviceState.battery.toString()
            }
            binding.batteryValueText.text = batteryLevel
        })

       /* viewModel.activationStates.observe(viewLifecycleOwner, { activationState ->
            if (activationState.index.name == "HOLD") {
                binding.activationStateCheckBox.isChecked = true
            } else if (activationState.index.name == "NONE") {
                binding.activationStateCheckBox.isChecked = false
            }
        })*/

        viewModel.gestureReceived.observe(viewLifecycleOwner, { gesture ->
            val gest = if (gesture == "DEBOUNCE_LDA_TEH") {
                "Let's Fing Go"
            } else{
                "dummy"
            }
            binding.detectedGestureVerdictText.text = gest
        })

        viewModel.eulerReceived.observe(viewLifecycleOwner, { euler ->

            binding.pitchValueText.text = getString(R.string.round).format(euler.pitch)
            binding.yawValueText.text = getString(R.string.round).format(euler.yaw)
            binding.rollValueText.text = getString(R.string.round).format(euler.roll)
        })

        viewModel.errorReceived.observe(viewLifecycleOwner, { error ->
            binding.errorText.text = error.message
        })

        viewModel.clearGesture.observe(viewLifecycleOwner, {
            clearGesture()
        })

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "ON START CALLED ")
        viewModel.connectToDevice()
    }
    override fun onStop() {
        Log.d(TAG, "ON STOP CALLED ")
        super.onStop()
        viewModel.disposeDisposables()
        clearViews()
    }

    private fun clearViews() {
        binding.detectedGestureVerdictText.text = getString(R.string.gesture_none)
        binding.pitchValueText.text = getString(R.string.euler_empty)
        binding.rollValueText.text = getString(R.string.euler_empty)
        binding.yawValueText.text = getString(R.string.euler_empty)
        //binding.activationStateCheckBox.isChecked = false
    }

    private fun clearGesture() {
        binding.detectedGestureVerdictText.text = getString(R.string.gesture_cleared)
    }

    private fun navigateToHapticsFragment() {
        if (view?.findNavController()?.currentDestination?.id == R.id.landingFragment) {
            view?.findNavController()
                ?.navigate(R.id.action_landingFragment_to_hapticsFragment)
        }
    }
}

