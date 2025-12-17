package org.onedroid.resizephoto.core.algorithm

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

object LanczosResizer {
    private const val A = 3

    private fun sinc(x: Double): Double {
        if (x == 0.0) return 1.0
        val p = Math.PI * x
        return sin(p) / p
    }

    private fun kernel(x: Double): Double {
        val ax = abs(x)
        if (ax >= A) return 0.0
        return sinc(x) * sinc(x / A)
    }

    fun resize(src: Bitmap, dstW: Int, dstH: Int): Bitmap {
        val tmp = resizeHorizontal(src, dstW)
        val final = resizeVertical(tmp, dstH)
        if (!tmp.isRecycled) tmp.recycle()
        return final
    }

    private fun resizeHorizontal(src: Bitmap, newW: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        val input = IntArray(srcW * srcH)
        src.getPixels(input, 0, srcW, 0, 0, srcW, srcH)

        val output = IntArray(newW * srcH)
        val scale = srcW.toDouble() / newW.toDouble()

        for (y in 0 until srcH) {
            val srcOffset = y * srcW
            val dstOffset = y * newW

            for (x in 0 until newW) {
                val srcX = (x + 0.5) * scale - 0.5
                val left = floor(srcX - A + 1).toInt()
                val right = floor(srcX + A).toInt()

                var wSum = 0.0
                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0

                for (i in left..right) {
                    val clamped = i.coerceIn(0, srcW - 1)
                    val w = kernel(srcX - i)
                    val c = input[srcOffset + clamped]

                    wSum += w
                    rSum += Color.red(c) * w
                    gSum += Color.green(c) * w
                    bSum += Color.blue(c) * w
                }

                val r = (rSum / wSum).roundToInt().coerceIn(0, 255)
                val g = (gSum / wSum).roundToInt().coerceIn(0, 255)
                val b = (bSum / wSum).roundToInt().coerceIn(0, 255)

                output[dstOffset + x] = Color.rgb(r, g, b)
            }
        }

        return createBitmap(newW, srcH).apply {
            setPixels(output, 0, newW, 0, 0, newW, srcH)
        }
    }

    private fun resizeVertical(src: Bitmap, newH: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        val input = IntArray(srcW * srcH)
        src.getPixels(input, 0, srcW, 0, 0, srcW, srcH)

        val output = IntArray(srcW * newH)
        val scale = srcH.toDouble() / newH.toDouble()

        for (x in 0 until srcW) {
            for (y in 0 until newH) {
                val srcY = (y + 0.5) * scale - 0.5
                val top = floor(srcY - A + 1).toInt()
                val bottom = floor(srcY + A).toInt()

                var wSum = 0.0
                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0

                for (i in top..bottom) {
                    val clamped = i.coerceIn(0, srcH - 1)
                    val w = kernel(srcY - i)
                    val c = input[clamped * srcW + x]

                    wSum += w
                    rSum += Color.red(c) * w
                    gSum += Color.green(c) * w
                    bSum += Color.blue(c) * w
                }

                val idx = y * srcW + x
                val r = (rSum / wSum).roundToInt().coerceIn(0, 255)
                val g = (gSum / wSum).roundToInt().coerceIn(0, 255)
                val b = (bSum / wSum).roundToInt().coerceIn(0, 255)

                output[idx] = Color.rgb(r, g, b)
            }
        }

        return createBitmap(srcW, newH).apply {
            setPixels(output, 0, srcW, 0, 0, srcW, newH)
        }
    }
}
