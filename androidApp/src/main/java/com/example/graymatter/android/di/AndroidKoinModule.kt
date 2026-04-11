package com.example.graymatter.android.di

import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.android.ui.viewmodel.TrashViewModel
import com.example.graymatter.android.ui.fileviewer.FileViewerViewModel
import com.example.graymatter.android.ui.graph.KnowledgeGraphViewModel
import com.example.graymatter.viewmodel.ReferenceSelectorViewModel
import com.example.graymatter.data.local.DatabaseDriverFactory
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidAppModule = module {
    // Provide the Android-specific DatabaseDriverFactory using Koin's androidContext()
    single { DatabaseDriverFactory(androidContext()) }
}

val androidViewModelModule = module {
    viewModel { GrayMatterViewModel(get(), get(), get(), get(), get(), get()) }
    
    viewModel { TrashViewModel(get(), get(), get(), get(), get()) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.TemplateViewModel(get()) }
    
    viewModel { FileViewerViewModel(get(), get(), get(), get(), get()) }
    
    viewModel { KnowledgeGraphViewModel(get(), get(), get(), get(), get()) }
    
    factory {
        ReferenceSelectorViewModel(get(), get(), get(), get(), null, get())
    }
}
