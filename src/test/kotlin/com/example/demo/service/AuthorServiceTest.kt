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
        val exception = assertThrows<AuthorAlreadyExistsException> {
            authorService.registerAuthor(authorDto)
        }
        assertThat(exception.message).isEqualTo("この著者は既に登録されています。")
        verify(exactly = 1) { authorRepository.insertAuthor(authorDto) }
    }

    /**
     * 正常なデータ更新の確認
     */
    @Test
    fun `partialUpdateAuthor should return updated count on successful update`() {
        // Given
        val id = 1L
        val updates = mapOf("name" to "Updated Name", "birthDate" to LocalDate.of(1995, 1, 1))
        val updatedRows = 1

        // When
        // authorRepository.updateAuthorが呼ばれたら、updatedRowsを返すようにモックを設定
        every { authorRepository.updateAuthor(id, updates) } returns updatedRows

        val result = authorService.partialUpdateAuthor(id, updates)

        // Then
        assertThat(result).isEqualTo(updatedRows)
        verify(exactly = 1) { authorRepository.updateAuthor(id, updates) }
    }

    /**
     * 存在しない著者IDを指定した更新を実施した際の確認
     */
    @Test
    fun `partialUpdateAuthor should return 0 when no author is found for update`() {
        // Given
        val id = 999L // 存在しないID
        val updates = mapOf("name" to "NonExistent")
        val updatedRows = 0

        // When
        every { authorRepository.updateAuthor(id, updates) } returns updatedRows

        val result = authorService.partialUpdateAuthor(id, updates)

        // Then
        assertThat(result).isEqualTo(updatedRows)
        verify(exactly = 1) { authorRepository.updateAuthor(id, updates) }
    }

    /**
     * データ更新内容が既存データと重複する場合の確認
     */
    @Test
    fun `partialUpdateAuthor should propagate DataIntegrityViolationException from repository`() {
        // Given
        val id = 1L
        val updates = mapOf("name" to "Conflicting Name")

        // When
        every { authorRepository.updateAuthor(any(), any()) } throws DataIntegrityViolationException("Duplicate key on update")

        // Then
        val exception = assertThrows<DataIntegrityViolationException> {
            authorService.partialUpdateAuthor(id, updates)
        }
        assertThat(exception.message).isEqualTo("Duplicate key on update")
        verify(exactly = 1) { authorRepository.updateAuthor(id, updates) }
    }
}
