package com.example.printerswanqaratest.core.print.Imp

import android.graphics.Bitmap
import android.graphics.Color
import com.github.anastaciocintra.escpos.image.CoffeeImage


class BitmapCoffeeImage(private val image:Bitmap): CoffeeImage {
    override fun getHeight(): Int = image.height
    override fun getWidth(): Int = image.width

    override fun getSubimage(x: Int, y: Int, w: Int, h: Int): CoffeeImage =
        BitmapCoffeeImage(Bitmap.createBitmap(image, x, y, w, h))

    override fun getRGB(x: Int, y: Int): Int {
        val pixel = image.getPixel(x, y)
        return (Color.alpha(pixel) shl 24) or (Color.red(pixel) shl 16) or (Color.green(pixel) shl 8) or (Color.blue(
            pixel
        ) shl 0);
    }
}