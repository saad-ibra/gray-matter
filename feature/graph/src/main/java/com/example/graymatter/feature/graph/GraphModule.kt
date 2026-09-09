package com.example.graymatter.feature.graph

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val graphModule = module {
    viewModel { KnowledgeGraphViewModel(get(), get(), get(), get(), get()) }
}
