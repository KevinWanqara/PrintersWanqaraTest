package com.printerswanqara.core.print.test

import com.printerswanqara.core.print.EscposCoffee
import com.github.anastaciocintra.escpos.Style
import java.net.InetSocketAddress
import java.net.Socket


class PrintWifiTest(private var host: String, private var port: Int, private var fontType: String) {
    operator fun invoke(): Boolean {
        return try {
            val targetHost = host.trim()
            if (targetHost.isEmpty() || port <= 0) {
                return false
            }

            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetHost, port), 3000)
                socket.soTimeout = 5000
                socket.tcpNoDelay = true
                socket.keepAlive = true

                socket.getOutputStream().use { outputStream ->
                    val style = Style()
                    if (this.fontType == "A") {
                        style.setFontName(Style.FontName.Font_A_Default)
                    } else {
                        style.setFontName(Style.FontName.Font_B)
                    }
                    val escposCoffee = EscposCoffee(style, outputStream)
                    escposCoffee.printWifiTest(targetHost, port, fontType)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("PrintWifiTest", "WiFi test print failed", e)
            false
        }
    }
}
