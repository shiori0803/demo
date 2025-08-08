package com.example.demo.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 書籍データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、フィールドの存在/nullを区別します。
 *
 * @property id 更新対象の書籍ID。リクエストボディでは使用されません。
 * @property title 書籍のタイトル。nullまたは空文字・空白のみの場合は更新対象から除外されます。
 * @property price 書籍の価格。0以上である必要があります。
 * @property publicationStatus 出版ステータス。0（未出版）または1（出版済）の値をとります。
 * @property authorIds 関連付ける著者IDのリスト。少なくとも1つのIDを指定する必要があります。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class PatchBookRequest(
    val id: Long? = null,
    val title: String?,
    @field:Min(
        value = 0,
        message = "validation.price",
    )
    val price: Int?,
    @field:Min(
        value = 0,
        message = "validation.publicationStatus",
    )
    @field:Max(
        value = 1,
        message = "validation.publicationStatus",
    )
    val publicationStatus: Int?,
    val authorIds: List<Long>?,
)
