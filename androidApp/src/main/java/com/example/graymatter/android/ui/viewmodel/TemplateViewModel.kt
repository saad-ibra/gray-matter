package com.example.graymatter.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.domain.CustomTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing Custom Templates.
 */
class TemplateViewModel(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    val templates: StateFlow<List<CustomTemplate>> = resourceRepository.templatesStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTemplate(template: CustomTemplate) {
        viewModelScope.launch {
            resourceRepository.saveTemplate(template)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            resourceRepository.deleteTemplate(id)
        }
    }
}
