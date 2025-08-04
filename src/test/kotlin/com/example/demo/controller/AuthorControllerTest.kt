package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.request.InsertAuthorRequest
import com.example.demo.dto.request.UpdateAuthorRequest
import com.example.demo.exception.AuthorAlreadyExistsException
import com.example.demo.service.AuthorService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class AuthorControllerTest {

    // AuthorServiceをモック化
    private val authorService: AuthorService = mockk()

    // テスト対象のAuthorController
    private lateinit var authorController: AuthorController

    @BeforeEach
    fun setUp() {
        // 各テストの前にAuthorControllerのインスタンスを再作成し、モックを注入
        authorController = AuthorController(authorService)
    }

    // --- createAuthor メソッドのテスト ---

    @Test
    fun `createAuthor should return CREATED status and AuthorDto on successful creation`() {
        // Given
        val request = InsertAuthorRequest(
            id = null, // 新規作成のためIDはnull
            name = "Haruki Murakami", // name に変更
            birthDate = LocalDate.of(1949, 1, 12)
        )
        val createdAuthorDto = AuthorDto(
            id = 1L, // サービスが生成するID
            name = "Haruki Murakami", // name に変更
            birthDate = LocalDate.of(1949, 1, 12)
        )

        // When
        // authorService.registerAuthorが呼ばれたら、createdAuthorDtoを返すようにモックを設定
        every { authorService.registerAuthor(any()) } returns createdAuthorDto

        val response: ResponseEntity<AuthorDto> = authorController.createAuthor(request)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(createdAuthorDto)
        // サービスメソッドが正しい引数で1回呼ばれたことを検証
        verify(exactly = 1) {
            authorService.registerAuthor(
                AuthorDto(
                    id = null, // コントローラから渡されるIDはnull
                    name = request.name!!, // name に変更
                    birthDate = request.birthDate!!
                )
            )
        }
    }

    @Test
    fun `createAuthor should throw IllegalArgumentException if ID is provided in request`() {
        // Given
        val request = InsertAuthorRequest(
            id = 100L, // IDが提供されている
            name = "Haruki Murakami", // name に変更
            birthDate = LocalDate.of(1949, 1, 12)
        )

        // When & Then
        // require条件に違反するため、IllegalArgumentExceptionがスローされることを検証
        val exception = assertThrows<IllegalArgumentException> {
            authorController.createAuthor(request)
        }
        assertThat(exception.message).isEqualTo("ID must be null for new author creation")
        // サービスメソッドは呼ばれないことを検証
        verify(exactly = 0) { authorService.registerAuthor(any()) }
    }

    @Test
    fun `createAuthor should propagate AuthorAlreadyExistsException from service`() {
        // Given
        val request = InsertAuthorRequest(
            id = null,
            name = "Duplicate Author", // name に変更
            birthDate = LocalDate.of(1990, 5, 10)
        )

        // When
        // authorService.registerAuthorがAuthorAlreadyExistsExceptionをスローするようにモックを設定
        every { authorService.registerAuthor(any()) } throws AuthorAlreadyExistsException("この著者は既に登録されています。")

        // Then
        // 例外がコントローラからそのままスローされることを検証 (GlobalExceptionHandlerで捕捉されることを想定)
        val exception = assertThrows<AuthorAlreadyExistsException> {
            authorController.createAuthor(request)
        }
        assertThat(exception.message).isEqualTo("この著者は既に登録されています。")
        verify(exactly = 1) { authorService.registerAuthor(any()) }
    }

    // --- updateAuthor メソッドのテスト ---

    @Test
    fun `updateAuthor should return OK status and updated count on successful update`() {
        // Given
        val authorId = 1L
        val request = UpdateAuthorRequest(
            name = "Updated Name", // name に変更
            birthDate = LocalDate.of(2000, 1, 1)
        )
        val updatedCount = 1

        // When
        // authorService.partialUpdateAuthorが呼ばれたら、updatedCountを返すようにモックを設定
        every { authorService.partialUpdateAuthor(authorId, any()) } returns updatedCount

        val response: ResponseEntity<Any> = authorController.updateAuthor(authorId, request)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(updatedCount)
        // サービスメソッドが正しい引数で1回呼ばれたことを検証
        verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, any()) }
    }

    @Test
    fun `updateAuthor should return NOT_FOUND status when no author is updated`() {
        // Given
        val authorId = 999L // 存在しないID
        val request = UpdateAuthorRequest(
            name = "NonExistent Author", // name に変更
            birthDate = LocalDate.of(1990, 5, 10)
        )
        val updatedCount = 0

        // When
        every { authorService.partialUpdateAuthor(authorId, any()) } returns updatedCount

        val response: ResponseEntity<Any> = authorController.updateAuthor(authorId, request)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).isNull()
        verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, any()) }
    }

    @Test
    fun `updateAuthor should throw IllegalArgumentException if ID in request body mismatches path variable ID`() {
        // Given
        val pathId = 1L
        val requestBodyId = 2L // パス変数と異なるID
        val request = UpdateAuthorRequest(
            id = requestBodyId,
            name = "Test Mismatch", // name に変更
            birthDate = LocalDate.of(2000, 1, 1)
        )

        // When & Then
        // require条件に違反するため、IllegalArgumentExceptionがスローされることを検証
        val exception = assertThrows<IllegalArgumentException> {
            authorController.updateAuthor(pathId, request)
        }
        assertThat(exception.message).isEqualTo("ID in request body must be null or match path variable ID")
        // サービスメソッドは呼ばれないことを検証
        verify(exactly = 0) { authorService.partialUpdateAuthor(any(), any()) }
    }
}
