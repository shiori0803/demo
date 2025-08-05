package com.example.demo.repository

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.BookDto
import db.tables.Authors.AUTHORS
import db.tables.records.AuthorsRecord
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.time.LocalDate
import db.tables.Books.BOOKS
import db.tables.BookAuthors.BOOK_AUTHORS


@Repository
class AuthorRepository(private val dslContext: DSLContext) {
    /**
     * 新しい著者データをデータベースに挿入します。
     * @param authorDto 挿入する著者データを含むDTO
     * @return 挿入された著者のID
     */
    fun insertAuthor(authorDto: AuthorDto): Long? {
        val record: AuthorsRecord? = dslContext.insertInto(AUTHORS).set(AUTHORS.NAME, authorDto.name)
            .set(AUTHORS.BIRTH_DATE, authorDto.birthDate).returning(AUTHORS.ID).fetchOne()
        return record?.id
    }

    /**
     * 既存の著者データを部分的に更新します (PATCHセマンティクス)。
     * @param id 更新対象の著者ID
     * @param updates 更新するフィールド名と値のマップ。
     * @return 更新されたレコード数
     */
    fun updateAuthor(id: Long, updates: Map<String, Any?>): Int {
        if (updates.isEmpty()) {
            return 0
        }

        val jooqUpdateMap = mutableMapOf<Field<*>, Any?>()

        updates.forEach { (fieldName, value) ->
            when (fieldName) {
                "name" -> jooqUpdateMap[AUTHORS.NAME] = value as String?
                "birthDate" -> jooqUpdateMap[AUTHORS.BIRTH_DATE] = value as LocalDate?
                else -> throw IllegalArgumentException("不明なフィールド名: $fieldName")
            }
        }

        return dslContext.update(AUTHORS).set(jooqUpdateMap).where(AUTHORS.ID.eq(id)).execute()
    }

    /**
     * 指定された著者IDが存在するかどうかを確認します。
     * @param id 検索する著者ID
     * @return 存在する場合はtrue、しない場合はfalse
     */
    fun existsById(id: Long): Boolean {
        return dslContext.selectCount().from(AUTHORS).where(AUTHORS.ID.eq(id)).fetchOne(0, Long::class.java) ?: 0L > 0L
    }

    /**
     * 指定された著者IDに紐づく全ての書籍情報を取得します。
     * @param authorId 取得する書籍情報の著者ID
     * @return 著者IDに紐づく書籍情報のリスト。見つからない場合は空のリスト。
     */
    fun findBooksByAuthorId(authorId: Long): List<BookDto> {
        return dslContext.select(
            BOOKS.ID,
            BOOKS.TITLE,
            BOOKS.PRICE,
            BOOKS.PUBLICATION_STATUS
        )
            .from(BOOKS)
            .join(BOOK_AUTHORS).on(BOOKS.ID.eq(BOOK_AUTHORS.BOOK_ID))
            .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
            .fetchInto(BookDto::class.java)
    }

    /**
     * 指定されたIDの著者データを取得します。
     * @param id 取得対象の著者ID
     * @return 指定されたIDの著者データ。見つからない場合はnull。
     */
    fun findById(id: Long): AuthorDto? {
        return dslContext.selectFrom(AUTHORS)
            .where(AUTHORS.ID.eq(id))
            .fetchOneInto(AuthorDto::class.java)
    }
}
