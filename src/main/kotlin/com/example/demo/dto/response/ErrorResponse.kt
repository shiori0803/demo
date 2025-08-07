package com.example.demo.dto.response

/**
 * エラーレスポンス共通クラス
 */
data class ErrorResponse(
    val status: Int,
    val message: String,
    val details: String? = null,
    val errors: List<String>? = null, // バリデーションエラーの詳細を格納する
)
