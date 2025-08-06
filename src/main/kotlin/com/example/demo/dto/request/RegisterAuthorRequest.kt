package com.example.demo.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 */
data class RegisterAuthorRequest(
    // name,birthDate共にnullを許容しないが「String?」ではなく「String」の定義にすると
    // そもそもリクエストにnull値をセットするとそもそもオブジェクトが作成できず、500エラーとなる
    // 「必須である」というメッセージを表示するためあえて「String?」で定義する
    @field:NotBlank(message = "validation.required")
    val name: String?,
    @field:NotNull(message = "validation.required")
    @field:Past(message = "validation.birthdate")
    val birthDate: LocalDate?
)