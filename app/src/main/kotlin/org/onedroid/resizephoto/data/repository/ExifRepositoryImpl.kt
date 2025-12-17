package org.onedroid.resizephoto.data.repository

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.onedroid.resizephoto.domain.repository.ExifRepository
import org.onedroid.resizephoto.presentation.home.ExifData
import java.io.File
import java.lang.reflect.Modifier

class ExifRepositoryImpl : ExifRepository {
    private val exifTags by lazy {
        // Use reflection to get all public static String fields starting with "TAG_"
        ExifInterface::class.java.fields
            .filter { field ->
                field.name.startsWith("TAG_") &&
                        field.type == String::class.java &&
                        Modifier.isStatic(field.modifiers) &&
                        Modifier.isPublic(field.modifiers)
            }
            .mapNotNull { field ->
                try {
                    field.get(null) as? String
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            .distinct()
    }

    override suspend fun getExif(file: File): ExifData {
        return withContext(Dispatchers.IO) {
            val exif = ExifInterface(file.absolutePath)

            val result = exifTags.mapNotNull { tag ->
                exif.getAttribute(tag)?.let { value ->
                    tag to value
                }
            }.toMap()

            ExifData(values = result)
        }
    }

    override suspend fun copyExif(source: File, destination: File) {
        withContext(Dispatchers.IO) {
            val oldExif = ExifInterface(source.absolutePath)
            val newExif = ExifInterface(destination.absolutePath)

            exifTags.forEach { tag ->
                oldExif.getAttribute(tag)?.let { value ->
                    newExif.setAttribute(tag, value)
                }
            }
            newExif.saveAttributes()
        }
    }
}
