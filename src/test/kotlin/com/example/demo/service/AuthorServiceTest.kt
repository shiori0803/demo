package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.exception.AuthorAlreadyExistsException
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

    @Test
    fun `registerAuthor should return AuthorDto with generated ID on successful insertion`() {
        // Given
        val authorDto = AuthorDto(
            id = null,
            firstName = "Test",
            middleName = "Middle",
            lastName = "User",
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
        assertThat(result.firstName).isEqualTo(authorDto.firstName)
        // 他のフィールドも検証
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) } // insertAuthorが1回呼ばれたことを検証
    }

    @Test
    fun `registerAuthor should throw AuthorAlreadyExistsException when DataIntegrityViolationException occurs`() {
        // Given
        val authorDto = AuthorDto(
            id = null,
            firstName = "Duplicate",
            middleName = null,
            lastName = "Author",
            birthDate = LocalDate.of(1990, 5, 10)
        )

        // When
        // authorRepository.insertAuthorが呼ばれたら、DataIntegrityViolationExceptionをスローするようにモックを設定
        every { authorRepository.insertAuthor(any()) } throws DataIntegrityViolationException("Duplicate entry for author")

        // Then
        // AuthorAlreadyExistsExceptionがスローされることを検証
        val exception = assertThrows<AuthorAlreadyExistsException> {
            authorService.registerAuthor(authorDto)
        }
        assertThat(exception.message).isEqualTo("この著者は既に登録されています。")
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    @Test
    fun `partialUpdateAuthor should return updated count on successful update`() {
        // Given
        val id = 1L
        val updates = mapOf("firstName" to "UpdatedName", "middleName" to null)
        val updatedRows = 1

        // When
        // authorRepository.updateAuthorが呼ばれたら、updatedRowsを返すようにモックを設定
        every { authorRepository.updateAuthor(id, updates) } returns updatedRows

        val result = authorService.partialUpdateAuthor(id, updates)

        // Then
        assertThat(result).isEqualTo(updatedRows)
        verify(exactly = 1) { authorRepository.updateAuthor(id, updates) }
    }

    @Test
    fun `partialUpdateAuthor should return 0 when no author is found for update`() {
        // Given
        val id = 999L // 存在しないID
        val updates = mapOf("firstName" to "NonExistent")
        val updatedRows = 0

        // When
        every { authorRepository.updateAuthor(id, updates) } returns updatedRows

        val result = authorService.partialUpdateAuthor(id, updates)

        // Then
        assertThat(result).isEqualTo(updatedRows)
        verify(exactly = 1) { authorRepository.updateAuthor(id, updates) }
    }

    // 必要に応じて、partialUpdateAuthorがリポジトリから他の例外をスローした場合のテストも追加
}