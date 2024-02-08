package com.example.electrostimulator.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.electrostimulator.bt.BLEConnection
import com.example.electrostimulator.bt.BLEManagerCallback
import com.example.electrostimulator.bt.BluetoothAdapterProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainViewModel(adapterProvider: BluetoothAdapterProvider) : ViewModel(), BLEManagerCallback {
    private val bleConnection: BLEConnection =
        BLEConnection(adapterProvider.getContext(), adapterProvider.getAdapter())

    var isConnected: Boolean = false
        get() = bleConnection.isConnected
        private set

    var isConnectionReady: MutableLiveData<Boolean> = MutableLiveData(false)
        private set

    var isConnectionLost: MutableLiveData<Boolean> = MutableLiveData(false)
        private set

    var deviceParams: MutableLiveData<StimulatorParams> = MutableLiveData()
        private set

    var paramsUpdated: MutableLiveData<Boolean> = MutableLiveData()
        private set

    init {
        bleConnection.registerObserver(this)
    }

    fun requestStimulatorParams() {
        bleConnection.readFromChar()
    }

    override fun onParamsReceived(byteArray: ByteArray) {
        val params: StimulatorParams = deserializeData(byteArray)
        deviceParams.postValue(params)
    }

    fun updateStimulatorParams(params: StimulatorParams) {
        val serializedParams = serializeData(params)
        deviceParams.postValue(params) // update params locally
        bleConnection.writeToChar(serializedParams)
    }

    override fun onParamsUpdated(byteArray: ByteArray) {
        val params: StimulatorParams = deserializeData(byteArray)
        Log.e("MainViewModel", "params: $params")
        Log.e("MainViewModel", "deviceParams.value: " + deviceParams.value.toString())

        if (params == deviceParams.value) {
            paramsUpdated.postValue(true)
        }
    }

    private fun serializeData(params: StimulatorParams): ByteArray {
        val byteBuffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(params.frequency)
        byteBuffer.putInt(params.duration)
        byteBuffer.put(if (params.explorationMode) 1.toByte() else 0.toByte())
        return byteBuffer.array()
    }

    private fun deserializeData(byteArray: ByteArray): StimulatorParams {
        val byteBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
        val frequency = byteBuffer.int
        val duration = byteBuffer.int
        val explorationMode = byteBuffer.get() != 0.toByte()
        return StimulatorParams(frequency, duration, explorationMode)
    }

    fun establishBleConnection(deviceAddress: String) {
        bleConnection.connect(deviceAddress)
    }

    override fun onConnectionReady() {
        isConnectionReady.postValue(true)
    }

    fun terminateBleConnection() {
        bleConnection.disconnect()
    }

    override fun onDisconnect() {
        isConnectionLost.postValue(true)
    }

    override fun onCleared() {
        super.onCleared()
        bleConnection.unregisterObserver(this)
    }
}

data class StimulatorParams(
    val frequency: Int,
    val duration: Int,
    val explorationMode: Boolean
)

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val adapterProvider: BluetoothAdapterProvider) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(adapterProvider) as T
        }
        throw IllegalArgumentException("ViewModel not found")
    }
}
