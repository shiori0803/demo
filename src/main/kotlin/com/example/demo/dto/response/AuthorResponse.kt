package com.example.demo.dto.response

import java.time.LocalDate

/**
 * 著者の情報を返却するためのDTOクラス。
 *
 * @property id 著者のユニークなID。
 * @property name 著者名。
 * @property birthDate 著者の生年月日。
 */
data class AuthorResponse(
    val id: Long?,
    val name: String?,
    val birthDate: LocalDate?,
)
