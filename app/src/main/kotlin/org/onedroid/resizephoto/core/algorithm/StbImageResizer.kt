package org.onedroid.resizephoto.core.algorithm


import android.graphics.Bitmap

class StbImageResizer {

    enum class Filter(val value: Int) {
        DEFAULT(getFilterDefault()),
        MITCHELL(getFilterMitchell()),
        CUBIC_BSPLINE(getFilterCubicBSpline()),
        CATMULL_ROM(getFilterCatmullRom())
    }

    companion object {
        init {
            System.loadLibrary("stb_image_resize")
        }

        @JvmStatic
        private external fun getFilterDefault(): Int

        @JvmStatic
        private external fun getFilterMitchell(): Int

        @JvmStatic
        private external fun getFilterCubicBSpline(): Int

        @JvmStatic
        private external fun getFilterCatmullRom(): Int
    }

    /**
     * Resize bitmap using STB image resize
     * @param src Source bitmap (must be ARGB_8888)
     * @param width Target width
     * @param height Target height
     * @param filter Filter type (DEFAULT, MITCHELL, CUBIC_BSPLINE, CATMULL_ROM)
     * @return Resized bitmap or null on failure
     */
    fun resize(
        src: Bitmap,
        width: Int,
        height: Int,
        filter: Filter = Filter.MITCHELL
    ): Bitmap? {
        require(src.config == Bitmap.Config.ARGB_8888) {
            "Source bitmap must be ARGB_8888"
        }
        require(width > 0 && height > 0) {
            "Dimensions must be positive"
        }

        val dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        return if (nativeResize(src, dst, filter.value)) {
            dst
        } else {
            dst.recycle()
            null
        }
    }

    private external fun nativeResize(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        filter: Int
    ): Boolean
}