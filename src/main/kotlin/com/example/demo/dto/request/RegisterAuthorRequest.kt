package com.example.demo.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 *
 * @property name 著者名。必須項目で、空文字や空白のみは許可されません。
 * @property birthDate 著者の生年月日。必須項目で、本日を含まない過去日のみ許可されます。
 */
data class RegisterAuthorRequest(
    @field:NotBlank(message = "validation.required") val name: String?,
    @field:NotNull(message = "validation.required")
    @field:Past(message = "validation.birthdate")
    val birthDate: LocalDate?,
)
