package com.example.demo.service

import com.example.demo.dto.BookDto
import com.example.demo.exception.InvalidPublicationStatusChangeException
import com.example.demo.exception.ItemAlreadyExistsException
import com.example.demo.exception.ItemNotFoundException
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.BookAuthorsRepository
import com.example.demo.repository.BookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException

class BookServiceTest {
    private val bookRepository: BookRepository = mockk()
    private val authorRepository: AuthorRepository = mockk()
    private val bookAuthorsRepository: BookAuthorsRepository = mockk()

    private lateinit var bookService: BookService

    @BeforeEach
    fun setUp() {
        bookService = BookService(bookRepository, authorRepository, bookAuthorsRepository)
    }

    // --- registerBook メソッドのテスト ---
    @Test
    fun `registerBook 正常に書籍情報が登録できること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Test Book", price = 1000, publicationStatus = 1)
        val authorIds = listOf(1L, 2L)
        val insertedBookDto = bookDto.copy(id = 10L)

        // モックの振る舞いを定義
        every { authorRepository.existsAllByIds(authorIds) } returns authorIds.size.toLong()
        every { bookRepository.insertBook(bookDto) } returns insertedBookDto
        every { bookAuthorsRepository.insertBookAuthor(any()) } returns 1
        every { bookAuthorsRepository.findAuthorIdsByBookId(insertedBookDto.id!!) } returns authorIds

        // When
        val result = bookService.registerBook(bookDto, authorIds)

        // Then
        assertThat(result.id).isEqualTo(insertedBookDto.id)
        assertThat(result.title).isEqualTo(insertedBookDto.title)
        assertThat(result.price).isEqualTo(insertedBookDto.price)
        assertThat(result.publicationStatus).isEqualTo(insertedBookDto.publicationStatus)
        assertThat(result.authorIds).containsExactlyInAnyOrderElementsOf(authorIds)

        // 依存メソッドが期待通りに呼び出されたか検証
        verify(exactly = 1) { authorRepository.existsAllByIds(authorIds) }
        verify(exactly = 1) { bookRepository.insertBook(bookDto) }
        authorIds.forEach { authorId ->
            verify(exactly = 1) {
                bookAuthorsRepository.insertBookAuthor(
                    com.example.demo.dto.BookAuthorDto(
                        insertedBookDto.id!!,
                        authorId,
                    ),
                )
            }
        }
        verify(exactly = 1) { bookAuthorsRepository.findAuthorIdsByBookId(insertedBookDto.id!!) }
    }

    @Test
    fun `registerBook 著者IDリストが空でIllegalArgumentExceptionをスローすること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Test Book", price = 1000, publicationStatus = 1)
        val authorIds = emptyList<Long>()

