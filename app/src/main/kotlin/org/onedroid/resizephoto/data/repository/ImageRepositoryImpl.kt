package org.onedroid.resizephoto.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.core.LanczosResizer
import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream

class ImageRepositoryImpl(private val context: Context) : ImageRepository {

    override suspend fun resizeImage(imageFile: File, percentage: Int, useLanczos: Boolean): File {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val width = (bitmap.width * percentage) / 100
            val height = (bitmap.height * percentage) / 100

            val resizedBitmap = if (useLanczos) {
                LanczosResizer.resize(bitmap, width, height)
            } else {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
            saveBitmap(resizedBitmap, imageFile.name)
        }
    }

    override suspend fun resizeImage(imageFile: File, width: Int, height: Int, useLanczos: Boolean): File {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val resizedBitmap = if (useLanczos) {
                LanczosResizer.resize(bitmap, width, height)
            } else {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
            saveBitmap(resizedBitmap, imageFile.name)
        }
    }

    private fun saveBitmap(bitmap: Bitmap, originalName: String): File {
        val filename = "resized_$originalName"
        val cacheFile = File(context.cacheDir, filename)

        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        saveToGallery(cacheFile, filename)
        return cacheFile
    }

    private fun saveToGallery(file: File, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Image Format Converter"
                )
            }
            val uri =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    file.inputStream().copyTo(out)
                }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Image Format Converter"
            )
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, filename)
            file.copyTo(dest, overwrite = true)
        }
    }
}
