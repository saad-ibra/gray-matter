package com.example.notes.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

actual class NativeStateFlow<T> actual constructor(source: StateFlow<T>) : StateFlow<T> by source