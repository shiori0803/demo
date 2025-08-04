package com.example.demo.repository

import com.example.demo.dto.BookAuthorDto
import org.jooq.DSLContext
import db.tables.BookAuthors.BOOK_AUTHORS // jOOQが生成するBookAuthorsテーブルの参照をインポート
import org.springframework.stereotype.Repository

/**
 * 書籍と著者の関連データ操作用リポジトリクラス
 */
@Repository
class BookAuthorsRepository(private val dslContext: DSLContext) {

    /**
     * 書籍と著者の関連データをデータベースに挿入します。
     * @param bookAuthorDto 挿入する関連データを含むDTO (bookIdとauthorId)
     * @return 挿入されたレコード数 (通常は1)
     */
    fun insertBookAuthor(bookAuthorDto: BookAuthorDto): Int {
        // book_authorsテーブルは複合主キーなので、returning()は不要（自動生成IDがないため）
        return dslContext.insertInto(BOOK_AUTHORS)
            .set(BOOK_AUTHORS.BOOK_ID, bookAuthorDto.bookId)
            .set(BOOK_AUTHORS.AUTHOR_ID, bookAuthorDto.authorId)
            .execute() // 挿入されたレコード数を返す
    }

    /**
     * 指定された書籍IDに紐づく全ての著者関連を削除します。
     * @param bookId 関連を削除する書籍のID
     * @return 削除されたレコード数
     */
    fun deleteBookAuthorsByBookId(bookId: Long): Int {
        return dslContext.deleteFrom(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .execute()
    }

    /**
     * 指定された書籍IDに紐づく著者関連を全て削除し、新しい著者関連を挿入します。
     * これは、書籍の著者リストを「置き換える」更新操作に相当します。
     * @param bookId 更新対象の書籍ID
     * @param newAuthorIds 新しく関連付ける著者IDのリスト
     * @return 挿入された新しい関連の総数
     */
    fun updateBookAuthors(bookId: Long, newAuthorIds: List<Long>): Int {
        // 1. 既存の関連を全て削除
        deleteBookAuthorsByBookId(bookId)

        // 2. 新しい関連を挿入
        var insertedCount = 0
        newAuthorIds.forEach { authorId ->
            val bookAuthorDto = BookAuthorDto(bookId = bookId, authorId = authorId)
            insertedCount += insertBookAuthor(bookAuthorDto)
        }
        return insertedCount
    }
}
