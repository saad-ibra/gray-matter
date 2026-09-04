package com.example.graymatter.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graymatter.data.TagRepository
import com.example.graymatter.domain.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TagViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = tagRepository.allTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun createTag(name: String) {
        val trimmed: String = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing: Tag? = tagRepository.getTagByName(trimmed)
            if (existing != null) return@launch
            val tag = Tag(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = System.currentTimeMillis()
            )
            tagRepository.insertTag(tag)
        }
    }

    fun renameTag(id: String, newName: String) {
        val trimmed: String = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            tagRepository.updateTagName(id, trimmed)
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch {
            tagRepository.deleteTag(id)
        }
    }

    fun getTagsByEntryId(entryId: String): Flow<List<Tag>> {
        return tagRepository.getTagsByEntryId(entryId)
    }

    fun getEntryCountByTagId(tagId: String): Flow<Long> {
        return kotlinx.coroutines.flow.flow {
            emit(tagRepository.getEntryCountByTagId(tagId))
        }
    }
}
