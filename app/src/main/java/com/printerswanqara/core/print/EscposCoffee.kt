package com.printerswanqara.core.print

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.printerswanqara.core.print.Imp.BitmapCoffeeImage
import com.printerswanqara.core.print.messageBuilder.BodyBuilder
import com.printerswanqara.core.print.messageBuilder.MediaBuilder
import com.printerswanqara.core.print.messageBuilder.MessageBuilder
import com.printerswanqara.core.print.messageBuilder.TitleBuilder
import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.EscPosConst
import com.github.anastaciocintra.escpos.PrintModeStyle
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.escpos.barcode.BarCode
import com.github.anastaciocintra.escpos.barcode.QRCode
import com.github.anastaciocintra.escpos.image.Bitonal
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither
import com.github.anastaciocintra.escpos.image.EscPosImage
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.Arrays


class EscposCoffee : PrinterLibraryRepository {

    private var style: Style
    private val outputStream: OutputStream
    private val escPos: EscPos
    private var printerCharacters: Int = 32
    private var fontName: String = "A"

    constructor(style: Style, outputStream: OutputStream, printerCharacters: Int = 32, fontName: String = "A") {
        this.style = style
        this.outputStream = outputStream
        this.escPos = EscPos(outputStream)
        this.printerCharacters = printerCharacters
        this.fontName = fontName
    }

    fun setStyle(style: Style) {
        this.style = style
    }

    fun feed(line: Int) {
        this.escPos.feed(line)
    }

