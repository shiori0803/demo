package com.example.demo.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ新規登録用リクエストDTO。
 * APIリクエストのバリデーションに使用されます。
 *
 * @property name 著者名。必須項目で、空文字や空白のみは許可されない。
 * @property birthDate 著者の生年月日。必須項目で、本日を含める未来日は許可されない。
 */
data class RegisterAuthorRequest(
    // name,birthDate共にnullを許容しないが「String?」ではなく「String」の定義にすると
    // そもそもリクエストにnull値をセットするとそもそもオブジェクトが作成できず、500エラーとなる
    // 「必須である」というメッセージを表示するためあえて「String?」で定義する
    @field:NotBlank(message = "validation.required") val name: String?,
    @field:NotNull(message = "validation.required")
    @field:Past(message = "validation.birthdate")
    val birthDate: LocalDate?,
)
