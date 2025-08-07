package com.example.demo.dto

/**
 * `book_authors` テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * 書籍と著者の関連情報を格納します。
 */
data class BookAuthorDto(
    /**
     * 書籍のID (BIGINT NOT NULL)
     */
    val bookId: Long,
    /**
     * 著者のID (BIGINT NOT NULL)
     */
    val authorId: Long,
)
