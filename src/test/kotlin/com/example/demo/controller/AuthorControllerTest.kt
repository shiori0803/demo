package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.request.PatchAuthorRequest
import com.example.demo.dto.request.RegisterAuthorRequest
import com.example.demo.dto.response.AuthorResponse
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.exception.ItemNotFoundException
import com.example.demo.service.AuthorService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.MethodArgumentNotValidException
import java.time.LocalDate
import java.util.stream.Stream

@WebMvcTest(AuthorController::class)
class AuthorControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) {
        @MockkBean
        private lateinit var authorService: AuthorService

        @BeforeEach
        fun setUp() {
            objectMapper.registerModule(JavaTimeModule())
        }

        // ----------------- テストデータ -----------------
        companion object {
            @JvmStatic
            fun invalidNameRequests(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(null, LocalDate.of(1949, 1, 12)),
                    Arguments.of("", LocalDate.of(1949, 1, 12)),
                    Arguments.of("   ", LocalDate.of(1949, 1, 12)),
                )

            @JvmStatic
            fun invalidFutureDates(): Stream<Arguments> =
                Stream.of(
                    Arguments.of("Test Name", LocalDate.now()),
                    Arguments.of("Test Name", LocalDate.now().plusDays(1)),
                )

            @JvmStatic
            fun partialUpdateTestData(): Stream<Arguments> {
                val originalAuthorDto = AuthorDto(id = 1L, name = "Original Name", birthDate = LocalDate.of(2000, 1, 1))
                val updatedAuthorDto = AuthorDto(id = 1L, name = "Updated Name", birthDate = LocalDate.of(1990, 1, 1))

                return Stream.of(
                    // nameのみ更新
                    Arguments.of(
                        originalAuthorDto,
                        updatedAuthorDto.copy(birthDate = originalAuthorDto.birthDate),
                        mapOf("name" to "Updated Name"),
                    ),
                    // birthDateのみ更新
                    Arguments.of(
                        originalAuthorDto,
                        updatedAuthorDto.copy(name = originalAuthorDto.name),
                        mapOf("birthDate" to LocalDate.of(1990, 1, 1)),
                    ),
                    // nameとbirthDateを両方更新
                    Arguments.of(
                        originalAuthorDto,
                        updatedAuthorDto,
                        mapOf("name" to "Updated Name", "birthDate" to LocalDate.of(1990, 1, 1)),
                    ),
                )
            }

            @JvmStatic
            fun invalidNameUpdates(): Stream<Arguments> {
                val birthDate = LocalDate.of(2000, 1, 1)

                // nameが無効な値を持つリクエストと、それに対応する期待値のupdatesマップ
                return Stream.of(
                    // nameがnullの場合
                    Arguments.of(PatchAuthorRequest(name = null, birthDate = birthDate), mapOf("birthDate" to birthDate)),
                    // nameが空文字の場合
                    Arguments.of(PatchAuthorRequest(name = "", birthDate = birthDate), mapOf("birthDate" to birthDate)),
                    // nameが空白のみの場合
                    Arguments.of(PatchAuthorRequest(name = "   ", birthDate = birthDate), mapOf("birthDate" to birthDate)),
                )
            }
        }

        // --- getBooksByAuthorId メソッドのテスト ---
        @Test
        fun `getBooksByAuthorId 正常にデータを取得できる場合`() {
            // Given
            val authorId = 1L
            val mockAuthorResponse =
                AuthorResponse(
                    id = authorId,
                    name = "Haruki Murakami",
                    birthDate = LocalDate.of(1949, 1, 12),
                )
            val mockAuthorWithBooksResponse =
                AuthorWithBooksResponse(
                    author = mockAuthorResponse,
                    books = emptyList(),
                )

            every { authorService.getAuthorWithBooksResponse(authorId) } returns mockAuthorWithBooksResponse

            // When & Then
            mockMvc.perform(get("/api/authors/{authorId}/books", authorId)).andExpect(status().isOk).andExpect { result ->
                val responseBody =
                    objectMapper.readValue(result.response.contentAsString, AuthorWithBooksResponse::class.java)
                assertThat(responseBody).isEqualTo(mockAuthorWithBooksResponse)
            }
            verify(exactly = 1) { authorService.getAuthorWithBooksResponse(authorId) }
        }

        @Test
        fun `getBooksByAuthorId 存在しない著者を指定してItemNotFoundExceptionがスローされる場合`() {
            // Given
            val authorId = 999L

            every { authorService.getAuthorWithBooksResponse(authorId) } throws ItemNotFoundException("著者ID")

            // When & Then
            mockMvc.perform(get("/api/authors/{authorId}/books", authorId)).andExpect(status().isNotFound)
            verify(exactly = 1) { authorService.getAuthorWithBooksResponse(authorId) }
        }

        // --- registerAuthor メソッドのテスト ---
        @Test
        fun `registerAuthor 正常にデータを登録できる場合`() {
            // Given
            val authorName = "Haruki Murakami"
            val authorBirthDate = LocalDate.now().minusDays(1)
            val authorId = 1L

            val request = RegisterAuthorRequest(name = authorName, birthDate = authorBirthDate)
            val createdAuthorDto = AuthorResponse(id = authorId, name = authorName, birthDate = authorBirthDate)
            val createdAuthorResponse = AuthorResponse(id = authorId, name = authorName, birthDate = authorBirthDate)

            every { authorService.registerAuthor(any()) } returns createdAuthorDto

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect { result ->
                    val responseBody = objectMapper.readValue(result.response.contentAsString, AuthorResponse::class.java)
                    assertThat(responseBody).isEqualTo(createdAuthorResponse)
                }
            verify(exactly = 1) { authorService.registerAuthor(any()) }
        }

