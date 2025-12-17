package org.onedroid.resizephoto.core.algorithm

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-quality image resizer using Lanczos-3 algorithm
 * Best for downscaling images while preserving sharpness
 *
 * Usage:
 * val resizer = LanczosResizer()
 * val resizedBitmap = resizer.resize(originalBitmap, 800, 600)
 *
 * Developed by Tawhid Monowar
 */


class LanczosResizer {

    companion object {
        private const val A = 3 // Lanczos-3
        private const val KERNEL_TAPS = 2 * A // support is [-A, A]
    }

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

    private data class Weights(
        val left: IntArray,
        val weight: Array<FloatArray>, // [dstIndex][tap]
        val wSum: FloatArray            // [dstIndex]
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Weights

            if (!left.contentEquals(other.left)) return false
            if (!weight.contentDeepEquals(other.weight)) return false
            if (!wSum.contentEquals(other.wSum)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = left.contentHashCode()
            result = 31 * result + weight.contentDeepHashCode()
            result = 31 * result + wSum.contentHashCode()
            return result
        }
    }

    private fun precomputeWeights(srcSize: Int, dstSize: Int): Weights {
        val scale = srcSize.toDouble() / dstSize.toDouble()
        val left = IntArray(dstSize)
        val weight = Array(dstSize) { FloatArray(KERNEL_TAPS + 1) } // inclusive taps
        val wSum = FloatArray(dstSize)

        for (d in 0 until dstSize) {
            val srcX = (d + 0.5) * scale - 0.5
            val l = floor(srcX - A + 1).toInt()
            left[d] = l

            var sum = 0.0
            for (t in 0..KERNEL_TAPS) {
                val i = l + t
                val w = kernel(srcX - i)
                val wf = w.toFloat()
                weight[d][t] = wf
                sum += w
            }
            // Avoid divide-by-zero (shouldn’t happen, but safe)
            wSum[d] = if (sum == 0.0) 1f else sum.toFloat()
        }
        return Weights(left, weight, wSum)
    }

    /**
     * Memory-optimized two-pass Lanczos resize.
     * - Horizontal: stream rows (no full-frame IntArray).
     * - Vertical: ring-buffer only needed rows.
     */
    fun resize(src: Bitmap, dstW: Int, dstH: Int): Bitmap {
        require(dstW > 0 && dstH > 0) { "Target dimensions must be positive" }
        if (src.width == dstW && src.height == dstH) {
            // If you truly need a new instance:
            // return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
            // But for memory, return original:
            return src
        }

        val tmp = resizeHorizontalStream(src, dstW) // creates new bitmap if width differs
        val out = resizeVerticalRing(tmp, dstH)     // creates new bitmap if height differs

        // Only recycle tmp if tmp was created by us and not reused as out
        if (tmp !== src && tmp !== out && !tmp.isRecycled) tmp.recycle()

        return out
    }

    private fun resizeHorizontalStream(src: Bitmap, newW: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        if (srcW == newW) return src

        val config = src.config ?: Bitmap.Config.ARGB_8888
        val dst = Bitmap.createBitmap(newW, srcH, config)

        val w = precomputeWeights(srcW, newW)

        val inRow = IntArray(srcW)
        val outRow = IntArray(newW)

        for (y in 0 until srcH) {
            src.getPixels(inRow, 0, srcW, 0, y, srcW, 1)

            for (x in 0 until newW) {
                val l = w.left[x]
                val weights = w.weight[x]
                val sumInv = 1f / w.wSum[x]

                var aAcc = 0f
                var rAcc = 0f
                var gAcc = 0f
                var bAcc = 0f

                for (t in 0..KERNEL_TAPS) {
                    val sx = (l + t).coerceIn(0, srcW - 1)
                    val c = inRow[sx]
                    val wf = weights[t]

                    val a = (c ushr 24) and 0xFF
                    val r = (c ushr 16) and 0xFF
                    val g = (c ushr 8) and 0xFF
                    val b = (c) and 0xFF

                    aAcc += a * wf
                    rAcc += r * wf
                    gAcc += g * wf
                    bAcc += b * wf
                }

                val a = (aAcc * sumInv).roundToInt().coerceIn(0, 255)
                val r = (rAcc * sumInv).roundToInt().coerceIn(0, 255)
                val g = (gAcc * sumInv).roundToInt().coerceIn(0, 255)
                val b = (bAcc * sumInv).roundToInt().coerceIn(0, 255)

                outRow[x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }

            dst.setPixels(outRow, 0, newW, 0, y, newW, 1)
        }

        return dst
    }

    private fun resizeVerticalRing(src: Bitmap, newH: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        if (srcH == newH) return src

        val config = src.config ?: Bitmap.Config.ARGB_8888
        val dst = Bitmap.createBitmap(srcW, newH, config)

        val w = precomputeWeights(srcH, newH)

        // Ring buffer of needed source rows
        val ringSize = KERNEL_TAPS + 3
        val ring = Array(ringSize) { IntArray(srcW) }
        val ringRowIndex = IntArray(ringSize) { Int.MIN_VALUE }

        fun getRow(row: Int): IntArray {
            val r = row.coerceIn(0, srcH - 1)
            // try find in ring
            for (i in 0 until ringSize) {
                if (ringRowIndex[i] == r) return ring[i]
            }
            // load into a slot (simple overwrite policy)
            val slot = (r % ringSize + ringSize) % ringSize
            if (ringRowIndex[slot] != r) {
                src.getPixels(ring[slot], 0, srcW, 0, r, srcW, 1)
                ringRowIndex[slot] = r
            }
            return ring[slot]
        }

        val outRow = IntArray(srcW)

        for (y in 0 until newH) {
            val top = w.left[y]
            val weights = w.weight[y]
            val sumInv = 1f / w.wSum[y]

            for (x in 0 until srcW) {
                var aAcc = 0f
                var rAcc = 0f
                var gAcc = 0f
                var bAcc = 0f

                for (t in 0..KERNEL_TAPS) {
                    val sy = top + t
                    val row = getRow(sy)
                    val c = row[x]
                    val wf = weights[t]

                    val a = (c ushr 24) and 0xFF
                    val r = (c ushr 16) and 0xFF
                    val g = (c ushr 8) and 0xFF
                    val b = (c) and 0xFF

                    aAcc += a * wf
                    rAcc += r * wf
                    gAcc += g * wf
                    bAcc += b * wf
                }

                val a = (aAcc * sumInv).roundToInt().coerceIn(0, 255)
                val r = (rAcc * sumInv).roundToInt().coerceIn(0, 255)
                val g = (gAcc * sumInv).roundToInt().coerceIn(0, 255)
                val b = (bAcc * sumInv).roundToInt().coerceIn(0, 255)

                outRow[x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }

            dst.setPixels(outRow, 0, srcW, 0, y, srcW, 1)
        }

        return dst
    }
}
