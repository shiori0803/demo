package com.example.demo.dto.response

import java.time.LocalDate

/**
 * 著者の情報を返却するためのオブジェクト
 *
 * @property id
 * @property name
 * @property birthDate
 */
data class AuthorResponse(
    val id: Long?,
    val name: String?,
    val birthDate: LocalDate?
)