package com.example.demo.repository

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.BookDto
import db.tables.Authors.AUTHORS
import db.tables.BookAuthors.BOOK_AUTHORS
import db.tables.Books.BOOKS
import db.tables.records.AuthorsRecord
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AuthorRepository(
    private val dslContext: DSLContext,
) {
    /**
     * 指定されたIDの著者データを取得します。
     * @param id 取得対象の著者ID
     * @return 指定されたIDの著者データ。見つからない場合はnull。
     */
    fun findById(id: Long): AuthorDto? = dslContext.selectFrom(AUTHORS).where(AUTHORS.ID.eq(id)).fetchOneInto(AuthorDto::class.java)

    /**
     * 指定された著者IDに紐づく全ての書籍情報を取得します。
     * @param authorId 取得する書籍情報の著者ID
     * @return 著者IDに紐づく書籍情報のリスト。見つからない場合は空のリスト。
     */
    fun findBooksByAuthorId(authorId: Long): List<BookDto> =
        dslContext
            .select(
                BOOKS.ID,
                BOOKS.TITLE,
                BOOKS.PRICE,
                BOOKS.PUBLICATION_STATUS,
            ).from(BOOKS)
            .join(BOOK_AUTHORS)
            .on(BOOKS.ID.eq(BOOK_AUTHORS.BOOK_ID))
            .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
            .fetchInto(BookDto::class.java)

    /**
     * 新しい著者データをデータベースに挿入し、挿入された著者データを返します。
     * @param authorDto 挿入する著者データを含むDTO
     * @return 挿入された著者データ。挿入に失敗した場合はnull。
     */
    fun insertAuthor(authorDto: AuthorDto): AuthorDto? {
        val record: AuthorsRecord? =
            dslContext
                .insertInto(AUTHORS)
                .set(AUTHORS.NAME, authorDto.name)
                .set(AUTHORS.BIRTH_DATE, authorDto.birthDate)
                .returning()
                .fetchOne()
        // AuthorsRecordをAuthorDtoに変換して返却
        return record?.into(AuthorDto::class.java)
    }

    /**
     * 既存の著者データを部分的に更新します (PATCHセマンティクス)。
     * @param id 更新対象の著者ID
     * @param updates 更新するフィールド名と値のマップ。
     * @return 更新されたレコード数
     */
    fun updateAuthor(
        id: Long,
        updates: Map<String, Any?>,
    ): Int {
        if (updates.isEmpty()) {
            return 0
        }

        val jooqUpdateMap = mutableMapOf<Field<*>, Any?>()

        updates.forEach { (fieldName, value) ->
            when (fieldName) {
                "name" -> jooqUpdateMap[AUTHORS.NAME] = value as String?
                "birthDate" -> jooqUpdateMap[AUTHORS.BIRTH_DATE] = value as LocalDate?
                else -> throw IllegalArgumentException("不明なフィールド名が入力されました: $fieldName")
            }
        }

        return dslContext
            .update(AUTHORS)
            .set(jooqUpdateMap)
            .where(AUTHORS.ID.eq(id))
            .execute()
    }

    /**
     * 指定された著者IDのリストが何件DBに存在するかをチェックする
     * @param authorIds 検索する著者IDのリスト
     * @return 引数で渡された著者IDと合致するレコード件数
     */
    fun existsAllByIds(authorIds: List<Long>): Long =
        dslContext
            .select(AUTHORS.ID)
            .from(AUTHORS)
            .where(AUTHORS.ID.`in`(authorIds))
            .fetch()
            .size
            .toLong()
}
