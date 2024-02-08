package com.example.electrostimulator.ui.search

import android.annotation.SuppressLint
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.electrostimulator.bt.BluetoothAdapterProvider
import java.lang.IllegalArgumentException

class SearchViewModel(adapterProvider: BluetoothAdapterProvider) : ViewModel() {
    private val foundDevices = HashMap<String, BtDeviceInfo>()
    private val _devices: MutableLiveData<List<BtDeviceInfo>> = MutableLiveData()
    val devices: LiveData<List<BtDeviceInfo>>
        get() = _devices

    private val adapter = adapterProvider.getAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallback? = null
    private val settings: ScanSettings
    private val filters: List<ScanFilter>
    var isScanActive: Boolean = false

    init {
        settings = buildSettings()
        filters = buildFilter()
    }

    private fun buildSettings() =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

    private fun buildFilter() =
        listOf(
            ScanFilter.Builder()
                .setServiceUuid(FILTER_UUID)
                .build()
        )

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (callback == null) {
            callback = BleScanCallback()
            scanner = adapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (callback != null) {
            scanner?.stopScan(callback)
            scanner = null
            callback = null
            clearDevices()
        }
    }

    fun clearDevices() {
        _devices.postValue(emptyList())
        foundDevices.clear()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    inner class BleScanCallback : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                val deviceAddress = result.device.address
                if (!foundDevices.containsKey(deviceAddress)) {
                    val deviceName = result.scanRecord?.deviceName
                    foundDevices[deviceAddress] = BtDeviceInfo(deviceName, deviceAddress)
                    _devices.postValue(foundDevices.values.toList())
                }
                Log.i(TAG, "callbackType: CALLBACK_TYPE_ALL_MATCHES")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothScanner", "onScanFailed: scan error $errorCode")
        }
    }

    companion object {
        const val TAG = "DevicesViewModel"
        val FILTER_UUID: ParcelUuid = ParcelUuid.fromString("cfff0681-0b4d-4542-92bc-72aa9fb777d3")
    }
}

data class BtDeviceInfo(
    val deviceName: String?,
    val deviceAddress: String
)

class SearchViewModelFactory(private val adapterProvider: BluetoothAdapterProvider) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(adapterProvider) as T
        }
        throw IllegalArgumentException("View Model not found")
    }
}