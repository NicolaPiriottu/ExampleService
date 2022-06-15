package com.example.exampleservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.exampleservice.databinding.FragmentNoGpsBinding
import com.example.exampleservice.services.SpeedService
import com.example.exampleservice.viewmodels.MainViewModel

/**
 * Created by Nicola Luigi Piriottu
 */
class NoGpsFragment : Fragment() {

    /**
     * Binding
     */
    private lateinit var binding: FragmentNoGpsBinding

    /**
     * ViewModel
     **/
    private val viewModel: MainViewModel by viewModels()

    /**
     *  Broadcast for receiver intent to service
     */
    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {

                SpeedService.INTENT_CHECK_GPS -> {
                    viewModel.goToNoGpsFragment(
                        intent.getBooleanExtra(
                            SpeedService.CHECK_GPS,
                            false
                        )
                    )
                }

                SpeedService.INTENT_SET_CURRENT_POSITION -> {
                    viewModel.goToNoGpsFragment(isGPS = true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureReceiver()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate & Bind
        binding = FragmentNoGpsBinding.inflate(inflater, container, false)

        //Setup
        setupObserver()

        binding.fragmentNoGpsBtn.setOnClickListener {

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri =
                Uri.fromParts("package", activity?.packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        //stop Broadcast receiver
        try {
            requireActivity().unregisterReceiver(serviceBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupObserver() {
        // Use Case
        viewModel.useCaseLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { useCase ->

                when (useCase) {

                    is MainViewModel.UseCaseLiveData.GoToNoGpsFragment -> {
                        if (useCase.isGps) {
                            findNavController().navigateUp()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Configure Broadcast receiver
     */
    private fun configureReceiver() {
        val filter = IntentFilter()
        //Filter
        filter.addAction(SpeedService.INTENT_ON_STATE_SERVICE)
        filter.addAction(SpeedService.INTENT_CHECK_GPS)
        filter.addAction(SpeedService.INTENT_SET_CURRENT_POSITION)

        //register
        requireActivity().registerReceiver(serviceBroadcastReceiver, filter)
    }

}