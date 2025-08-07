package com.example.demo.dto.response

/**
 * 書籍情報とそれに紐づく著者IDリストを返却するためのDTOクラス。
 *
 * @property id 書籍のユニークなID。
 * @property title 書籍のタイトル。
 * @property price 書籍の価格。
 * @property publicationStatus 出版ステータス。0: 未出版, 1: 出版済。
 * @property authorIds 書籍に紐づく著者IDのリスト。
 */
data class BookWithAuthorsResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: Int,
    val authorIds: List<Long>,
)
