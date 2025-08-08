package com.example.demo.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 書籍新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 *
 * @property title 書籍のタイトル。必須項目で、空文字や空白のみは許可されません。
 * @property price 書籍の価格。必須項目で、0以上である必要があります。
 * @property publicationStatus 出版ステータス。必須項目で、0（未出版）または1（出版済）の値をとります。
 * @property authorIds 関連付ける著者IDのリスト。必須項目で、少なくとも1つのIDを指定する必要があります。
 */
data class RegisterBookRequest(
    @field:NotBlank(message = "validation.required") val title: String?,
    @field:NotNull(message = "validation.required")
    @field:Min(
        value = 0,
        message = "validation.price",
    ) val price: Int?,
    @field:NotNull(message = "validation.required")
    @field:Min(
        value = 0,
        message = "validation.publicationStatus",
    )
    @field:Max(
        value = 1,
        message = "validation.publicationStatus",
    )
    val publicationStatus: Int?,
    @field:NotNull(message = "validation.required")
    @field:Size(
        min = 1,
        message = "validation.authorIds.size.min",
    ) val authorIds: List<Long>?,
)
