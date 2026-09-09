package com.example.graymatter.feature.fileviewer

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val fileViewerModule = module {
    viewModel { FileViewerViewModel(get(), get(), get(), get(), get()) }
}
