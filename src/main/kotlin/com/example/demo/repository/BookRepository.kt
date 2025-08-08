package com.example.demo.repository

import com.example.demo.dto.BookDto
import db.tables.Books.BOOKS
import db.tables.records.BooksRecord
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository

/**
 * 書籍データへのデータベースアクセスを担うリポジトリクラス。
 *
 * @property dslContext jOOQのDSLコンテキスト。
 */
@Repository
class BookRepository(
    private val dslContext: DSLContext,
) {
    /**
     * 指定されたIDの書籍データを取得します。
     *
     * @param id 取得対象の書籍ID。
     * @return 指定されたIDの書籍データ。見つからない場合はnull。
     */
    fun findById(id: Long): BookDto? =
        dslContext
            .selectFrom(BOOKS)
            .where(BOOKS.ID.eq(id))
            .fetchOneInto(BookDto::class.java)

    /**
     * 新しい書籍データをデータベースに挿入し、挿入された書籍データを返します。
     *
     * @param bookDto 挿入する書籍データを含むDTO。
     * @return 挿入された書籍データ。挿入に失敗した場合はnull。
     */
    fun insertBook(bookDto: BookDto): BookDto? {
        val record: BooksRecord? =
            dslContext
                .insertInto(BOOKS)
                .set(BOOKS.TITLE, bookDto.title)
                .set(BOOKS.PRICE, bookDto.price)
                .set(BOOKS.PUBLICATION_STATUS, bookDto.publicationStatus)
                .returning()
                .fetchOne()
        return record?.into(BookDto::class.java)
    }

    /**
     * 既存の書籍データを部分的に更新します (PATCHセマンティクス)。
     *
     * @param id 更新対象の書籍ID。
     * @param updates 更新するフィールド名と値のマップ。マップに存在しないフィールドは更新しません。
     * @return 更新されたレコード数。
     */
    fun updateBook(
        id: Long,
        updates: Map<String, Any?>,
    ): Int {
        val jooqUpdateMap = mutableMapOf<Field<*>, Any?>()

        updates.forEach { (fieldName, value) ->
            when (fieldName) {
                "title" -> jooqUpdateMap[BOOKS.TITLE] = value as String?
                "price" -> jooqUpdateMap[BOOKS.PRICE] = value as Int?
                "publicationStatus" -> jooqUpdateMap[BOOKS.PUBLICATION_STATUS] = value as Int?
                else -> throw IllegalArgumentException("不明なフィールド名が入力されました: $fieldName")
            }
        }

        return dslContext
            .update(BOOKS)
            .set(jooqUpdateMap)
            .where(BOOKS.ID.eq(id))
            .execute()
    }
}
