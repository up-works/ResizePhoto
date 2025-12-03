package org.onedroid.resizephoto.core.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.onedroid.resizephoto.presentation.home.HomeViewModel

val viewModelModule = module {
    viewModel {
        HomeViewModel(androidApplication(), get(), get())
    }
}
