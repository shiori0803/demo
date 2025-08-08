package com.example.demo.dto.response

/**
 * 書籍の情報を返却するためのDTOクラス。
 *
 * @property id 書籍のユニークなID。
 * @property title 書籍のタイトル。
 * @property price 書籍の価格。
 * @property publicationStatus 出版ステータス。0: 未出版, 1: 出版済。
 */
data class BookResponse(
    val id: Long?,
    val title: String,
    val price: Int,
    val publicationStatus: Int,
)
