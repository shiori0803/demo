package com.example.demo.repository

import com.example.demo.dto.AuthorDto
import org.jooq.DSLContext
import db.tables.Authors.AUTHORS
import db.tables.records.AuthorsRecord
import org.springframework.stereotype.Repository

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
     * 既存の著者データを更新します。
     * @param authorDto 更新する著者データを含むDTO。idは更新対象を特定するために使用します。
     * @return 更新されたレコード数
     */
    fun updateAuthor(authorDto: AuthorDto): Int {
        return dslContext.update(AUTHORS)
            .set(AUTHORS.FIRST_NAME, authorDto.firstName)
            .set(AUTHORS.MIDDLE_NAME, authorDto.middleName)
            .set(AUTHORS.LAST_NAME, authorDto.lastName)
            .set(AUTHORS.BIRTH_DATE, authorDto.birthDate)
            .where(AUTHORS.ID.eq(authorDto.id))
            .execute()
    }
}
