package com.example.demo.dto.response

/**
 * エラーレスポンス共通DTOクラス。
 * APIリクエストでエラーが発生した際に、クライアントに返却される情報です。
 *
 * @property status HTTPステータスコード。
 * @property message エラーの概要メッセージ。
 * @property details エラーの詳細情報（URIなど）。
 * @property errors バリデーションエラーの詳細なリスト。
 */
data class ErrorResponse(
    val status: Int,
    val message: String,
    val details: String? = null,
    val errors: List<String>? = null,
)
