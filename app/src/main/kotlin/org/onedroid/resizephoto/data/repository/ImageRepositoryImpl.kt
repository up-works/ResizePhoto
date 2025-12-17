package org.onedroid.resizephoto.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.core.algorithm.LanczosResizer
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

        return cacheFile
    }
}
