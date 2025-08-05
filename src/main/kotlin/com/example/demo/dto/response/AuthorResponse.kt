package com.example.demo.dto.response

import java.time.LocalDate

data class AuthorResponse(
    val id: Long?,
    val name: String?,
    val birthDate: LocalDate?
)