package com.example.demo.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * 書籍データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、フィールドの存在/nullを区別します。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT) // JSONに存在しないフィールドは含めない
data class UpdateBookRequest(
    val id: Long? = null, // パス変数でIDを受け取るため、ボディのIDは無視またはチェック用

    val title: String?,

    @field:Min(value = 0, message = "価格は0以上である必要があります")
    val price: Int?,

    @field:Min(value = 0, message = "出版ステータスは0または1である必要があります")
    val publicationStatus: Int?, // 0: 未出版, 1: 出版済

    // 著者IDリストはOptional<List<Long>>やJsonNullable<List<Long>>も考えられるが、
    // シンプルにList<Long>?として、nullの場合は更新しない、空リストの場合は関連を全て削除、と解釈する
    @field:Size(min = 1, message = "著者IDは少なくとも1つ指定する必要があります") // 0以上を許可
    val authorIds: List<Long>?
)