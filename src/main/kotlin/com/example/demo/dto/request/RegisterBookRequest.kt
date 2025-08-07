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
 * @property title 書籍のタイトル。必須項目で、空文字や空白のみは許可されない。
 * @property price 書籍の価格。必須項目で、0以上である必要がある。
 * @property publicationStatus 出版ステータス。必須項目で、0（未出版）または1（出版済）の値をとる。
 * @property authorIds 関連付ける著者IDのリスト。必須項目で、少なくとも1つのIDを指定する必要がある。
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
    // 要素の最後にコンマを付けてもエラーにならない
    // 利点：新しい項目追加の時に最後の行を修正しなくてよいので差分が少ない。行の入れ替えのときのコンマ付け忘れが無くなる
)
