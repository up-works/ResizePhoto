package org.onedroid.resizephoto.domain.repository

import java.io.File

interface ImageRepository {
    suspend fun resizeImage(imageFile: File, percentage: Int, useLanczos: Boolean): File
    suspend fun resizeImage(imageFile: File, width: Int, height: Int, useLanczos: Boolean): File
}
