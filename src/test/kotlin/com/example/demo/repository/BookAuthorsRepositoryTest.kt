package com.example.demo.repository

import com.example.demo.BaseIntegrationTest
import com.example.demo.dto.BookAuthorDto
import db.tables.Authors.AUTHORS
import db.tables.BookAuthors.BOOK_AUTHORS
import db.tables.Books.BOOKS
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class BookAuthorsRepositoryTest
    @Autowired
    constructor(
        private val bookAuthorsRepository: BookAuthorsRepository,
        private val ctx: DSLContext,
    ) : BaseIntegrationTest() {
        @BeforeEach
        fun beforeEach() {
            // 各テスト実行前にbooks, authors, book_authors テーブルをクリーンアップ
            ctx.truncate(BOOK_AUTHORS).cascade().execute()
            ctx
                .truncate(BOOKS)
                .restartIdentity()
                .cascade()
                .execute()
            ctx
                .truncate(AUTHORS)
                .restartIdentity()
                .cascade()
                .execute()
        }

        // ----------------- テストデータとヘルパー関数 -----------------
        private fun insertBook(): Long? =
            ctx
                .insertInto(BOOKS)
                .set(BOOKS.TITLE, "テスト書籍")
                .set(BOOKS.PRICE, 1000)
                .set(BOOKS.PUBLICATION_STATUS, 0)
                .returning(BOOKS.ID)
                .fetchOne()
                ?.id

        private fun insertAuthor(name: String): Long? =
            ctx
                .insertInto(AUTHORS)
                .set(AUTHORS.NAME, name)
                .set(AUTHORS.BIRTH_DATE, LocalDate.of(1990, 1, 1))
                .returning(AUTHORS.ID)
                .fetchOne()
                ?.id

        private fun getBookAuthorCountInDb(): Long = ctx.selectCount().from(BOOK_AUTHORS).fetchOne(0, Long::class.java) ?: 0L

        // --- findAuthorIdsByBookId ファンクションのテスト ---
        @Test
        fun `findAuthorIdsByBookId 指定した書籍IDに紐づく著者IDリストを取得できる場合`() {
            // Given
            val bookId = insertBook()!!
            val authorId1 = insertAuthor("テスト著者1")!!
            val authorId2 = insertAuthor("テスト著者2")!!
            bookAuthorsRepository.insertBookAuthor(BookAuthorDto(bookId = bookId, authorId = authorId1))
            bookAuthorsRepository.insertBookAuthor(BookAuthorDto(bookId = bookId, authorId = authorId2))

            // When
            val result = bookAuthorsRepository.findAuthorIdsByBookId(bookId)

            // Then
            assertThat(result).containsExactlyInAnyOrder(authorId1, authorId2)
        }

        @Test
        fun `findAuthorIdsByBookId 紐づく著者がない書籍IDを指定した場合、空のリストを返却すること`() {
            // Given
            val bookId = insertBook()!!

            // When
            val result = bookAuthorsRepository.findAuthorIdsByBookId(bookId)

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `findAuthorIdsByBookId 存在しない書籍IDを指定した場合、空のリストを返却すること`() {
            // Given
            val nonExistentBookId = 999L

            // When
            val result = bookAuthorsRepository.findAuthorIdsByBookId(nonExistentBookId)

            // Then
            assertThat(result).isEmpty()
        }

        // --- insertBookAuthor ファンクションのテスト ---
        @Test
        fun `insertBookAuthor 正常に登録ができた場合`() {
            // Given
            val bookId = insertBook()!!
            val authorId = insertAuthor("新規著者")!!
            val bookAuthorDto = BookAuthorDto(bookId = bookId, authorId = authorId)

            // When
            val insertedCount = bookAuthorsRepository.insertBookAuthor(bookAuthorDto)

            // Then
            assertThat(insertedCount).isEqualTo(1)
            assertThat(getBookAuthorCountInDb()).isEqualTo(1L)
        }

        @Test
        fun `insertBookAuthor 既に存在する複合主キーで登録しようとした場合`() {
            // Given
            val bookId = insertBook()!!
            val authorId = insertAuthor("新規著者")!!
            val bookAuthorDto = BookAuthorDto(bookId = bookId, authorId = authorId)
            bookAuthorsRepository.insertBookAuthor(bookAuthorDto)

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
            assertThat(getBookAuthorCountInDb()).isEqualTo(1L)
        }

        @Test
        fun `insertBookAuthor 外部キー制約に違反するデータを登録しようとした場合`() {
            // Given
            val nonExistentBookId = 999L
            val nonExistentAuthorId = 999L
            val bookAuthorDto = BookAuthorDto(bookId = nonExistentBookId, authorId = nonExistentAuthorId)

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
            assertThat(getBookAuthorCountInDb()).isEqualTo(0L)
        }

        // --- deleteBookAuthorsByBookId ファンクションのテスト ---
        @Test
        fun `deleteBookAuthorsByBookId 指定した書籍IDに紐づく著者関連を正常に削除できること`() {
            // Given
            val bookId = insertBook()!!
            val authorId1 = insertAuthor("新規著者1")!!
            val authorId2 = insertAuthor("新規著者2")!!
            bookAuthorsRepository.insertBookAuthor(BookAuthorDto(bookId = bookId, authorId = authorId1))
            bookAuthorsRepository.insertBookAuthor(BookAuthorDto(bookId = bookId, authorId = authorId2))
            assertThat(getBookAuthorCountInDb()).isEqualTo(2L)

            // When
            val deletedCount = bookAuthorsRepository.deleteBookAuthorsByBookId(bookId)

            // Then
            assertThat(deletedCount).isEqualTo(2)
            assertThat(getBookAuthorCountInDb()).isEqualTo(0L)
        }

        @Test
        fun `deleteBookAuthorsByBookId 削除対象のレコードが存在しない場合、0が返却されること`() {
            // Given
            val nonExistentBookId = 999L
            insertBook()
            insertAuthor("新規著者")

            // When
            val deletedCount = bookAuthorsRepository.deleteBookAuthorsByBookId(nonExistentBookId)

            // Then
            assertThat(deletedCount).isEqualTo(0)
            assertThat(getBookAuthorCountInDb()).isEqualTo(0L)
        }
    }
