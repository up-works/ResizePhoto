package org.onedroid.resizephoto.domain.usecase

import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File

class ResizeImageUseCase(private val imageRepository: ImageRepository) {
    suspend operator fun invoke(imageFile: File, percentage: Int): File {
        return imageRepository.resizeImage(imageFile, percentage)
    }

    suspend operator fun invoke(imageFile: File, width: Int, height: Int): File {
        return imageRepository.resizeImage(imageFile, width, height)
    }
}
