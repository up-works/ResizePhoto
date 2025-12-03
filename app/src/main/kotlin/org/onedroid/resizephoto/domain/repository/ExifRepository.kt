package org.onedroid.resizephoto.domain.repository

import org.onedroid.resizephoto.presentation.home.ExifData
import java.io.File

interface ExifRepository {
    suspend fun getExif(file: File): ExifData
    suspend fun copyExif(source: File, destination: File)
}
