package com.example.demo.dto

/**
 * `books` テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * データベースから取得した書籍情報を格納します。
 */
data class BookDto(
    /**
     * 書籍のユニークなID (BIGSERIAL -> Long)
     * データベースによって自動生成されるため、新規登録時はnullを許容します。
     */
    val id: Long?,
    /**
     * 書籍のタイトル (VARCHAR(255) NOT NULL -> String)
     * 必須項目であるため、非nullのStringとして定義します。
     */
    val title: String,
    /**
     * 書籍の価格 (INT NOT NULL -> Int)
     * 必須項目であるため、非nullのIntとして定義します。
     */
    val price: Int,
    /**
     * 出版ステータス (INT NOT NULL -> Int)
     * 0: 未出版, 1: 出版済
     * 必須項目であるため、非nullのIntとして定義します。
     */
    val publicationStatus: Int,
)
