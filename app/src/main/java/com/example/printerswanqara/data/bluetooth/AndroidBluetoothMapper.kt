package com.example.printerswanqara.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.printerswanqara.domain.models.BluetoothDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDomain(): BluetoothDomain {
    return BluetoothDomain(
        name = name,
        address = address
    )
}