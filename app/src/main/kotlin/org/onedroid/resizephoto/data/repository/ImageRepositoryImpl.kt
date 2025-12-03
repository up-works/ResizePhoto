package org.onedroid.resizephoto.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale

class ImageRepositoryImpl(private val context: Context) : ImageRepository {
    override suspend fun resizeImage(imageFile: File, percentage: Int): File {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val width = (bitmap.width * percentage) / 100
            val height = (bitmap.height * percentage) / 100

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            val outputFile = File(context.cacheDir, "resized_${imageFile.name}")

            FileOutputStream(outputFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            outputFile
        }
    }

    override suspend fun resizeImage(imageFile: File, width: Int, height: Int): File {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            val outputFile = File(context.cacheDir, "resized_${imageFile.name}")

            FileOutputStream(outputFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            outputFile
        }
    }
}
