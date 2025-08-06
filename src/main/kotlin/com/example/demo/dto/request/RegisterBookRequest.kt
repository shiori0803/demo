package com.example.demo.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 書籍新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 */
data class RegisterBookRequest(
    val id: Long? = null,

    @field:NotBlank(message = "タイトルは必須項目です")
    val title: String?,

    @field:NotNull(message = "価格は必須項目です")
    @field:Min(value = 0, message = "価格は0以上である必要があります")
    val price: Int?,

    @field:NotNull(message = "出版ステータスは必須項目です")
    @field:Min(value = 0, message = "出版ステータスは0または1である必要があります")
    val publicationStatus: Int?, // 0: 未出版, 1: 出版済

    @field:NotNull(message = "著者IDは必須項目です")
    @field:Size(min = 1, message = "著者IDは少なくとも1つ指定する必要があります")
    val authorIds: List<Long>? // 関連付ける著者IDのリスト
)