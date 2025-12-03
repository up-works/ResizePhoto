package org.onedroid.resizephoto.core.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.onedroid.resizephoto.data.repository.ExifRepositoryImpl
import org.onedroid.resizephoto.data.repository.ImageRepositoryImpl
import org.onedroid.resizephoto.domain.repository.ExifRepository
import org.onedroid.resizephoto.domain.repository.ImageRepository

val repositoryModule = module {
    single<ExifRepository> { ExifRepositoryImpl() }
    single<ImageRepository> { ImageRepositoryImpl(androidContext()) }
}
