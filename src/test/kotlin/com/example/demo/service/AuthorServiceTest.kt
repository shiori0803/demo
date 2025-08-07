package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.BookDto
import com.example.demo.dto.response.AuthorResponse
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.dto.response.BookResponse
import com.example.demo.exception.ItemAlreadyExistsException
import com.example.demo.exception.ItemNotFoundException
import com.example.demo.exception.UnexpectedException
import com.example.demo.repository.AuthorRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class AuthorServiceTest {
    // AuthorRepositoryをモック化
    private val authorRepository: AuthorRepository = mockk()

    // テスト対象のAuthorService
    private lateinit var authorService: AuthorService

    @BeforeEach
    fun setUp() {
        // 各テストの前にAuthorServiceのインスタンスを再作成し、モックを注入
        authorService = AuthorService(authorRepository)
    }

    // --- getAuthorWithBooksResponse ファンクションのテスト ---

    @Test
    fun `getAuthorWithBooksResponse 正常にデータが取得できた場合`() {
        // Given
        val authorId = 1L
        // mockで返される値の設定
        val authorName = "Haruki Murakami"
        val authorBirthDate = LocalDate.now().minusDays(1)

        val mockAuthorDto = AuthorDto(id = authorId, name = authorName, birthDate = authorBirthDate)
        val mockBookDtoList =
            listOf(
                BookDto(id = 101L, title = "Norwegian Wood", price = 2000, publicationStatus = 1),
                BookDto(id = 102L, title = "Kafka on the Shore", price = 2500, publicationStatus = 1),
            )

        // 期待値の設定
        val expectedAuthorResponse =
            AuthorResponse(id = authorId, name = authorName, birthDate = authorBirthDate)
        // mockBookDtoListに含まれる全てのBookDtoオブジェクトをBookResponseに変換してexpectedBookResponsesに代入
        val expectedBookResponses =
            mockBookDtoList.map {
                BookResponse(it.id, it.title, it.price, it.publicationStatus)
            }
        val expectedResponse = AuthorWithBooksResponse(author = expectedAuthorResponse, books = expectedBookResponses)

        // When
        // AuthorRepositoryクラスのファンクションを呼び出しをした時の結果をmock化
        every { authorRepository.findById(authorId) } returns mockAuthorDto
        every { authorRepository.findBooksByAuthorId(authorId) } returns mockBookDtoList

        // テスト対象のファンクションを呼び出し
        val result = authorService.getAuthorWithBooksResponse(authorId)

        // Then
        // ファンクション実行結果が期待値と同一であることを確認
        assertThat(result).isEqualTo(expectedResponse)
        // authorRepository.findById()の呼び出しが1回あったことを確認
        verify(exactly = 1) { authorRepository.findById(authorId) }
        // authorRepository.findBooksByAuthorId()の呼び出しが1回あったことを確認
        verify(exactly = 1) { authorRepository.findBooksByAuthorId(authorId) }
    }

    @Test
    fun `getAuthorWithBooksResponse 存在しない著者を指定してItemNotFoundExceptionがスローされる場合`() {
        // Given
        val authorId = 999L

        // When
        every { authorRepository.findById(authorId) } returns null

        // Then
        // ItemNotFoundExceptionがスローされることの確認
        val exception =
            assertThrows<ItemNotFoundException> {
                authorService.getAuthorWithBooksResponse(authorId)
            }
        assertThat(exception.itemType).isEqualTo("著者ID")
        assertThat(exception.message).isEqualTo("error.item.not.found")
        verify(exactly = 1) { authorRepository.findById(authorId) }
        // 処理が途中で終了するためauthorRepository.findBooksByAuthorId()は呼び出されないことの確認
        verify(exactly = 0) { authorRepository.findBooksByAuthorId(any()) }
    }

    // --- registerAuthor ファンクションのテスト ---

    @Test
    fun `registerAuthor 正常に登録できた場合`() {
        // Given
        val authorDto =
            AuthorDto(
                id = null,
                name = "Test User",
                birthDate = LocalDate.of(2000, 1, 1),
            )
        val generatedAuthorDto = authorDto.copy(id = 123L)
        val expectedAuthorResponse = AuthorResponse(id = 123L, name = "Test User", birthDate = LocalDate.of(2000, 1, 1))

        // When
        // insertAuthorはAuthorDtoを返すように変更されている
        every { authorRepository.insertAuthor(any()) } returns generatedAuthorDto

        val result = authorService.registerAuthor(authorDto)

        // Then
        assertThat(result).isEqualTo(expectedAuthorResponse)
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    @Test
    fun `registerAuthor 同一の著者が登録済みでItemAlreadyExistsExceptionがスローされた場合`() {
        // Given
        val authorDto =
            AuthorDto(
                id = null,
                name = "Duplicate Author",
                birthDate = LocalDate.of(1990, 5, 10),
            )

        // When
        every { authorRepository.insertAuthor(any()) } throws DataIntegrityViolationException("Duplicate entry for author")

        // Then
        val exception =
            assertThrows<ItemAlreadyExistsException> {
                authorService.registerAuthor(authorDto)
            }
        assertThat(exception.itemType).isEqualTo("著者")
        assertThat(exception.message).isEqualTo("error.item.already.exists")
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    @Test
    fun `registerAuthor insertedAuthorDtoがnullでIllegalStateExceptionがスローされた場合`() {
        // Given
        val authorDto =
            AuthorDto(
                id = null,
                name = "Test Author",
                birthDate = LocalDate.of(1990, 5, 10),
            )
        every { authorRepository.insertAuthor(any()) } returns null

        // When & Then
        val exception =
            assertThrows<IllegalStateException> {
                authorService.registerAuthor(authorDto)
            }
        // 例外メッセージが空でないこと
        assertThat(exception.message).isNotNull()
        // insertAuthorが呼び出されたことを検証
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    // --- partialUpdateAuthor ファンクションのテスト ---

    @Test
    fun `partialUpdateAuthor 正常に更新ができる場合`() {
        // Given
        val authorId = 1L
        val updates = mapOf("name" to "Updated Name")
        val updatedAuthorDto = AuthorDto(id = authorId, name = "Updated Name", birthDate = LocalDate.now())

        // When
        every { authorRepository.updateAuthor(authorId, updates) } returns 1
        every { authorRepository.findById(authorId) } returns updatedAuthorDto

        val result = authorService.partialUpdateAuthor(authorId, updates)

        // Then
        assertThat(result).isEqualTo(updatedAuthorDto)
        verify(exactly = 1) { authorRepository.updateAuthor(authorId, updates) }
        verify(exactly = 1) { authorRepository.findById(authorId) }
    }

    @Test
    fun `partialUpdateAuthor updatesマップが空でIllegalArgumentExceptionをスローする場合`() {
        // Given
        val authorId = 1L
        val updates = emptyMap<String, Any?>()

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                authorService.partialUpdateAuthor(authorId, updates)
            }
        assertThat(exception.message).isEqualTo("error.nothing.update")
        verify(exactly = 0) { authorRepository.updateAuthor(any(), any()) }
    }

    @Test
    fun `partialUpdateAuthor 更新件数が1件以上、しかしauthorRepositoryfindByIdで著者IDが取れずUnexpectedExceptionをスローする場合`() {
        // Given
        val authorId = 1L
        val updates = mapOf("name" to "Updated Name")

        // When
        every { authorRepository.updateAuthor(authorId, updates) } returns 1
        every { authorRepository.findById(authorId) } returns null

        // Then
        val exception =
            assertThrows<UnexpectedException> {
                authorService.partialUpdateAuthor(authorId, updates)
            }
        assertThat(exception.message).isEqualTo("著者情報更新APIにて著者データの登録が完了しましたが、著者IDが取得できません。")
        verify(exactly = 1) { authorRepository.updateAuthor(authorId, updates) }
        verify(exactly = 1) { authorRepository.findById(authorId) }
    }

    @Test
    fun `partialUpdateAuthor 更新件数が0件以下でItemNotFoundExceptionをスローする場合`() {
        // Given
        val authorId = 999L
        val updates = mapOf("name" to "Updated Name")

        // When
        every { authorRepository.updateAuthor(authorId, updates) } returns 0

        // Then
        val exception =
            assertThrows<ItemNotFoundException> {
                authorService.partialUpdateAuthor(authorId, updates)
            }
        assertThat(exception.itemType).isEqualTo("著者ID")
        assertThat(exception.message).isEqualTo("error.item.not.found")
        verify(exactly = 1) { authorRepository.updateAuthor(authorId, updates) }
        verify(exactly = 0) { authorRepository.findById(any()) }
    }
}
