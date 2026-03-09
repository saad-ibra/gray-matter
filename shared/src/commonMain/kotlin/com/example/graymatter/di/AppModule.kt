package com.example.graymatter.di

import com.example.graymatter.data.DefaultItemRepository
import com.example.graymatter.data.DefaultOpinionRepository
import com.example.graymatter.data.DefaultResourceRepository
import com.example.graymatter.data.DefaultTopicRepository
import com.example.graymatter.data.ItemRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.data.local.DatabaseDriverFactory
import com.example.graymatter.database.GrayMatterDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Simple dependency injection module for Gray Matter app.
 */
class AppModule(
    private val databaseDriverFactory: DatabaseDriverFactory
) {
    private val database: GrayMatterDatabase by lazy {
        databaseDriverFactory.createDriver()
    }
    
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    
    val dispatchersProvider: DispatchersProvider by lazy {
        DispatchersProvider()
    }
    
    val resourceRepository: ResourceRepository by lazy {
        DefaultResourceRepository(database, defaultDispatcher)
    }
    
    val opinionRepository: OpinionRepository by lazy {
        DefaultOpinionRepository(database, defaultDispatcher)
    }
    
    val topicRepository: TopicRepository by lazy {
        DefaultTopicRepository(database, defaultDispatcher)
    }
    
    val itemRepository: ItemRepository by lazy {
        DefaultItemRepository(
            database = database,
            resourceRepository = resourceRepository,
            opinionRepository = opinionRepository,
            topicRepository = topicRepository,
            dispatcher = defaultDispatcher
        )
    }
}

/**
 * Provides coroutine dispatchers.
 */
class DispatchersProvider {
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
}
