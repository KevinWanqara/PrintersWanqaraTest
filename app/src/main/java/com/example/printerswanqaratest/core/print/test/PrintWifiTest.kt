package com.example.printerswanqaratest.core.print.test

import com.example.printerswanqaratest.core.print.EscposCoffee
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.output.TcpIpOutputStream


class PrintWifiTest(private var host: String, private var port: Int, private var fontType: String) {
    operator fun invoke() {
        TcpIpOutputStream(host, port).use { outputStream ->
            val style = Style()
            if (this.fontType == "A") {
                style.setFontName(Style.FontName.Font_A_Default)
            } else {
                style.setFontName(Style.FontName.Font_B)
            }
            val escposCoffee = EscposCoffee(style, outputStream)
            escposCoffee.printWifiTest(host,port,fontType)
        }
    }
}