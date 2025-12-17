package org.onedroid.resizephoto.domain.repository

import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import java.io.File

interface ImageRepository {
    suspend fun resizeImage(imageFile: File, percentage: Int, algorithm: ResizeAlgorithm): File
    suspend fun resizeImage(imageFile: File, width: Int, height: Int, algorithm: ResizeAlgorithm): File
}
