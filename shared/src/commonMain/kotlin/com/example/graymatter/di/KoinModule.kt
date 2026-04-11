package com.example.graymatter.di

import com.example.graymatter.data.*
import com.example.graymatter.data.local.DatabaseDriverFactory
import com.example.graymatter.database.GrayMatterDatabase
import com.example.graymatter.domain.business.AutoLinkService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val sharedModule = module {
    single<CoroutineDispatcher> { Dispatchers.Default }
    single<GrayMatterDatabase> { get<DatabaseDriverFactory>().createDriver() }
    
    single<ResourceRepository> { DefaultResourceRepository(get(), get()) }
    single<OpinionRepository> { DefaultOpinionRepository(get(), get()) }
    single<TopicRepository> { DefaultTopicRepository(get(), get()) }
    single<ReferenceLinkRepository> { DefaultReferenceLinkRepository(get(), get()) }
    
    single<ResourceEntryRepository> { 
        DefaultResourceEntryRepository(get(), get(), get(), get(), get()) 
    }
    
    single { AutoLinkService(get(), get(), get()) }
}
