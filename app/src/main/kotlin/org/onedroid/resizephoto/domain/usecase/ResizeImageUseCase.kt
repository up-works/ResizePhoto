package org.onedroid.resizephoto.domain.usecase

import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import org.onedroid.resizephoto.domain.repository.ImageRepository
import java.io.File

class ResizeImageUseCase(private val imageRepository: ImageRepository) {
    suspend operator fun invoke(imageFile: File, percentage: Int, useLanczos: Boolean = false): File {
        return imageRepository.resizeImage(imageFile, percentage, useLanczos)
    }

    suspend operator fun invoke(imageFile: File, width: Int, height: Int, useLanczos: Boolean = false): File {
        return imageRepository.resizeImage(imageFile, width, height, useLanczos)
    }

    suspend operator fun invoke(imageFile: File, percentage: Int, algorithm: ResizeAlgorithm): File {
        return imageRepository.resizeImage(imageFile, percentage, algorithm)
    }

    suspend operator fun invoke(imageFile: File, width: Int, height: Int, algorithm: ResizeAlgorithm): File {
        return imageRepository.resizeImage(imageFile, width, height, algorithm)
    }
}
