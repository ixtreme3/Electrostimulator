package com.example.electrostimulator.bt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*

interface BLEManagerCallback {
    fun onConnect() {}
    fun onDisconnect() {}
    fun onConnectionReady() {}
    fun onConnectionFailed() {}
    fun onParamsReceived(byteArray: ByteArray) {}
    fun onParamsUpdated(byteArray: ByteArray) {}
}

@SuppressLint("MissingPermission")
class BLEConnection(private val context: Context, private val btAdapter: BluetoothAdapter) {
    private val observers: MutableList<BLEManagerCallback> = mutableListOf()
    private val observersLock = Any()
    private var currentGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isMtuSet: Boolean = false

    var isConnected: Boolean = false
        private set

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange: connection success")
                    isConnected = true
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "onConnectionStateChange: disconnection success")
                    disconnect()
                }
            } else {
                Log.e(TAG, "onConnectionStateChange: error connection $status")
                disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered: ${gatt.services.size}")
                isMtuSet = gatt.requestMtu(ESP_GATT_MAX_MTU_SIZE)
            } else {
                Log.e(TAG, "onServicesDiscovered disconnect()")
                disconnect()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            handler.postDelayed({ enableCharNotification() }, 100)
            notifyObserversOnConnectionReady()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // val data = bytesToString(value)
                Log.i(TAG, "onCharacteristicRead: ${value.toHexString()}")

                notifyObserversOnDataReceived(value)
            } else {
                Log.e(TAG, "onCharacteristicRead: error reading the char")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val value = characteristic?.value ?: return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // val data = bytesToString(value)
                Log.i(TAG, "onCharacteristicRead: ${value.toHexString()}")

                notifyObserversOnDataReceived(value)
            } else {
                Log.e(TAG, "onCharacteristicRead: error reading the char")
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.e(TAG, "onCharacteristicChanged: $value")
            /* отбрасываем инициализирующие пакеты */
            if (value.toHexString() == "000102030405060708090a0b0c0d0e") return

            if (characteristic.uuid.toString() == GATTS_READ_NOTIFY_DATA_UUID) {
                notifyObserversOnParamsChanged(value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val value = characteristic?.value ?: return

            /* отбрасываем инициализирующие пакеты */
            if (value.toHexString() == "000102030405060708090a0b0c0d0e") return

            if (characteristic.uuid.toString() == GATTS_READ_NOTIFY_DATA_UUID) {
                notifyObserversOnParamsChanged(value)
            }
        }
    }

    fun connect(address: String) {
        if (isConnected) {
            Log.e(TAG, "connect: Already connected to $address")
            return
        }

        val device: BluetoothDevice = btAdapter.getRemoteDevice(address)
        currentGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun enableCharNotification() {
        if (currentGatt == null) {
            Log.e(TAG, "enableNotification: gatt is null")
            return
        }
        val service = currentGatt?.getService(UUID.fromString(GATTS_SERVICE_UUID))
        if (service == null) {
            Log.e(TAG, "enableNotification: unable to get service")
            return
        }
        val characteristic = service.getCharacteristic(UUID.fromString(GATTS_READ_NOTIFY_DATA_UUID))
        if (characteristic == null) {
            Log.e(TAG, "enableNotification: unable to get characteristic")
            return
        }
        val descriptor = characteristic.getDescriptor(UUID.fromString(UUID_2902))
        if (descriptor == null) {
            Log.e(TAG, "enableNotification: unable to get descriptor")
            return
        }

        currentGatt?.setCharacteristicNotification(characteristic, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt?.writeDescriptor(
                descriptor,
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            )
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            currentGatt?.writeDescriptor(descriptor)
        }
    }

    fun readFromChar() {
        if (!isConnected) {
            Log.e(TAG, "readData: not connected")
            return
        }
        val chrResponse = currentGatt?.getService(UUID.fromString(GATTS_SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(GATTS_READ_NOTIFY_DATA_UUID))

        if (currentGatt == null) {
            Log.e(TAG, "readData: gatt is null")
        }
        if (chrResponse == null) {
            Log.e(TAG, "readData: characteristic not found")
        }
        currentGatt?.readCharacteristic(chrResponse)
    }

    fun writeToChar(data: ByteArray) {
        if (!isConnected) {
            Log.e(TAG, "writeToChar: not connected")
            return
        }

        val characteristic = currentGatt
            ?.getService(UUID.fromString(GATTS_SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(GATTS_WRITE_DATA_UUID))

        if (characteristic == null) {
            Log.e(TAG, "writeToChar: unable to get characteristic")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = currentGatt?.writeCharacteristic(
                characteristic, data,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
            if (status != BluetoothStatusCodes.SUCCESS) {
                Log.e(TAG, "writeToChar: write failed")
                return
            }
        } else {
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            val result = currentGatt?.writeCharacteristic(characteristic)
            if (result != true) {
                Log.e(TAG, "writeToChar: write failed")
                return
            }
        }
    }

    fun disconnect() {
        isConnected = false
        notifyObserversOnDisconnect()
        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null
    }


    fun registerObserver(observer: BLEManagerCallback) {
        synchronized(observersLock) {
            observers.add(observer)
        }
    }

    fun unregisterObserver(observer: BLEManagerCallback) {
        synchronized(observersLock) {
            observers.remove(observer)
        }
    }

    private fun notifyObserversOnConnect() {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onConnect()
            }
        }
    }

    private fun notifyObserversOnDisconnect() {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onDisconnect()
            }
        }
    }

    private fun notifyObserversOnConnectionReady() {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onConnectionReady()
            }
        }
    }

    private fun notifyObserversOnConnectionFailed() {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onConnectionFailed()
            }
        }
    }

    private fun notifyObserversOnDataReceived(data: ByteArray) {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onParamsReceived(data)
            }
        }
    }

    private fun notifyObserversOnParamsChanged(data: ByteArray) {
        synchronized(observersLock) {
            for (observer in observers) {
                observer.onParamsUpdated(data)
            }
        }
    }

    companion object {
        const val TAG = "BLEConnection"

        const val ESP_GATT_MAX_MTU_SIZE = 512

        const val GATTS_SERVICE_UUID = "cfff0681-0b4d-4542-92bc-72aa9fb777d3"
        const val GATTS_READ_NOTIFY_DATA_UUID = "59e9b7d5-e9f2-4e66-9693-49d58736181b"
        const val GATTS_WRITE_DATA_UUID = "c88c01a2-b45d-42b0-8173-272be8e72332"

        const val UUID_2902 = "00002902-0000-1000-8000-00805f9b34fb"
    }
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

fun bytesToString(bytes: ByteArray): String {
    val stringBuilder = StringBuilder(bytes.size)
    for (byte in bytes) {
        stringBuilder.append(String.format("%02X ", byte))
    }
    return stringBuilder.toString()
}