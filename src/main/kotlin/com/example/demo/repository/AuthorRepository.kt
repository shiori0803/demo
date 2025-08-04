package com.example.demo.repository

import com.example.demo.dto.AuthorDto
import org.jooq.DSLContext
import db.tables.Authors.AUTHORS
import db.tables.records.AuthorsRecord
import org.springframework.stereotype.Repository
import java.time.LocalDate
import org.jooq.Field

@Repository
class AuthorRepository(private val dslContext: DSLContext) {
    /**
     * 新しい著者データをデータベースに挿入します。
     * @param authorDto 挿入する著者データを含むDTO
     * @return 挿入された著者のID
     */
    fun insertAuthor(authorDto: AuthorDto): Long? {
        // returning(AUTHORS.ID) を使用して、自動生成されたIDを取得
        val record: AuthorsRecord? = dslContext.insertInto(AUTHORS).set(AUTHORS.FIRST_NAME, authorDto.firstName)
            .set(AUTHORS.MIDDLE_NAME, authorDto.middleName)
            .set(AUTHORS.LAST_NAME, authorDto.lastName).set(AUTHORS.BIRTH_DATE, authorDto.birthDate)
            .returning(AUTHORS.ID)
            .fetchOne()
        return record?.id
    }

    /**
     * 既存の著者データを部分的に更新します (PATCHセマンティクス)。
     * @param id 更新対象の著者ID
     * @param updates 更新するフィールド名と値のマップ。
     * マップに存在しないフィールドは更新しません。
     * マップに存在するが値がnullのフィールドは、DBの対応するカラムをnullに更新します（nullableなカラムの場合）。
     * @return 更新されたレコード数
     */
    fun updateAuthor(id: Long, updates: Map<String, Any?>): Int {
        if (updates.isEmpty()) {
            return 0 // 更新するフィールドがない場合
        }

        // 更新するフィールドと値のマップをjOOQのFieldオブジェクトと値のペアに変換
        val jooqUpdateMap = mutableMapOf<Field<*>, Any?>()

        updates.forEach { (fieldName, value) ->
            when (fieldName) {
                "firstName" -> jooqUpdateMap[AUTHORS.FIRST_NAME] = value as String?
                "middleName" -> jooqUpdateMap[AUTHORS.MIDDLE_NAME] = value as String?
                "lastName" -> jooqUpdateMap[AUTHORS.LAST_NAME] = value as String?
                "birthDate" -> jooqUpdateMap[AUTHORS.BIRTH_DATE] = value as LocalDate?
                // 他のフィールドがあればここに追加
                else -> throw IllegalArgumentException("不明なフィールド名: $fieldName")
            }
        }

        // jOOQのupdate().set(Map<Field<?>, ?>) を使用して、動的にSET句を構築
        return dslContext.update(AUTHORS)
            .set(jooqUpdateMap) // マップを直接渡すことで、型推論の問題を回避
            .where(AUTHORS.ID.eq(id))
            .execute()
    }
}