        // when & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                bookService.registerBook(bookDto, authorIds)
            }
        assertThat(exception.message).isEqualTo("validation.authorIds.size.min")

        // 依存メソッドが呼び出されないことを確認
        verify(exactly = 0) { authorRepository.existsAllByIds(any()) }
        verify(exactly = 0) { bookRepository.insertBook(any()) }
        verify(exactly = 0) { bookAuthorsRepository.insertBookAuthor(any()) }
    }

    @Test
    fun `registerBook 著者IDが見つからずItemNotFoundExceptionをスローすること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Test Book", price = 1000, publicationStatus = 1)
        val authorIds = listOf(1L, 999L)

        // 著者IDが存在しないようにモックを設定（サイズが一致しない）
        every { authorRepository.existsAllByIds(authorIds) } returns 1

        // When & Then
        val exception =
            assertThrows<ItemNotFoundException> {
                bookService.registerBook(bookDto, authorIds)
            }
        assertThat(exception.itemType).isEqualTo("著者ID")

        // bookRepository.insertBookは呼び出されないことを確認
        verify(exactly = 1) { authorRepository.existsAllByIds(authorIds) }
        verify(exactly = 0) { bookRepository.insertBook(any()) }
    }

    @Test
    fun `registerBook 同一の書籍がすでに登録済みでItemAlreadyExistsExceptionがスローされること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Existing Book", price = 1000, publicationStatus = 1)
        val authorIds = listOf(1L)

        // 著者IDは存在するとする
        every { authorRepository.existsAllByIds(authorIds) } returns authorIds.size.toLong()
        // bookRepository.insertBookがDataIntegrityViolationExceptionをスローするように設定
        every { bookRepository.insertBook(any()) } throws DataIntegrityViolationException("Duplicate entry for book")

        // When & Then
        val exception =
            assertThrows<ItemAlreadyExistsException> {
                bookService.registerBook(bookDto, authorIds)
            }
        assertThat(exception.itemType).isEqualTo("書籍")

        verify(exactly = 1) { authorRepository.existsAllByIds(authorIds) }
        verify(exactly = 1) { bookRepository.insertBook(bookDto) }
    }

    @Test
    fun `registerBook insertedBookDtoがnullでIllegalStateExceptionをスローすること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Test Book", price = 1000, publicationStatus = 1)
        val authorIds = listOf(1L)

        every { authorRepository.existsAllByIds(authorIds) } returns authorIds.size.toLong()
        every { bookRepository.insertBook(any()) } returns null // nullを返すように設定

        // When & Then
        assertThrows<IllegalStateException> {
            bookService.registerBook(bookDto, authorIds)
        }

        // 著者IDのチェックは成功し、書籍の登録が試みられたことを確認
        verify(exactly = 1) { authorRepository.existsAllByIds(authorIds) }
        verify(exactly = 1) { bookRepository.insertBook(bookDto) }
    }

    @Test
    fun `registerBook bookAuthorsRepositoryへの登録失敗時にIllegalStateExceptionをスローすること`() {
        // Given
        val bookDto = BookDto(id = null, title = "Rollback Book", price = 1000, publicationStatus = 1)
        val authorIds = listOf(1L, 2L)
        val insertedBookDto = bookDto.copy(id = 10L)

        // モックの振る舞いを定義
        every { authorRepository.existsAllByIds(authorIds) } returns authorIds.size.toLong()
        every { bookRepository.insertBook(bookDto) } returns insertedBookDto
        every {
            bookAuthorsRepository.insertBookAuthor(
                com.example.demo.dto
                    .BookAuthorDto(10L, 1L),
            )
        } returns 1
        every {
            bookAuthorsRepository.insertBookAuthor(
                com.example.demo.dto.BookAuthorDto(
                    10L,
                    2L,
                ),
            )
        } throws IllegalStateException("book_authors registration failed")

        // When & Then
        assertThrows<IllegalStateException> {
            bookService.registerBook(bookDto, authorIds)
        }

        // 検証
        verify(exactly = 1) { bookRepository.insertBook(bookDto) }
        verify(exactly = 2) { bookAuthorsRepository.insertBookAuthor(any()) }
    }

    // --- updateBook メソッドのテスト ---
    private val bookId = 1L
    private val originalBookDto = BookDto(id = bookId, title = "Original Title", price = 100, publicationStatus = 0)
    private val originalAuthorIds = listOf(1L)

    @Test
    fun `updateBook 正常に更新が完了しBookWithAuthorsResponseを返却すること`() {
        // Given
        val updates = mapOf("title" to "Updated Title", "price" to 200)
        val newAuthorIds = listOf(2L, 3L)
        val updatedBookDto = originalBookDto.copy(title = "Updated Title", price = 200)

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { bookRepository.updateBook(bookId, updates) } returns 1
        every { authorRepository.existsAllByIds(newAuthorIds) } returns newAuthorIds.size.toLong()
        every { bookAuthorsRepository.deleteBookAuthorsByBookId(bookId) } returns originalAuthorIds.size
        every { bookAuthorsRepository.insertBookAuthor(any()) } returns 1
        every { bookRepository.findById(bookId) } returns updatedBookDto
        every { bookAuthorsRepository.findAuthorIdsByBookId(bookId) } returns newAuthorIds

        // When
        val result = bookService.updateBook(bookId, updates, newAuthorIds)

        // Then
        assertThat(result.id).isEqualTo(bookId)
        assertThat(result.title).isEqualTo(updatedBookDto.title)
        assertThat(result.price).isEqualTo(updatedBookDto.price)
        assertThat(result.authorIds).containsExactlyInAnyOrderElementsOf(newAuthorIds)

        verify(exactly = 2) { bookRepository.findById(bookId) }
        verify(exactly = 1) { bookRepository.updateBook(bookId, updates) }
        verify(exactly = 1) { authorRepository.existsAllByIds(newAuthorIds) }
        verify(exactly = 1) { bookAuthorsRepository.deleteBookAuthorsByBookId(bookId) }
        verify(exactly = newAuthorIds.size) { bookAuthorsRepository.insertBookAuthor(any()) }
    }

    @Test
    fun `updateBook 指定の書籍IDが存在しない場合、ItemNotFoundExceptionをスローすること`() {
        // Given
        val nonExistentId = 999L
        val updates = mapOf("title" to "Updated Title")

        every { bookRepository.findById(nonExistentId) } returns null

        // When & Then
        assertThrows<ItemNotFoundException> {
            bookService.updateBook(nonExistentId, updates, null)
        }
        verify(exactly = 1) { bookRepository.findById(nonExistentId) }
        verify(exactly = 0) { bookRepository.updateBook(any(), any()) }
    }

    @Test
    fun `updateBook PublicationStatusを出版済みから未出版に更新しようとした場合、InvalidPublicationStatusChangeExceptionをスローすること`() {
        // Given
        val publishedBook = originalBookDto.copy(publicationStatus = 1)
        val updates = mapOf("publicationStatus" to 0)

        every { bookRepository.findById(bookId) } returns publishedBook

        // When & Then
        assertThrows<InvalidPublicationStatusChangeException> {
            bookService.updateBook(bookId, updates, null)
        }
        verify(exactly = 1) { bookRepository.findById(bookId) }
        verify(exactly = 0) { bookRepository.updateBook(any(), any()) }
    }

    @Test
    fun `updateBook booksテーブル更新用マップが空の場合、booksテーブルの更新処理がスキップされること`() {
        // Given
        val updates = emptyMap<String, Any?>()
        val newAuthorIds = listOf(2L)

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { authorRepository.existsAllByIds(newAuthorIds) } returns newAuthorIds.size.toLong()
        every { bookAuthorsRepository.deleteBookAuthorsByBookId(bookId) } returns originalAuthorIds.size
        every { bookAuthorsRepository.insertBookAuthor(any()) } returns 1
        every { bookRepository.findById(bookId) } returns originalBookDto
        every { bookAuthorsRepository.findAuthorIdsByBookId(bookId) } returns newAuthorIds

        // When
        bookService.updateBook(bookId, updates, newAuthorIds)

        // Then
        verify(exactly = 0) { bookRepository.updateBook(any(), any()) }
        verify(exactly = 1) { bookAuthorsRepository.deleteBookAuthorsByBookId(bookId) }
        verify(exactly = newAuthorIds.size) { bookAuthorsRepository.insertBookAuthor(any()) }
    }

    @Test
    fun `updateBook 書籍データ更新後の更新件数が0件の場合、ItemNotFoundExceptionをスローすること`() {
        // Given
        val updates = mapOf("title" to "Updated Title")

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { bookRepository.updateBook(bookId, updates) } returns 0

        // When & Then
        assertThrows<ItemNotFoundException> {
            bookService.updateBook(bookId, updates, null)
        }
        verify(exactly = 1) { bookRepository.findById(bookId) }
        verify(exactly = 1) { bookRepository.updateBook(bookId, updates) }
        verify(exactly = 0) { authorRepository.existsAllByIds(any()) }
    }

    @Test
    fun `updateBook 書籍に紐づく著者の項目がnullもしくは空の場合、book_authorsテーブルの更新処理がスキップされること`() {
        // Given
        val updates = mapOf("title" to "Updated Title")
        val updatedBookDto = originalBookDto.copy(title = "Updated Title")

        // nullの場合のテスト
        val newAuthorIds: List<Long>? = null

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { bookRepository.updateBook(bookId, updates) } returns 1
        every { bookRepository.findById(bookId) } returns updatedBookDto
        every { bookAuthorsRepository.findAuthorIdsByBookId(bookId) } returns originalAuthorIds

        // When
        bookService.updateBook(bookId, updates, newAuthorIds)

        // Then
        verify(exactly = 1) { bookRepository.updateBook(bookId, updates) }
        verify(exactly = 0) { bookAuthorsRepository.deleteBookAuthorsByBookId(any()) }
        verify(exactly = 0) { bookAuthorsRepository.insertBookAuthor(any()) }

        // Given
        val emptyAuthorIds = emptyList<Long>() // 空リストの場合のテスト

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { bookRepository.updateBook(bookId, updates) } returns 1
        every { bookRepository.findById(bookId) } returns updatedBookDto
        every { bookAuthorsRepository.findAuthorIdsByBookId(bookId) } returns emptyAuthorIds

        // When
        bookService.updateBook(bookId, updates, emptyAuthorIds)

        // Then
        // 2回目の updateBook呼び出しを考慮して、verifyのカウントを増やす
        verify(exactly = 2) { bookRepository.updateBook(bookId, updates) }
        // 著者関連の更新はどちらの場合もスキップされることを確認
        verify(exactly = 0) { bookAuthorsRepository.deleteBookAuthorsByBookId(any()) }
        verify(exactly = 0) { bookAuthorsRepository.insertBookAuthor(any()) }
    }

    @Test
    fun `updateBook 書籍に紐づく著者IDが存在しない場合、ItemNotFoundExceptionをスローすること`() {
        // Given
        val updates = emptyMap<String, Any?>()
        val newAuthorIds = listOf(999L)

        every { bookRepository.findById(bookId) } returns originalBookDto
        every { authorRepository.existsAllByIds(newAuthorIds) } returns 0 // 存在しないことを示す

        // When & Then
        assertThrows<ItemNotFoundException> {
            bookService.updateBook(bookId, updates, newAuthorIds)
        }

        // 検証
        verify(exactly = 1) { bookRepository.findById(bookId) }
        verify(exactly = 1) { authorRepository.existsAllByIds(newAuthorIds) }
        verify(exactly = 0) { bookRepository.updateBook(any(), any()) }
    }
}
