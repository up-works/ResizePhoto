package org.onedroid.resizephoto.domain.usecase

import org.onedroid.resizephoto.domain.repository.ExifRepository
import org.onedroid.resizephoto.presentation.home.ExifData
import java.io.File

class GetExifUseCase(
    private val repo: ExifRepository
) {
    suspend operator fun invoke(file: File): ExifData {
        return repo.getExif(file)
    }

    suspend fun copy(source: File, destination: File) {
        repo.copyExif(source, destination)
    }
}
