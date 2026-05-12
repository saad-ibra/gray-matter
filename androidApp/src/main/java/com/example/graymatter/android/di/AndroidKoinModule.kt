package com.example.graymatter.android.di

import com.example.graymatter.android.security.SecureDatabaseKeyManager
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.android.ui.viewmodel.TrashViewModel
import com.example.graymatter.android.ui.fileviewer.FileViewerViewModel
import com.example.graymatter.android.ui.graph.KnowledgeGraphViewModel
import com.example.graymatter.viewmodel.ReferenceSelectorViewModel
import com.example.graymatter.android.ui.viewmodel.LookupsViewModel
import com.example.graymatter.data.local.DatabaseDriverFactory
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidAppModule = module {
    // Hardware-backed key management for database encryption
    single { SecureDatabaseKeyManager(androidContext()) }

    // Provide the Android-specific DatabaseDriverFactory with SQLCipher passphrase
    single {
        val keyManager: SecureDatabaseKeyManager = get()
        DatabaseDriverFactory(androidContext(), keyManager.getDatabasePassphrase())
    }
}

val androidViewModelModule = module {
    viewModel { GrayMatterViewModel(get(), get(), get(), get(), get(), get()) }
    
    viewModel { TrashViewModel(get(), get(), get(), get(), get()) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.TemplateViewModel(get()) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.HomeViewModel(get(), get()) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.DraftingViewModel(get(), get()) }
    
    viewModel { FileViewerViewModel(get(), get(), get(), get(), get()) }
    
    viewModel { KnowledgeGraphViewModel(get(), get(), get(), get(), get()) }
    
    viewModel { LookupsViewModel(get()) }
    
    viewModel { com.example.graymatter.android.ui.library.LibrarySearchViewModel(get(), get(), get(), get()) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.BackupViewModel(androidContext() as android.app.Application) }
    
    viewModel { com.example.graymatter.android.ui.viewmodel.SecurityViewModel(androidContext() as android.app.Application) }
    
    factory {
        ReferenceSelectorViewModel(get(), get(), get(), get(), null, get())
    }
}
