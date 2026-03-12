package com.printerswanqara.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.printerswanqara.domain.models.BluetoothDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDomain(): BluetoothDomain {
    return BluetoothDomain(
        name = name,
        address = address
    )
}