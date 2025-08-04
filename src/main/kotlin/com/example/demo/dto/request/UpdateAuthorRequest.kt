package com.example.demo.dto.request

import java.time.LocalDate
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 著者データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、Optional<T>を使用してフィールドの存在/nullを区別します。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class UpdateAuthorRequest(
    val id: Long? = null,
    val name: String?,
    val birthDate: LocalDate?
)