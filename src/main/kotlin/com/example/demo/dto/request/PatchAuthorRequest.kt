package com.example.demo.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、Optional<T>を使用してフィールドの存在/nullを区別します。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class PatchAuthorRequest(
    val id: Long? = null,
    val name: String?,
    @field:Past(message = "validation.birthdate")
    val birthDate: LocalDate?
)