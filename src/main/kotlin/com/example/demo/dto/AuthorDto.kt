package com.example.demo.dto

import java.time.LocalDate

/**
 * `authors` テーブルに対応するデータ転送オブジェクト (DTO) クラス。
 * データベースから取得した著者情報を格納します。
 */
data class AuthorDto(
    /**
     * 著者のユニークなID (BIGSERIAL -> Long)
     */
    val id: Long,

    /**
     * 著者のファーストネーム (VARCHAR(255) NOT NULL -> String)
     */
    val firstName: String,

    /**
     * 著者のミドルネーム (VARCHAR(255) -> String?)
     * NULLを許容するため、nullableなStringとして定義します。
     */
    val middleName: String?,

    /**
     * 著者のラストネーム (VARCHAR(255) NOT NULL -> String)
     */
    val lastName: String,

    /**
     * 著者の生年月日 (DATE NOT NULL -> LocalDate)
     * Java 8以降のDate/Time APIを使用します。
     */
    val birthDate: LocalDate
)
