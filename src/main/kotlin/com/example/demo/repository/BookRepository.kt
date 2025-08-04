package com.example.demo.repository

import com.example.demo.dto.BookDto
import org.jooq.DSLContext
import db.tables.Books.BOOKS // jOOQが生成するBooksテーブルの参照をインポート
import db.tables.records.BooksRecord // jOOQが生成するBooksRecordの参照をインポート
import org.springframework.stereotype.Repository
import org.jooq.Field

/**
 * 書籍データ操作用リポジトリクラス
 */
@Repository
class BookRepository(private val dslContext: DSLContext) {

    /**
     * 新しい書籍データをデータベースに挿入します。
     * @param bookDto 挿入する書籍データを含むDTO
     * @return 挿入された書籍のID
     */
    fun insertBook(bookDto: BookDto): Long? {
        // returning(BOOKS.ID) を使用して、自動生成されたIDを取得
        val record: BooksRecord? = dslContext.insertInto(BOOKS)
            .set(BOOKS.TITLE, bookDto.title)
            .set(BOOKS.PRICE, bookDto.price)
            .set(BOOKS.PUBLICATION_STATUS, bookDto.publicationStatus)
            .returning(BOOKS.ID)
            .fetchOne()
        return record?.id
    }

    /**
     * 既存の書籍データを部分的に更新します (PATCHセマンティクス)。
     * @param id 更新対象の書籍ID
     * @param updates 更新するフィールド名と値のマップ。
     * マップに存在しないフィールドは更新しません。
     * マップに存在するが値がnullのフィールドは、DBの対応するカラムをnullに更新します（nullableなカラムの場合）。
     * @return 更新されたレコード数
     */
    fun updateBook(id: Long, updates: Map<String, Any?>): Int {
        if (updates.isEmpty()) {
            return 0 // 更新するフィールドがない場合
        }

        // 更新するフィールドと値のマップをjOOQのFieldオブジェクトと値のペアに変換
        val jooqUpdateMap = mutableMapOf<Field<*>, Any?>()

        updates.forEach { (fieldName, value) ->
            when (fieldName) {
                "title" -> jooqUpdateMap[BOOKS.TITLE] = value as String?
                "price" -> jooqUpdateMap[BOOKS.PRICE] = value as Int?
                "publicationStatus" -> jooqUpdateMap[BOOKS.PUBLICATION_STATUS] = value as Int?
                else -> throw IllegalArgumentException("不明なフィールド名: $fieldName")
            }
        }

        // jOOQのupdate().set(Map<Field<?>, ?>) を使用して、動的にSET句を構築
        return dslContext.update(BOOKS)
            .set(jooqUpdateMap)
            .where(BOOKS.ID.eq(id))
            .execute()
    }

    /**
     * 指定されたIDの書籍データを取得します。
     * @param id 取得対象の書籍ID
     * @return 指定されたIDの書籍データ。見つからない場合はnull。
     */
    fun findById(id: Long): BookDto? {
        return dslContext.selectFrom(BOOKS)
            .where(BOOKS.ID.eq(id))
            .fetchOneInto(BookDto::class.java)
    }
}
