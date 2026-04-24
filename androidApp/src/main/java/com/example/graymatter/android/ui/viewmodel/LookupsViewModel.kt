package com.example.graymatter.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.domain.Opinion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LookupsViewModel(
    private val opinionRepository: OpinionRepository
) : ViewModel() {

    private val allOpinions = opinionRepository.getAllOpinions()

    // Filter only dictionary lookups
    private val allLookups = allOpinions.map { opinions ->
        opinions.filter { it.text.startsWith("[DICT") }
    }

    val activeLookups: StateFlow<List<Opinion>> = allLookups.map { lookups ->
        lookups.filter { !it.text.contains(" #learnt") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learntLookups: StateFlow<List<Opinion>> = allLookups.map { lookups ->
        lookups.filter { it.text.contains(" #learnt") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleLearntStatus(opinion: Opinion) {
        viewModelScope.launch {
            val isCurrentlyLearnt = opinion.text.contains(" #learnt")
            val newText = if (isCurrentlyLearnt) {
                opinion.text.replace(" #learnt", "")
            } else {
                "${opinion.text} #learnt"
            }
            opinionRepository.updateOpinion(opinion.copy(text = newText))
        }
    }
}
