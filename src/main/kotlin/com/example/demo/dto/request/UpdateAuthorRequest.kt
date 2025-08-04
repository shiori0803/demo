package com.example.demo.dto.request

import java.time.LocalDate
import com.fasterxml.jackson.annotation.JsonSetter // JsonSetter をインポート
import com.fasterxml.jackson.annotation.Nulls // Nulls をインポート
import com.fasterxml.jackson.annotation.JsonInclude // JsonInclude をインポート

/**
 * 著者データ部分更新用リクエストDTO。
 * PATCHリクエストの特性に合わせて、Optional<T>を使用してフィールドの存在/nullを区別します。
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT) // Optional.empty() のフィールドはJSONに含めない設定
data class UpdateAuthorRequest(
    val id: Long? = null,
    val firstName: String?,
    @JsonSetter(nulls = Nulls.SET) // nullが送られてきたら明示的にnullを設定
    val middleName: String?, // JsonNullable<String> から String? に戻す
    val lastName: String?,
    val birthDate: LocalDate?
)