    override suspend fun print(messageBuilder: MessageBuilder) {
        try {
            this.printTitle(messageBuilder.getTitleMessage())
            this.printBody(messageBuilder.getBodyMessage())
            this.printMedia(messageBuilder.getMediaMessage())
            this.cut()
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun printTitle(titleBuilder: TitleBuilder) {
        try {
            titleBuilder.getTitleMessage().forEach { message ->
                this.escPos.write(style, message)
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun printBody(bodyBuilder: BodyBuilder) {
        try {
            bodyBuilder.getBodyMessage().forEach { message ->
                this.escPos.write(this.style, message)
            }
        } catch (e: Exception) {
            println(e)
        }
    }

     fun printMessage(message: String) {
        try {
            this.escPos.write(message)
//            this.escPos.write("\u001B" + "\u0042" + "\u0005" + "\u0002")

        } catch (e: Exception) {
            println(e)
        }
    }

    override suspend fun printMedia(mediaBuilder: MediaBuilder) {
        try {
            mediaBuilder.getMediaMessage().forEach { (key, message) ->
                if (key == "imgU") {
                    message.forEach { image ->
                        printImageFromUrl(this.escPos, image)
                    }
                }
                if (key == "BC") {
                    message.forEach { barCode ->
                        printBarCode(this.escPos, barCode)
                    }
                }
                if (key == "QR") {
                    message.forEach { qr ->
                        printQRCode(this.escPos, qr)
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun cut() {
        try {
            this.escPos.feed(5)
            this.escPos.cut(EscPos.CutMode.FULL)

        } catch (ex: IOException) {
            println(ex.message)
        }
    }

    override fun printWifiTest(host: String, port: Int, fontType: String) {
        try {
            val escpos = EscPos(this.outputStream)
            val title = Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
            val title1 = Style()
                .setLineSpacing(8)
                .setFontName(Style.FontName.Font_B)
            val characters = arrayOfNulls<String>(11)
            Arrays.fill(characters, "000000000-")
            val zeros = java.lang.String.join("", *characters)
            escpos.writeLF(title, "Test")
            val pms = PrintModeStyle().setFontSize(false, false)
            if (fontType == "A") {
                pms.setFontName(PrintModeStyle.FontName.Font_A_Default)
            } else {
                pms.setFontName(PrintModeStyle.FontName.Font_B)
            }
            escpos.feed(1)
            escpos.writeLF(pms, zeros)
            escpos.feed(1)
            escpos.writeLF(
                title1,
                "Cuente los caracteres que entran en la primera linea de texto y coloque el valor en Cantidad de caracteres"
            )
            escpos.feed(1)
            escpos.writeLF(
                Style().setJustification(EscPosConst.Justification.Center),
                "Configuracion Actual"
            )
            escpos.writeLF("IP de la impresora: " + host)
            escpos.writeLF("Puerto de la impresora: " + port)
            escpos.writeLF("Tipo de fuente: " + fontType)
            escpos.feed(2)
            escpos.feed(5)
            escpos.cut(EscPos.CutMode.FULL)

        } catch (e: Exception) {
            println(e)
        }
    }

    override fun printBluetoothTest(fontType: String) {
        try {
            val escpos = EscPos(this.outputStream)
            val title = Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
            val title1 = Style()
                .setLineSpacing(8)
                .setFontName(Style.FontName.Font_B)
            val characters = arrayOfNulls<String>(11)
            Arrays.fill(characters, "000000000-")
            val zeros = java.lang.String.join("", *characters)
            escpos.writeLF(title, "Test")
            val pms = PrintModeStyle().setFontSize(false, false)
            if (fontType == "A") {
                pms.setFontName(PrintModeStyle.FontName.Font_A_Default)
            } else {
                pms.setFontName(PrintModeStyle.FontName.Font_B)
            }
            escpos.feed(1)
            escpos.writeLF(pms, zeros)
            escpos.feed(1)
            escpos.writeLF(
                title1,
                "Cuente los caracteres que entran en la primera linea de texto y coloque el valor en Cantidad de caracteres"
            )
            escpos.feed(1)
            escpos.writeLF(
                Style().setJustification(EscPosConst.Justification.Center),
                "Configuracion Actual"
            )
            escpos.writeLF("Tipo de Impresora: bluetooth")
            escpos.writeLF("Tipo de fuente: " + fontType)
            escpos.feed(2)
            escpos.feed(5)
            escpos.cut(EscPos.CutMode.FULL)

        } catch (e: Exception) {
            println(e)
        }
    }
    override fun printUSBTest(fontType: String) {
        try {
            val escpos = EscPos(this.outputStream)
            val title = Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
            val title1 = Style()
                .setLineSpacing(8)
                .setFontName(Style.FontName.Font_B)
            val characters = arrayOfNulls<String>(11)
            Arrays.fill(characters, "000000000-")
            val zeros = java.lang.String.join("", *characters)
            escpos.writeLF(title, "Test")
            val pms = PrintModeStyle().setFontSize(false, false)
            if (fontType == "A") {
                pms.setFontName(PrintModeStyle.FontName.Font_A_Default)
            } else {
                pms.setFontName(PrintModeStyle.FontName.Font_B)
            }
            escpos.feed(1)
            escpos.writeLF(pms, zeros)
            escpos.feed(1)
            escpos.writeLF(
                title1,
                "Cuente los caracteres que entran en la primera linea de texto y coloque el valor en Cantidad de caracteres"
            )
            escpos.feed(1)
            escpos.writeLF(
                Style().setJustification(EscPosConst.Justification.Center),
                "Configuracion Actual"
            )
            escpos.writeLF("Tipo de Impresora: USB")
            escpos.writeLF("Tipo de fuente: " + fontType)
            escpos.feed(2)
            escpos.feed(5)
            escpos.cut(EscPos.CutMode.FULL)

        } catch (e: Exception) {
            println(e)
        }
    }

    override fun openCashDrawer() {
        try {
            this.escPos.write(27).write(112).write(0).write(25).write(250)
        } catch (e: java.lang.Exception) {
            println(e.message)
        }
    }

    fun closeStream() {
        this.escPos.close()
    }


    internal suspend fun printImageFromUrl(escpos: EscPos, imageUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(imageUrl).openStream()
                var image: Bitmap = BitmapFactory.decodeStream(inputStream)
                // Use 152 for logo width (divisible by 8)
                val logoWidth = 152
                // Calculate printer width based on characters (Font A is typically 12 dots wide)
                // e.g., 32 chars * 12 = 384 dots (58mm)

                // Determine multiplier based on font.
                // Font A: 12 dots. Font B: 9 dots.
                // Assuming "A" is default (12x24) and "B" is smaller (9x17)
                // If checking for "AA" (Double width/height A) or "BB" (Double width/height B),
                // the character count usually adjusts inversely, but the total width remains same.
                // The physical width in dots is constant for a printer (384 or 576).
                // But the 'printerCharacters' count provided by user is what fits in a line.
                // printerWidth = printerCharacters * charWidth

                val charWidth = if (fontName.equals("B", ignoreCase = true)) 9
                                else if (fontName.equals("A", ignoreCase = true)) 12
                                else 12 // Default to 12

                val printerWidth = this@EscposCoffee.printerCharacters * charWidth

// Crop to 1:1 aspect ratio (centered square)
                val size = minOf(image.width, image.height)
                val xOffset = (image.width - size) / 2
                val yOffset = (image.height - size) / 2
                image = Bitmap.createBitmap(image, xOffset, yOffset, size, size)

// Resize to logoWidth x logoWidth
                if (image.width != logoWidth || image.height != logoWidth) {
                    image = Bitmap.createScaledBitmap(image, logoWidth, logoWidth, true)
                }

                // Create a canvas with the full printer width and center the logo manually
                val finalBitmap = Bitmap.createBitmap(printerWidth, logoWidth, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(finalBitmap)
                canvas.drawColor(Color.WHITE)
                val leftPos = (printerWidth - logoWidth) / 2f
                canvas.drawBitmap(image, leftPos, 0f, null)


                val algorithm: Bitonal = BitonalOrderedDither()
                val imageWrapper = RasterBitImageWrapper()
                // Set justification to Left (default). Since the logo is manually centered in the bitmap,
                // and Left alignment is the default state that printers revert to, alignment persists across chunks.
                imageWrapper.setJustification(EscPosConst.Justification.Left_Default)
                val escposImage = EscPosImage(BitmapCoffeeImage(finalBitmap), algorithm)
                escpos.write(imageWrapper, escposImage)
                escpos.feed(1)

            } catch (e: Exception) {
                println(e)
            }
        }
    }

    private fun printQRCode(escpos: EscPos, qrCodeMessage: String) {
        try {
            val qrcode = QRCode()
            qrcode.setJustification(EscPosConst.Justification.Center)
            escpos.feed(1)
            escpos.write(qrcode, qrCodeMessage)
            escpos.feed(1)
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun printBarCode(escpos: EscPos, barCodeMessage: String) {
        try {
            val barcode = BarCode()
            escpos.feed(1)
            escpos.write(barcode, barCodeMessage)
            escpos.feed(1)
        } catch (e: Exception) {
            println(e)
        }
    }

}