package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.exception.AuthorAlreadyExistsException
import com.example.demo.exception.AuthorNotFoundException
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

    /**
     * 正常なデータ登録の確認
     */
    @Test
    fun `registerAuthor should return AuthorDto with generated ID on successful insertion`() {
        // Given
        val authorDto = AuthorDto(
            id = null,
            name = "Test User",
            birthDate = LocalDate.of(2000, 1, 1)
        )
        val generatedId = 123L

        // When
        // authorRepository.insertAuthorが呼ばれたら、generatedIdを返すようにモックを設定
        every { authorRepository.insertAuthor(any()) } returns generatedId

        val result = authorService.registerAuthor(authorDto)

        // Then
        // 期待される結果と実際の結果を検証
        assertThat(result.id).isEqualTo(generatedId)
        assertThat(result.name).isEqualTo(authorDto.name)
        assertThat(result.birthDate).isEqualTo(authorDto.birthDate)
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) } // insertAuthorが1回呼ばれたことを検証
    }

    /**
     * DBの制約に違反した場合の確認（重複データ登録）
     */
    @Test
    fun `registerAuthor should throw AuthorAlreadyExistsException when DataIntegrityViolationException occurs`() {
        // Given
        val authorDto = AuthorDto(
            id = null,
            name = "Duplicate Author",
            birthDate = LocalDate.of(1990, 5, 10)
        )

        // When
        // authorRepository.insertAuthorが呼ばれたら、DataIntegrityViolationExceptionをスローするようにモックを設定
        every { authorRepository.insertAuthor(any()) } throws DataIntegrityViolationException("Duplicate entry for author")

        // Then
        // AuthorAlreadyExistsExceptionがスローされることを検証
        assertThrows<AuthorAlreadyExistsException> {
            authorService.registerAuthor(authorDto)
        }
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    // --- partialUpdateAuthor メソッドのテスト ---

    @Test
    fun `partialUpdateAuthor should return updated AuthorDto on successful update`() {
        // Given
        val authorId = 1L
        val updates = mapOf("name" to "Updated Name")
        val updatedCount = 1
        val updatedAuthorDto = AuthorDto(id = authorId, name = "Updated Name", birthDate = LocalDate.now())

        // When
        // authorRepository.updateAuthorが成功した件数（1）を返すようにモックを設定
        every { authorRepository.updateAuthor(authorId, updates) } returns updatedCount
        // updateAuthor呼び出し後に、findByIdが更新後のAuthorDtoを返すようにモックを設定
        every { authorRepository.findById(authorId) } returns updatedAuthorDto

        val result = authorService.partialUpdateAuthor(authorId, updates)

        // Then
        assertThat(result).isEqualTo(updatedAuthorDto)
        // updateAuthorとfindByIdがそれぞれ1回呼ばれたことを検証
        verify(exactly = 1) { authorRepository.updateAuthor(authorId, updates) }
        verify(exactly = 1) { authorRepository.findById(authorId) }
    }

    @Test
    fun `partialUpdateAuthor should throw AuthorNotFoundException when no author is found for update`() {
        // Given
        val authorId = 999L
        val updates = mapOf("name" to "NonExistent Author")
        val updatedCount = 0

        // When
        // authorRepository.updateAuthorが更新件数0を返すようにモックを設定
        every { authorRepository.updateAuthor(authorId, updates) } returns updatedCount

        // Then
        // AuthorNotFoundExceptionがスローされることを検証
        assertThrows<AuthorNotFoundException> {
            authorService.partialUpdateAuthor(authorId, updates)
        }
        // updateAuthorは呼ばれるが、findByIdは呼ばれないことを検証
        verify(exactly = 1) { authorRepository.updateAuthor(authorId, updates) }
        verify(exactly = 0) { authorRepository.findById(any()) }
    }

    @Test
    fun `partialUpdateAuthor should throw IllegalArgumentException when updates map is empty`() {
        // Given
        val authorId = 1L
        val updates = emptyMap<String, Any?>()

        // When & Then
        // updatesマップが空の場合、IllegalArgumentExceptionがスローされることを検証
        val exception = assertThrows<IllegalArgumentException> {
            authorService.partialUpdateAuthor(authorId, updates)
        }
        // メッセージの検証はGlobalExceptionHandlerTestに任せ、ここではビジネスロジックが正しく例外をスローすることのみを検証
        verify(exactly = 0) { authorRepository.updateAuthor(any(), any()) }
        verify(exactly = 0) { authorRepository.findById(any()) }
    }
}