        @ParameterizedTest
        @MethodSource("invalidNameRequests")
        fun `registerAuthor nameがnullと空文字でバリデーションエラーが発生する場合`(
            name: String?,
            birthDate: LocalDate?,
        ) {
            // Given
            val request = RegisterAuthorRequest(name = name, birthDate = birthDate)

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("name")
                }
        }

        @Test
        fun `registerAuthor birthDateがnullの場合にバリデーションエラーが発生する場合`() {
            // Given
            val request = RegisterAuthorRequest(name = "Test Name", birthDate = null)

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("birthDate")
                }
        }

        @ParameterizedTest
        @MethodSource("invalidFutureDates")
        fun `registerAuthor birthDateが不正な日付の場合にバリデーションエラーが発生する場合`(
            name: String?,
            birthDate: LocalDate?,
        ) {
            // Given
            val request = RegisterAuthorRequest(name = name, birthDate = birthDate)

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("birthDate")
                }
        }

        @Test
        fun `registerAuthor リクエストボディにnameがそもそも含まれない場合`() {
            // Given
            val json = """{"birthDate": "2000-01-01"}"""

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors").contentType(MediaType.APPLICATION_JSON).content(json),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("name")
                }
        }

        @Test
        fun `registerAuthor リクエストボディにbirthDateがそもそも含まれない場合`() {
            // Given
            val json = """{"name": "Test Name"}"""

            // When & Then
            mockMvc
                .perform(
                    post("/api/authors").contentType(MediaType.APPLICATION_JSON).content(json),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("birthDate")
                }
        }

        // --- patchAuthor メソッドのテスト ---
        @ParameterizedTest
        @MethodSource("partialUpdateTestData")
        fun `patchAuthor 正常に更新できる場合`(
            originalAuthorDto: AuthorDto,
            updatedAuthorDto: AuthorDto,
            updatesMap: Map<String, Any?>,
        ) {
            // Given
            val authorId = originalAuthorDto.id!!
            val request =
                PatchAuthorRequest(name = updatesMap["name"] as? String, birthDate = updatesMap["birthDate"] as? LocalDate)

            every { authorService.partialUpdateAuthor(authorId, updatesMap) } returns updatedAuthorDto

            // When & Then
            mockMvc
                .perform(
                    patch("/api/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect { result ->
                    val responseBody = objectMapper.readValue(result.response.contentAsString, AuthorDto::class.java)
                    assertThat(responseBody).isEqualTo(updatedAuthorDto)
                }
            verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, updatesMap) }
        }

        @ParameterizedTest
        @MethodSource("invalidNameUpdates")
        fun `patchAuthor nameがnullまたは空文字、空白の場合に更新値として渡さない`(
            request: PatchAuthorRequest,
            expectedUpdates: Map<String, Any?>,
        ) {
            // Given
            val authorId = 1L
            val updatedAuthorDto = AuthorDto(id = authorId, name = "Original Name", birthDate = LocalDate.of(2000, 1, 1))

            every { authorService.partialUpdateAuthor(authorId, expectedUpdates) } returns updatedAuthorDto

            // When & Then
            mockMvc
                .perform(
                    patch("/api/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect { result ->
                    val responseBody = objectMapper.readValue(result.response.contentAsString, AuthorDto::class.java)
                    assertThat(responseBody).isEqualTo(updatedAuthorDto)
                }
            verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, expectedUpdates) }
        }

        @Test
        fun `patchAuthor birthDateの値がnullだったらサービスクラスに更新値として渡さない`() {
            // Given
            val authorId = 1L
            val name = "Updated Name"
            val request = PatchAuthorRequest(name = name, birthDate = null)
            val expectedUpdates = mapOf("name" to name)
            val updatedAuthorDto = AuthorDto(id = authorId, name = name, birthDate = LocalDate.of(2000, 1, 1))

            every { authorService.partialUpdateAuthor(authorId, expectedUpdates) } returns updatedAuthorDto

            // When & Then
            mockMvc
                .perform(
                    patch("/api/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect { result ->
                    val responseBody = objectMapper.readValue(result.response.contentAsString, AuthorDto::class.java)
                    assertThat(responseBody).isEqualTo(updatedAuthorDto)
                }
            // MockKのverifyファンクションは、ファンクションが呼び出された際の引数の中身まで検証する（mapの中身も検証できる）
            verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, expectedUpdates) }
        }

        @Test
        fun `patchAuthor birthDateの値が本日を含む未来日だったらバリデーションエラーになる`() {
            // Given
            val authorId = 1L
            val request = PatchAuthorRequest(name = "Test Name", birthDate = LocalDate.now())

            every {
                authorService.partialUpdateAuthor(
                    any(),
                    any(),
                )
            } throws IllegalArgumentException("validation.birthdate")

            // When & Then
            mockMvc
                .perform(
                    patch("/api/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    val resolvedException = result.resolvedException as IllegalArgumentException
                    assertThat(resolvedException.message).isEqualTo("validation.birthdate")
                }
            verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, any()) }
        }

        @Test
        fun `patchAuthor リクエストボディが空だったら更新するものがないエラーになる`() {
            // Given
            val authorId = 1L
            val request = PatchAuthorRequest(name = null, birthDate = null)

            every {
                authorService.partialUpdateAuthor(
                    authorId,
                    emptyMap(),
                )
            } throws IllegalArgumentException("error.nothing.update")

            // When & Then
            mockMvc
                .perform(
                    patch("/api/authors/{id}", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    val resolvedException = result.resolvedException as IllegalArgumentException
                    assertThat(resolvedException.message).isEqualTo("error.nothing.update")
                }
            verify(exactly = 1) { authorService.partialUpdateAuthor(authorId, emptyMap()) }
        }
    }
