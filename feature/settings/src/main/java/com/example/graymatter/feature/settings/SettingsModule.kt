package com.example.graymatter.feature.settings

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val settingsModule = module {
    viewModel { BackupViewModel(androidContext() as android.app.Application) }
    viewModel { SecurityViewModel(androidContext() as android.app.Application) }
    viewModel { LookupsViewModel(get()) }
}
