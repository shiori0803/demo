package com.example.demo.repository

import com.example.demo.BaseIntegrationTest
import com.example.demo.dto.BookDto
import db.tables.Books.BOOKS
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.util.stream.Stream

class BookRepositoryTest
    @Autowired
    constructor(
        private val bookRepository: BookRepository,
        private val ctx: DSLContext,
    ) : BaseIntegrationTest() {
        // 各テスト実行前にbooksテーブルをクリーンアップする
        @BeforeEach
        fun beforeEach() {
            ctx
                .truncate(BOOKS)
                .restartIdentity()
                .cascade()
                .execute()
        }

        // ----------------- テストデータとヘルパー関数 -----------------

        companion object {
            @JvmStatic
            fun updateBookTestData(): Stream<Arguments> {
                val initialBook = BookDto(id = 999L, title = "テスト駆動開発", price = 4000, publicationStatus = 0)
                return Stream.of(
                    Arguments.of(
                        "全項目を更新",
                        initialBook,
                        initialBook.copy(
                            title = "テスト駆動開発（改訂版）",
                            price = 5000,
                            publicationStatus = 1,
                        ),
                    ),
                    Arguments.of(
                        "titleのみを更新",
                        initialBook,
                        initialBook.copy(title = "テスト駆動開発（新版）"),
                    ),
                    Arguments.of(
                        "priceのみを更新",
                        initialBook,
                        initialBook.copy(price = 4500),
                    ),
                    Arguments.of(
                        "publicationStatusのみを更新",
                        initialBook,
                        initialBook.copy(publicationStatus = 1),
                    ),
                )
            }
        }

        /**
         * データベースのレコード数を取得するヘルパー関数
         */
        private fun getBookCountInDb(): Long = ctx.selectCount().from(BOOKS).fetchOne(0, Long::class.java) ?: 0L

        /**
         * テスト用の書籍データをDBに登録し、自動生成されたIDを反映したDTOを返すヘルパー関数
         */
        private fun insertAndGetBook(bookDto: BookDto): BookDto {
            val insertedBookDto = bookRepository.insertBook(bookDto)
            assertThat(insertedBookDto).isNotNull
            return insertedBookDto!!
        }

        // --- findById ファンクションのテスト ---
        @Test
        fun `findById 指定したデータが取得できた場合`() {
            // Given
            val bookDto = BookDto(id = null, title = "テスト駆動開発", price = 4000, publicationStatus = 0)
            val insertedBook = bookRepository.insertBook(bookDto)!!

            // When
            val foundBook = bookRepository.findById(insertedBook.id!!)

            // Then
            assertThat(foundBook).isNotNull()
            assertThat(foundBook).isEqualTo(insertedBook)
        }

        @Test
        fun `findById 指定したデータが取得できなかった場合`() {
            // Given
            val nonExistentId = 999L

            // When
            val foundBook = bookRepository.findById(nonExistentId)

            // Then
            assertThat(foundBook).isNull()
        }

        // --- insertBook ファンクションのテスト ---
        @Test
        fun `insertBook 正常に登録ができること`() {
            // Given
            val bookDto =
                BookDto(
                    id = null,
                    title = "Kotlin入門",
                    price = 3000,
                    publicationStatus = 0,
                )

            // When
            val insertedBookDto = bookRepository.insertBook(bookDto)

            // Then
            assertThat(insertedBookDto).isNotNull()
            val insertedBook = bookRepository.findById(insertedBookDto!!.id!!)
            assertThat(insertedBook).isEqualTo(insertedBookDto)
        }

        @Test
        fun `insertBook 0以下の値がpriceに登録できないこと`() {
            // Given
            val bookWithInvalidPrice = BookDto(id = null, title = "Invalid Price", price = -100, publicationStatus = 1)

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookRepository.insertBook(bookWithInvalidPrice)
            }
            assertThat(getBookCountInDb()).isEqualTo(0L)
        }

        @Test
        fun `insertBook 0, 1以外の値がpublication_statusに登録できないこと`() {
            // Given
            val bookWithInvalidStatus = BookDto(id = null, title = "Invalid Status", price = 100, publicationStatus = 2)

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookRepository.insertBook(bookWithInvalidStatus)
            }
            assertThat(getBookCountInDb()).isEqualTo(0L)
        }

        @Test
        fun `insertBook title, price, publication_statusが重複するレコードが登録できないこと`() {
            // Given
            val bookDto = BookDto(id = null, title = "Duplicate Book", price = 2000, publicationStatus = 1)
            bookRepository.insertBook(bookDto)

            val duplicateBookDto = BookDto(id = null, title = "Duplicate Book", price = 2000, publicationStatus = 1)

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookRepository.insertBook(duplicateBookDto)
            }
            // データベースのレコード数が1件であることを確認
            assertThat(getBookCountInDb()).isEqualTo(1L)
        }

        // --- updateBook ファンクションのテスト ---
        @ParameterizedTest(name = "{0}")
        @MethodSource("updateBookTestData")
        fun `updateBook 正常に更新ができること`(
            testName: String,
            initialData: BookDto,
            updatedData: BookDto,
        ) {
            // Given
            val bookInDb = insertAndGetBook(initialData.copy(id = null))

            val updatesMap = mutableMapOf<String, Any?>()
            if (updatedData.title != initialData.title) {
                updatesMap["title"] = updatedData.title
            }
            if (updatedData.price != initialData.price) {
                updatesMap["price"] = updatedData.price
            }
            if (updatedData.publicationStatus != initialData.publicationStatus) {
                updatesMap["publicationStatus"] = updatedData.publicationStatus
            }

            // When
            val updatedCount = bookRepository.updateBook(bookInDb.id!!, updatesMap)

            // Then
            assertThat(updatedCount).isEqualTo(1)
            val retrievedBook =
                ctx.selectFrom(BOOKS).where(BOOKS.ID.eq(bookInDb.id)).fetchOneInto(BookDto::class.java)
            assertThat(retrievedBook).isEqualTo(updatedData.copy(id = bookInDb.id))
        }

        @Test
        fun `updateBook 更新するデータがない場合、更新件数が0件であること`() {
            // Given
            val initialBook = BookDto(id = null, title = "テスト書籍", price = 1000, publicationStatus = 0)
            val bookInDb = insertAndGetBook(initialBook)
            val updatesMap = emptyMap<String, Any?>()

            // When
            val updatedCount = bookRepository.updateBook(bookInDb.id!!, updatesMap)

            // Then
            assertThat(updatedCount).isEqualTo(0)
            val retrievedBook =
                ctx.selectFrom(BOOKS).where(BOOKS.ID.eq(bookInDb.id)).fetchOneInto(BookDto::class.java)
            assertThat(retrievedBook).isEqualTo(initialBook.copy(id = bookInDb.id))
        }

        @Test
        fun `updateBook 存在しないIDを指定した場合、更新件数が0件であること`() {
            // Given
            val nonExistentId = 999L
            val updatesMap = mapOf("title" to "Updated Title")

            // When
            val updatedCount = bookRepository.updateBook(nonExistentId, updatesMap)

            // Then
            assertThat(updatedCount).isEqualTo(0)
        }

        @Test
        fun `updateBook 不明なフィールド名が入力された場合、IllegalArgumentExceptionをスローすること`() {
            // Given
            val initialBook = BookDto(id = null, title = "テスト書籍", price = 1000, publicationStatus = 0)
            val bookInDb = insertAndGetBook(initialBook)
            val invalidUpdatesMap = mapOf("invalidField" to "Invalid Value")

            // When & Then
            assertThrows<IllegalArgumentException> {
                bookRepository.updateBook(bookInDb.id!!, invalidUpdatesMap)
            }
        }

        @Test
        fun `updateBook 重複するデータに更新しようとした場合、DataIntegrityViolationExceptionをスローすること`() {
            // Given
            val bookA = insertAndGetBook(BookDto(id = null, title = "Book A", price = 1000, publicationStatus = 0))
            val bookB = insertAndGetBook(BookDto(id = null, title = "Book B", price = 2000, publicationStatus = 1))

            // Book Bの情報をBook Aと重複する内容に更新しようとする
            val updatesMap =
                mapOf(
                    "title" to bookA.title,
                    "price" to bookA.price,
                    "publicationStatus" to bookA.publicationStatus,
                )

            // When & Then
            assertThrows<DataIntegrityViolationException> {
                bookRepository.updateBook(bookB.id!!, updatesMap)
            }

            // データベースのレコード数が変わっていないことを確認
            assertThat(getBookCountInDb()).isEqualTo(2L)
        }
    }
