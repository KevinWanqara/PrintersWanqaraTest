package com.example.printerswanqara.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

object NetworkScanner {

    suspend fun scanLocalSubnet(port: Int, timeout: Int = 100): List<String> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext emptyList()
        val subnet = localIp.substringBeforeLast(".")
        
        val foundIps = mutableListOf<String>()
        
        (1..254).map { i ->
            async {
                val ip = "$subnet.$i"
                if (isPortOpen(ip, port, timeout)) {
                    ip
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull().toMutableList()
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress.contains(".")) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
