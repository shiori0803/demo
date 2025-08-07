package com.example.demo.dto

import java.time.LocalDate

/**
 * `authors` テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * データベースから取得した著者情報を格納します。
 */
data class AuthorDto(
    val id: Long?,
    val name: String?, // 新しい name カラム
    val birthDate: LocalDate?,
)
