package com.example.demo.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Past
import java.time.LocalDate

/**
 * 著者データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、フィールドの存在/nullを区別します。
 *
 * @property id 更新対象の著者ID。リクエストボディでは通常使用されず、パス変数から取得されます。
 * @property name 著者名。nullまたは空文字・空白のみの場合は更新対象から除外されます。
 * @property birthDate 著者の生年月日。現在日を含まない過去日のみ許可されます。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
data class PatchAuthorRequest(
    val id: Long? = null,
    val name: String?,
    @field:Past(message = "validation.birthdate")
    val birthDate: LocalDate?,
)
