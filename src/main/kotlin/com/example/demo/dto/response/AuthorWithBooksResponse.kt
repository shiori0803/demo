package com.example.demo.dto.response

/**
 * 著者情報検索APIのレスポンス用オブジェクト
 *
 * @property author
 * @property books
 */
data class AuthorWithBooksResponse(
    val author: AuthorResponse,
    val books: List<BookResponse>,
)
