package com.example.demo.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 */
data class InsertAuthorRequest(
    val id: Long? = null, // 新規作成時はIDが不要なためnullable
    @field:NotNull(message = "firstNameは必須項目です")
    val firstName: String?,
    val middleName: String?,
    @field:NotNull(message = "lastNameは必須項目です")
    val lastName: String?,
    @field:NotNull(message = "birthDateは必須項目です")
    @field:Past(message = "birthDateは過去の日付である必要があります")
    val birthDate: LocalDate?
)