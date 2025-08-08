package com.example.demo.dto

import java.time.LocalDate

/**
 * `authors`テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * データベースから取得した著者情報を格納します。
 *
 * @property id 著者のユニークなID。データベースで自動生成されるため、新規登録時はnullを許容します。
 * @property name 著者名。必須項目で、非nullのStringとして定義されます。
 * @property birthDate 著者の生年月日。必須項目で、非nullのLocalDateとして定義されます。
 */
data class AuthorDto(
    val id: Long?,
    val name: String?,
    val birthDate: LocalDate?,
)
