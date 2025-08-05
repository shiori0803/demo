package com.example.demo.dto.response

// 著者情報と書籍リストをまとめるレスポンスDTO
data class AuthorWithBooksResponse(
    val author: AuthorResponse,
    val books: List<BookResponse>
)