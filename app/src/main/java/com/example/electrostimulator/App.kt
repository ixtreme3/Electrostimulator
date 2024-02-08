package com.example.electrostimulator

import android.app.Application
import com.example.electrostimulator.bt.BluetoothAdapterProvider

class App: Application() {
    val adapterProvider: BluetoothAdapterProvider by lazy {
        BluetoothAdapterProvider.Base(applicationContext)
    }
}