package com.example.printerswanqara.domain.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDomain>>
    val pairedDevices: StateFlow<List<BluetoothDomain>>
    val errors: SharedFlow<String>
    val isConnected: StateFlow<Boolean>
    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(device: BluetoothDomain): Flow<ConnectionResult>
    fun pairDevice(device: BluetoothDomain, onResult: (Boolean) -> Unit)

    fun updatePairedDevices()
    fun closeConnection()
    fun release()
}