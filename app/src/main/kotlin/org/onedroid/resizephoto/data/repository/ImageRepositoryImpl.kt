package org.onedroid.resizephoto.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.core.algorithm.LanczosResizer
import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale

class ImageRepositoryImpl(private val context: Context) : ImageRepository {

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun resizeImage(imageFile: File, percentage: Int, useLanczos: Boolean): File {
        val algorithm = if (useLanczos) ResizeAlgorithm.LANCZOS else ResizeAlgorithm.BITMAP_SCALING
        return resizeImage(imageFile, percentage, algorithm)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun resizeImage(
        imageFile: File,
        percentage: Int,
        algorithm: ResizeAlgorithm
    ): File = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(imageFile)
        val targetWidth = (bitmap.width * percentage) / 100
        val targetHeight = (bitmap.height * percentage) / 100

        resizeAndSave(bitmap, imageFile.name, targetWidth, targetHeight, algorithm)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun resizeImage(
        imageFile: File,
        width: Int,
        height: Int,
        useLanczos: Boolean
    ): File {
        val algorithm = if (useLanczos) ResizeAlgorithm.LANCZOS else ResizeAlgorithm.BITMAP_SCALING
        return resizeImage(imageFile, width, height, algorithm)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun resizeImage(
        imageFile: File,
        width: Int,
        height: Int,
        algorithm: ResizeAlgorithm
    ): File = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(imageFile)
        resizeAndSave(bitmap, imageFile.name, width, height, algorithm)
    }

    // Core resizing logic extracted to eliminate duplication
    @RequiresApi(Build.VERSION_CODES.R)
    private fun resizeAndSave(
        bitmap: Bitmap,
        originalName: String,
        targetWidth: Int,
        targetHeight: Int,
        algorithm: ResizeAlgorithm
    ): File {
        val resizedBitmap = resizeBitmap(bitmap, targetWidth, targetHeight, algorithm)
        val format = determineCompressFormat(originalName)
        return saveBitmap(resizedBitmap, originalName, format)
    }

    private fun decodeBitmap(imageFile: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException("Failed to decode image: ${imageFile.name}")
        return rotateBitmapIfRequired(bitmap, imageFile)
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, imageFile: File): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun resizeBitmap(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        algorithm: ResizeAlgorithm
    ): Bitmap {
        return when (algorithm) {
            ResizeAlgorithm.LANCZOS -> LanczosResizer.resize(bitmap, width, height)
            ResizeAlgorithm.BITMAP_SCALING -> bitmap.scale(width, height)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun saveBitmap(
        bitmap: Bitmap,
        originalName: String,
        format: Bitmap.CompressFormat
    ): File {
        val outputFile = createOutputFile(originalName, format)

        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(format, COMPRESSION_QUALITY, stream)
        }

        return outputFile
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createOutputFile(originalName: String, format: Bitmap.CompressFormat): File {
        val baseName = originalName.substringBeforeLast('.')
        val extension = format.toFileExtension()
        val filename = "resized_${baseName}.$extension"

        return File(context.cacheDir, filename)
    }

    private fun determineCompressFormat(filename: String): Bitmap.CompressFormat {
        val extension = filename.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun Bitmap.CompressFormat.toFileExtension(): String {
        return when (this) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP,
            Bitmap.CompressFormat.WEBP_LOSSY,
            Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
            else -> "jpg"
        }
    }

    companion object {
        private const val COMPRESSION_QUALITY = 100
    }
}