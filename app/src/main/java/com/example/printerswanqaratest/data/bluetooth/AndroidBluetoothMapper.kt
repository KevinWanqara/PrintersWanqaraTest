package com.example.printerswanqaratest.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.printerswanqaratest.domain.models.BluetoothDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDomain(): BluetoothDomain {
    return BluetoothDomain(
        name = name,
        address = address
    )
}