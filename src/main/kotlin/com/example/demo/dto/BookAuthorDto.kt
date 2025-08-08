package com.example.demo.dto

/**
 * `book_authors`テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * 書籍と著者の関連情報を格納します。
 *
 * @property bookId 書籍のID。必須項目で、非nullのLongとして定義されます。
 * @property authorId 著者のID。必須項目で、非nullのLongとして定義されます。
 */
data class BookAuthorDto(
    val bookId: Long,
    val authorId: Long,
)
