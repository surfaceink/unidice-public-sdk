package com.unidice.scanandcontrolexample.scanning

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unidice.scanandcontrolexample.ExampleApplication
import com.unidice.scanandcontrolexample.R
import com.unidice.scanandcontrolexample.scanning.ScanResultsAdapter
import com.unidice.scanandcontrolexample.databinding.FragmentScanBinding
import com.unidice.sdk.api.UnidiceBTManager
import com.unidice.sdk.api.UnidiceScanResult
import com.unidice.sdk.internal.UnidiceControllerBase

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null

    private val TAG = "ScanFragment"
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: ScanResultsAdapter
    private lateinit var btManager: UnidiceBTManager    // Scans, finds, and connects to Unidice

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentScanBinding.inflate(inflater, container, false)


        binding.scanButton.setOnClickListener {
            if(btManager.isScanning()) {
                onStopScan()
            } else if (unidiceController().isConnected()) {
                onDisconnect()
            } else {
                onScan()
            }
        }

        binding.scanList.layoutManager = LinearLayoutManager(context) //this@DevicesFragment)

        val listener = { scanResult: UnidiceScanResult ->
            Log.d("scan", "user has selected a Unidice to connect to")
            //viewModel.selectedBluetoothDevice.value = scanResult.device
            stopBleScan()
            unidiceController().handleUserSelectedADevice(scanResult.macAddress)
            configureUIBasedOnState()
        }

        adapter = ScanResultsAdapter(listener).apply {
            Log.d("DevicesFragment", "advertisement.   asking viewModel to stopScan()")
            setHasStableIds(true)
        }

        binding.scanList.adapter = adapter

        btManager = unidiceController().getBTManager()

        configureUIBasedOnState()

        return binding.root

    }

    fun configureUIBasedOnState() {
        if(btManager.isScanning()) {
            showUIForScanning()
        } else {
            showUIForNotScanning()
        }
    }

    fun onUnidiceScanResult(result: UnidiceScanResult) {
        //Log.d(“ScanCallback”, "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
        Log.d("ScanCallback", "Found BLE device! ")
        adapter.update(result)
    }

    fun onBatchScanResults(results: MutableList<UnidiceScanResult>) {
        if (results != null) {
            for(result in results) {
                adapter.update(result)
            }
        }
    }

    fun unidiceController() : UnidiceControllerBase {
        return (requireActivity().application as ExampleApplication).getUnidiceController()
    }

    fun startBleScan() {
        btManager.startScanning(::onUnidiceScanResult, ::onBatchScanResults)
    }

    fun stopBleScan() {
        btManager.stopScanning()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showUIForNotScanning() {
        if (unidiceController().isConnected()) {
            showUIForConnected()
        } else {
            binding.headline.text = "No Unidice Connercted"
            binding.headline.setTextColor(R.color.white)
            binding.scanButton.text = "Scan for Unidice.."
            binding.subhead.visibility = View.VISIBLE
        }
    }

    fun showUIForScanning() {
        binding.headline.text = "Scanning Devices..."
        binding.headline.setTextColor(R.color.white)
        binding.scanButton.text = "Stop Scanning"
        binding.subhead.visibility = View.GONE
    }

    fun showUIForConnected() {
        binding.headline.text = "Connected to your Unidice"
        binding.headline.setTextColor(R.color.white)
        binding.scanButton.text = "Disconnect"
        binding.subhead.visibility = View.GONE
    }

    fun onDisconnect() {
        unidiceController().disconnectFromUnidice()
        configureUIBasedOnState()
    }

    fun onStopScan() {
        stopBleScan()
        configureUIBasedOnState()
    }

    fun onScan() {
        if (ensurePermissions()) {
            startBleScan()
            configureUIBasedOnState()
        } else {
            Log.w(TAG, "ScanUI: User asked for Scan but we don't have permissions")
            showBluetoothPermissionsAlert()
        }
    }

    private fun showBluetoothPermissionsAlert() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.bleNotEnabled).setMessage(R.string.pleaseEnableBluetooth)
        builder.setPositiveButton(R.string.ok_choice) { _, _ -> }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
    // Permissions related functions

    private fun Context.hasPermission(
        permission: String
    ): Boolean = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private val hasBluetoothPermissions: Boolean
        get() {
            val c = requireContext()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (c.hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    c.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return true
                }
            } else {
                if (c.hasPermission(Manifest.permission.BLUETOOTH))
                    return true;
            }

            return false
        }

    private fun ensurePermissions() : Boolean {

        if (!hasLocationPermission) {
            Log.d("UnidiceFragment", "User does not have location permission")
            requestLocationPermission()
            return false
        }

        if (!hasBluetoothPermissions) {
            Log.d("UnidiceFragment", "User does not have bluetooth permission")
            requestBluetoothPermission()
            return false
        }

        return true
    }

    private object RequestCode {
        const val EnableBluetooth = 55001
        const val LocationPermission = 55002
    }

    private val OLD_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH
    )

    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private fun Fragment.requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(ANDROID_12_BLE_PERMISSIONS, RequestCode.EnableBluetooth)
        }
        else {
            requestPermissions(OLD_BLE_PERMISSIONS, RequestCode.EnableBluetooth)
        }
    }

    private fun Fragment.requestBluetoothScanPermission() {
        val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        requestPermissions(permissions, RequestCode.EnableBluetooth)
    }

    private fun Fragment.requestBluetoothConnectPermission() {
        val permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        requestPermissions(permissions, RequestCode.EnableBluetooth)
    }

    /**
     * Shows the native Android permission request dialog.
     *
     * The result of the dialog will come back via [Activity.onRequestPermissionsResult] method.
     */
    private fun Fragment.requestLocationPermission() {

        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M ) {
            Log.w(TAG, "ScanUI: requestLocationPermission, but we are <M.  So,sufficient location permissions")
            return
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            requestPermissions(permissions, RequestCode.LocationPermission)
        }
        else {
            // For android m, n,o, p, we need only the COARSE_LOCATION
            val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            requestPermissions(permissions, RequestCode.LocationPermission)
        }
    }

    private val hasLocationPermission: Boolean
        get() {
            val c = context ?: return false

            if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M ) {
                Log.i(TAG, "ScanUI: device is < Android.M.  Sufficient location permissions")
                return true
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (! c.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.i(TAG, "ScanUI: device is >= Android.Q.  But no permission for ACCESS_FINE_LOCATION")
                    return false
                }
            }
            else {
                // For android m, n,o, p, we need only the COARSE_LOCATION
                if (! c.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Log.i( TAG, "ScanUI: device is >= M and < Android.Q.  But no permission for COARSE_LOCATION")
                    return false
                }
            }

            return true
        }
}