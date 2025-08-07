package com.example.demo.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * 書籍データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、フィールドの存在/nullを区別します。
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
    @field:Size(
        min = 1,
        message = "validation.authorIds.size.min",
    )
    val authorIds: List<Long>?,
)
