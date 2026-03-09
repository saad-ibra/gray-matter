package com.example.graymatter.domain

data class SearchResult(
    val page: Int,
    val snippet: String,
    val matchStart: Int = 0
)
