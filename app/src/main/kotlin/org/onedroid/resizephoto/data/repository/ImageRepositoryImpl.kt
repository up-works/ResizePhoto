package org.onedroid.resizephoto.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.core.algorithm.LanczosResizer
import org.onedroid.resizephoto.core.algorithm.StbImageResizer
import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import androidx.core.graphics.scale

class ImageRepositoryImpl(private val context: Context) : ImageRepository {

    private val lanczosResizer = LanczosResizer()
    private val stbResizer = StbImageResizer()

    override suspend fun resizeImage(
        imageFile: File,
        percentage: Int,
        algorithm: ResizeAlgorithm
    ): File = withContext(Dispatchers.IO) {
        require(percentage in 1..100) { "Percentage must be between 1 and 100" }
        require(imageFile.exists()) { "Image file does not exist" }

        val bounds = getOrientedBounds(imageFile)
        val targetWidth = (bounds.width * percentage / 100f).roundToInt().coerceAtLeast(1)
        val targetHeight = (bounds.height * percentage / 100f).roundToInt().coerceAtLeast(1)

        resizeImage(imageFile, targetWidth, targetHeight, algorithm)
    }

    override suspend fun resizeImage(
        imageFile: File,
        width: Int,
        height: Int,
        algorithm: ResizeAlgorithm
    ): File = withContext(Dispatchers.IO) {
        require(width > 0 && height > 0) { "Dimensions must be positive" }
        require(imageFile.exists()) { "Image file does not exist" }

        // Get actual dimensions after EXIF orientation
        val bounds = getOrientedBounds(imageFile)

        // If already target size, return original
        if (bounds.width == width && bounds.height == height) {
            return@withContext imageFile
        }

        // Calculate optimal inSampleSize using oriented dimensions
        val sampleSize = calculateInSampleSize(bounds.width, bounds.height, width, height)

        // Decode with inSampleSize for memory efficiency
        val sampledBitmap = decodeSampled(imageFile, sampleSize)

        try {
            // Perform final resize
            val resizedBitmap = when (algorithm) {
                ResizeAlgorithm.BITMAP_SCALING -> {
                    resizeBitmapScaling(sampledBitmap, width, height)
                }
                ResizeAlgorithm.LANCZOS -> {
                    lanczosResizer.resize(sampledBitmap, width, height)
                }
                ResizeAlgorithm.STB_MITCHELL -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.MITCHELL)
                }
                ResizeAlgorithm.STB_DEFAULT -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.DEFAULT)
                }
                ResizeAlgorithm.STB_CUBIC_BSPLINE -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.CUBIC_BSPLINE)
                }
                ResizeAlgorithm.STB_CATMULL_ROM -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.CATMULL_ROM)
                }
                ResizeAlgorithm.STB_BOX -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.BOX)
                }
                ResizeAlgorithm.STB_TRIANGLE -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.TRIANGLE)
                }
                ResizeAlgorithm.STB_POINT_SAMPLE -> {
                    stbResizer.resize(sampledBitmap, width, height, StbImageResizer.Filter.POINT_SAMPLE)
                }
            } ?: throw IllegalStateException("Resize failed")

            try {
                // Save to file with EXIF data preserved
                return@withContext saveBitmap(resizedBitmap, imageFile)
            } finally {
                // Only recycle if it's a different bitmap
                if (resizedBitmap !== sampledBitmap && !resizedBitmap.isRecycled) {
                    resizedBitmap.recycle()
                }
            }
        } finally {
            if (!sampledBitmap.isRecycled) {
                sampledBitmap.recycle()
            }
        }
    }

    /**
     * Decode image bounds without loading full bitmap
     */
    private data class ImageBounds(val width: Int, val height: Int)

    private fun decodeBounds(file: File): ImageBounds {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return ImageBounds(options.outWidth, options.outHeight)
    }

    /**
     * Get image bounds after applying EXIF orientation
     * (dimensions swap for 90° and 270° rotations)
     */
    private fun getOrientedBounds(file: File): ImageBounds {
        val bounds = decodeBounds(file)

        val exif = try {
            ExifInterface(file.absolutePath)
        } catch (e: Exception) {
            return bounds
        }

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        // Swap width and height for 90° and 270° rotations
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                ImageBounds(bounds.height, bounds.width) // Swapped!
            }
            else -> bounds
        }
    }

    /**
     * Calculate optimal inSampleSize to reduce memory usage
     * inSampleSize is a power of 2 that subsamples the image
     */
    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1

        // Don't subsample if upscaling or already close to target
        if (srcWidth <= targetWidth && srcHeight <= targetHeight) {
            return 1
        }

        // Calculate the largest inSampleSize that keeps dimensions >= target
        // This reduces memory while still providing enough pixels for quality resize
        while (srcWidth / (sampleSize * 2) >= targetWidth &&
            srcHeight / (sampleSize * 2) >= targetHeight
        ) {
            sampleSize *= 2
        }

        return sampleSize
    }

    /**
     * Decode image with inSampleSize for memory efficiency
     */
    private fun decodeSampled(file: File, sampleSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false // STB/Lanczos will create new bitmap anyway
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IllegalStateException("Failed to decode image")

        // Apply EXIF orientation to prevent rotation issues
        return applyExifOrientation(bitmap, file)
    }

    /**
     * Apply EXIF orientation to bitmap
     */
    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val exif = try {
            ExifInterface(file.absolutePath)
        } catch (e: Exception) {
            return bitmap // If EXIF fails, return original
        }

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        // Return early if no rotation needed
        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap // Should never reach here
        }

        return try {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            // Only recycle if a new bitmap was created
            if (rotated !== bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            // If rotation fails due to memory, return original
            bitmap
        }
    }

    /**
     * Fast bitmap scaling using built-in filtering
     * Good for downscaling, faster than Lanczos but slightly lower quality
     */
    private fun resizeBitmapScaling(src: Bitmap, width: Int, height: Int): Bitmap {
        if (src.width == width && src.height == height) {
            return src
        }
        return src.scale(width, height)
    }

    /**
     * Save bitmap to file with optimal compression and EXIF preservation
     */
    private fun saveBitmap(bitmap: Bitmap, originalFile: File): File {
        // Determine format from original file
        val format = when (originalFile.extension.lowercase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }

        // Create output file
        val outputFile = createTempFile(
            prefix = "resized_${System.currentTimeMillis()}",
            suffix = ".${originalFile.extension}",
            directory = context.cacheDir
        )

        // Compress and save
        FileOutputStream(outputFile).use { out ->
            val quality = if (format == Bitmap.CompressFormat.PNG) {
                100 // PNG is lossless, quality param ignored
            } else {
                100 // High quality for JPEG/WebP
            }
            bitmap.compress(format, quality, out)
            out.flush()
        }

        // Preserve EXIF data (important for photos!)
        copyExifData(originalFile, outputFile)

        return outputFile
    }

    /**
     * Copy EXIF metadata from original to resized image
     * CRITICAL: Preserves GPS, camera info, date taken, etc.
     */
    private fun copyExifData(sourceFile: File, destFile: File) {
        try {
            val sourceExif = ExifInterface(sourceFile.absolutePath)
            val destExif = ExifInterface(destFile.absolutePath)

            // List of EXIF tags to preserve
            val tags = listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_APERTURE_VALUE,
                ExifInterface.TAG_ISO_SPEED,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_SOFTWARE
            )

            // Copy each tag if it exists
            tags.forEach { tag ->
                sourceExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }

            // Reset orientation to NORMAL since we already applied rotation
            destExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )

            // Save the EXIF data
            destExif.saveAttributes()
        } catch (e: Exception) {
            // Log but don't fail - EXIF preservation is nice-to-have
            android.util.Log.w("ImageRepository", "Failed to copy EXIF data", e)
        }
    }
}