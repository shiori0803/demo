package com.example.demo.dto

/**
 * `books`テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * データベースから取得した書籍情報を格納します。
 *
 * @property id 書籍のユニークなID。データベースで自動生成されるため、新規登録時はnullを許容します。
 * @property title 書籍のタイトル。必須項目で、非nullのStringとして定義されます。
 * @property price 書籍の価格。必須項目で、非nullのIntとして定義されます。
 * @property publicationStatus 出版ステータス。0:未出版、1:出版済のいずれかの値をとる、必須項目です。
 */
data class BookDto(
    val id: Long?,
    val title: String,
    val price: Int,
    val publicationStatus: Int,
)
