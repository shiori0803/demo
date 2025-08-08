package com.example.demo.dto.response

/**
 * 著者情報検索APIのレスポンス用DTOクラス。
 * 著者情報と、その著者が執筆した書籍のリストを保持します。
 *
 * @property author 著者情報。
 * @property books 著者が執筆した書籍情報のリスト。
 */
data class AuthorWithBooksResponse(
    val author: AuthorResponse,
    val books: List<BookResponse>,
)
