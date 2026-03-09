package com.example.notes.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


actual class NativeStateFlow<T> actual constructor(source: StateFlow<T>) : StateFlow<T> by source {

    fun subscribe(onCollect: (T) -> Unit): DisposableHandle {
        val scope = MainScope().apply {
            launch(Dispatchers.Main) { collect(onCollect) }
        }
        return DisposableHandle { scope.cancel() }
    }
}