package com.example.demo.repository

import com.example.demo.dto.BookAuthorDto
import db.tables.BookAuthors.BOOK_AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 書籍と著者の関連データへのデータベースアクセスを担うリポジトリクラス。
 *
 * @property dslContext jOOQのDSLコンテキスト。
 */
@Repository
class BookAuthorsRepository(
    private val dslContext: DSLContext,
) {
    /**
     * 指定された書籍IDに紐づく著者IDのリストを取得します。
     *
     * @param bookId 検索対象の書籍ID。
     * @return 著者IDのリスト。紐づく著者がない場合は空のリストを返します。
     */
    fun findAuthorIdsByBookId(bookId: Long): List<Long> =
        dslContext
            .select(BOOK_AUTHORS.AUTHOR_ID)
            .from(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .fetchInto(Long::class.java)

    /**
     * 書籍と著者の関連データをデータベースに登録します。
     *
     * @param bookAuthorDto 登録する関連データを含むDTO (bookIdとauthorId)。
     * @return 挿入されたレコード数 (通常は1)。
     */
    fun insertBookAuthor(bookAuthorDto: BookAuthorDto): Int =
        dslContext
            .insertInto(BOOK_AUTHORS)
            .set(BOOK_AUTHORS.BOOK_ID, bookAuthorDto.bookId)
            .set(BOOK_AUTHORS.AUTHOR_ID, bookAuthorDto.authorId)
            .execute()

    /**
     * 指定された書籍IDに紐づくすべての著者関連を削除します。
     *
     * @param bookId 関連を削除する書籍のID。
     * @return 削除されたレコード数。
     */
    fun deleteBookAuthorsByBookId(bookId: Long): Int =
        dslContext
            .deleteFrom(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .execute()
}
