package com.example.demo.dto.response

data class BookResponse(
    val id: Long?,
    val title: String,
    val price: Int,
    val publicationStatus: Int,
)
