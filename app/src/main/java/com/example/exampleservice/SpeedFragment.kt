package com.example.exampleservice

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.exampleservice.databinding.FragmentSpeedBinding
import com.example.exampleservice.services.SpeedService
import com.example.exampleservice.viewmodels.MainViewModel

/**
 * Created by Nicola Luigi Piriottu
 */
class SpeedFragment : Fragment() {

    /**
     * Binding
     */
    private lateinit var binding: FragmentSpeedBinding

    /**
     * ViewModel
     **/
    private val viewModel: MainViewModel by viewModels()

    /**
     *  Location Permission
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Log.d("Niko", "Ti trovi nel primo controllo ACCESS_FINE_LOCATION")

                //Check background permission
                setOnStartService(
                    permissions.getOrDefault(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        false
                    )
                )
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                Log.d("Niko", "Ti trovi nel secondo controllo ACCESS_COARSE_LOCATION")
                //Check background permission
                setOnStartService(
                    permissions.getOrDefault(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        false
                    )
                )
            }
            else -> {
                // No location access granted.
                Log.d("Niko", "Non hai i permessi")
                showDialog()
            }
        }
    }

    /**
     *  Broadcast for receiver intent to service
     */
    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                SpeedService.INTENT_ON_STATE_SERVICE -> {

                    viewModel.isRunService(
                        intent.getBooleanExtra(
                            SpeedService.ON_STATE_SERVICE,
                            false
                        )
                    )
                }
                SpeedService.INTENT_CHECK_GPS -> {
                    viewModel.goToNoGpsFragment(
                        intent.getBooleanExtra(
                            SpeedService.CHECK_GPS,
                            false
                        )
                    )
                }
                SpeedService.INTENT_SET_CURRENT_POSITION -> {

                    viewModel.showSpeed(intent.getParcelableExtra<Location>(SpeedService.SET_CURRENT_POSITION))

                }
            }
        }
    }

    private var isStartService: Boolean = false

    /**
     *  Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Precise location access granted.
            Log.d("Niko", "Ti trovi ad aver accettato i permessi ad avvio app")

            //start Service receiver
            configureReceiver()

            //Check background permission
            setOnStartService(
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )

        } else {
            //POrta il bottone in stato di attivazione ( o cambia fragment)
            //goToSettings()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate & Bind
        binding = FragmentSpeedBinding.inflate(inflater, container, false)

        //Setup
        setupView()
        setupObserver()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateTextBtn(isStartService)
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

    private fun setupView() {
        binding.fragmentStartStopBtn.setOnClickListener {

            if (!isStartService) {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                setOnStopService()
            }
        }
    }

    private fun setupObserver() {
        // Use Case
        viewModel.useCaseLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { useCase ->

                when (useCase) {
                    is MainViewModel.UseCaseLiveData.ShowSpeed -> {
                        binding.fragmentSpeedTv.text = useCase.speed
                        isStartService = true
                    }
                    is MainViewModel.UseCaseLiveData.IsRunService -> {

                        isStartService = useCase.isRunService
                        //Set button test
                        binding.fragmentStartStopBtn.setText(

                            if (!isStartService) {
                                R.string.speed_start_button_fragment
                            } else {
                                R.string.speed_stop_button_fragment
                            }
                        )
                    }

                    is MainViewModel.UseCaseLiveData.GoToNoGpsFragment -> {
                        if (!useCase.isGps) {
                            findNavController().navigate(SpeedFragmentDirections.toNoGpsFragment())
                        }
                        isStartService = false
                    }
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

    /**
     * Service
     */
    private fun setOnService(action: SpeedService.ActionsType) {
        Intent(activity, SpeedService::class.java).also {

            it.action = action.name
            //Starting the service in >=26 Mode from a BroadcastReceiver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(it)
            } else {
                requireActivity().startService(it)
            }
        }
    }

    private fun setOnStartService(isBackgroundPermission: Boolean) {
        if (isBackgroundPermission) {
            Log.d("Niko", "hai accesso al background")
            setOnService(SpeedService.ActionsType.START)
        } else {
            Log.d("Niko", " NON hai accesso al background")
            showDialog()
        }
    }

    private fun setOnStopService() {
        setOnService(SpeedService.ActionsType.STOP)
    }

    private fun goToSettings() {
        //Richiede i permessi
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri =
            Uri.fromParts("package", activity?.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(R.string.dialog_active_background_permission_message)
            .setPositiveButton(R.string.dialog_active_background_permission_positive_button,
                DialogInterface.OnClickListener { _, _ ->
                    goToSettings()
                })
            .setNegativeButton(R.string.dialog_active_background_permission_negative_button,
                DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                })
        // Create the AlertDialog object and return it
        builder.create().show()
    }

    private fun updateTextBtn(isStart: Boolean) {
        binding.fragmentStartStopBtn.setText(

            if (!isStart) {
                R.string.speed_start_button_fragment
            } else {
                R.string.speed_stop_button_fragment
            }
        )
    }
}