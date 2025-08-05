package com.example.demo.dto.response

data class BookWithAuthorsResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: Int,
    val authorIds: List<Long>
)