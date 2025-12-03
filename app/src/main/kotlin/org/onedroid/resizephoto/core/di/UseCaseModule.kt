package org.onedroid.resizephoto.core.di

import org.koin.dsl.module
import org.onedroid.resizephoto.domain.usecase.GetExifUseCase
import org.onedroid.resizephoto.domain.usecase.ResizeImageUseCase

val useCaseModule = module {
    factory { GetExifUseCase(get()) }
    factory { ResizeImageUseCase(get()) }
}
