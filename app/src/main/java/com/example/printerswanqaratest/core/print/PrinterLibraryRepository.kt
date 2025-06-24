package com.example.printerswanqaratest.core.print

import com.example.printerswanqaratest.core.print.messageBuilder.BodyBuilder
import com.example.printerswanqaratest.core.print.messageBuilder.MediaBuilder
import com.example.printerswanqaratest.core.print.messageBuilder.MessageBuilder
import com.example.printerswanqaratest.core.print.messageBuilder.TitleBuilder
import java.io.OutputStream

interface PrinterLibraryRepository {

    suspend fun print(messageBuilder: MessageBuilder)

    fun printTitle(titleBuilder: TitleBuilder)

    fun printBody(bodyBuilder: BodyBuilder)

    suspend fun printMedia(mediaBuilder: MediaBuilder)

    fun cut()

    fun printWifiTest(host: String, port: Int, fontType: String)
    fun printBluetoothTest(fontType: String)
    fun printUSBTest(fontType: String)

    fun openCashDrawer()
}