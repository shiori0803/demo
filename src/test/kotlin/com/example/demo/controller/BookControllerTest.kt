package com.example.demo.controller

import com.example.demo.dto.request.PatchBookRequest
import com.example.demo.dto.request.RegisterBookRequest
import com.example.demo.dto.response.BookWithAuthorsResponse
import com.example.demo.service.BookService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.stream.Stream

@WebMvcTest(BookController::class)
class BookControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) {
        @MockkBean
        private lateinit var bookService: BookService

        @BeforeEach
        fun setUp() {
            objectMapper.registerModule(JavaTimeModule())
        }

        companion object {
            // titleが空文字、空白のみ、nullの場合
            @JvmStatic
            fun invalidTitleRequests(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(null),
                    Arguments.of(""),
                    Arguments.of("   "),
                )

            // publicationStatusがnull、0または1以外の値の場合
            @JvmStatic
            fun invalidPublicationStatusRequests(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(null),
                    Arguments.of(999),
                )

            // authorIdsがnull、または空の場合
            @JvmStatic
            fun invalidAuthorIdsStatusRequests(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(null),
                    Arguments.of(emptyList<Long>()),
                )
        }

        // --- createBook メソッドのテスト ---
        @Test
        fun `createBook 正常なリクエストで書籍が作成され201が返されること`() {
            // Given
            val title = "Test Book"
            val price = 1000
            val publicationStatus = 1
            val authorIds = listOf(1L)

            val request =
                RegisterBookRequest(
                    title,
                    price,
                    publicationStatus,
                    authorIds,
                )
            val createdBook =
                BookWithAuthorsResponse(
                    id = 1L,
                    title,
                    price,
                    publicationStatus,
                    authorIds,
                )
            // bookService.registerBookが呼び出されたらcreatedBookを返すようにモック化
            every { bookService.registerBook(any(), any()) } returns createdBook

            // When & Then
            mockMvc
                .perform(
                    post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect { result ->
                    val responseBody =
                        objectMapper.readValue(result.response.contentAsString, BookWithAuthorsResponse::class.java)
                    assertThat(responseBody).isEqualTo(createdBook)
                }
            // bookService.registerBookが1回呼び出されたことを検証
            verify(exactly = 1) { bookService.registerBook(any(), any()) }
        }

        @ParameterizedTest
        @MethodSource("invalidTitleRequests")
        fun `createBook titleが不正な値の場合にバリデーションエラーが発生し400が返されること`(title: String?) {
            // Given
            val request =
                RegisterBookRequest(
                    title = title,
                    price = 1000,
                    publicationStatus = 1,
                    authorIds = listOf(1L),
                )

            // When & Then
            mockMvc
                .perform(
                    post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("title")
                }
        }

        @Test
        fun `createBook priceがnullの場合にバリデーションエラーが発生し400が返されること`() {
            // Given
            val request =
                RegisterBookRequest(
                    title = "Test Book",
                    price = null,
                    publicationStatus = 1,
                    authorIds = listOf(1L),
                )

            // When & Then
            mockMvc
                .perform(
                    post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("price")
                }
        }

        @ParameterizedTest
        @MethodSource("invalidPublicationStatusRequests")
        fun `createBook publicationStatusが不正な値の場合にバリデーションエラーが発生し400が返されること`(publicationStatus: Int?) {
            // Given
            val request =
                RegisterBookRequest(
                    title = "Test Book",
                    price = 1000,
                    publicationStatus = publicationStatus,
                    authorIds = listOf(1L),
                )

            // When & Then
            mockMvc
                .perform(
                    post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("publicationStatus")
                }
        }

        @ParameterizedTest
        @MethodSource("invalidAuthorIdsStatusRequests")
        fun `createBook authorIdsが不正な値の場合にバリデーションエラーが発生し400が返されること`(authorIds: List<Long>?) {
            // Given
            val request =
                RegisterBookRequest(
                    title = "Test Book",
                    price = 1000,
                    publicationStatus = 1,
                    authorIds = authorIds,
                )

            // When & Then
            mockMvc
                .perform(
                    post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect { result ->
                    assertThat(result.resolvedException).isInstanceOf(MethodArgumentNotValidException::class.java)
                    val errors = (result.resolvedException as MethodArgumentNotValidException).bindingResult.fieldErrors
                    assertThat(errors).hasSize(1)
                    assertThat(errors.first().field).isEqualTo("authorIds")
                }
        }

        // --- patchBook メソッドのテスト ---
        @Test
        fun `patchBook 正常に更新が完了する場合`() {
            // Given
            val bookId = 1L
            val updatedTitle = "更新後のタイトル"
            val updatedPrice = 2500
            val newAuthorIds = listOf(2L)

            val requestBody =
                PatchBookRequest(
                    title = updatedTitle,
                    price = updatedPrice,
                    publicationStatus = null,
                    authorIds = newAuthorIds,
                )
            val expectedBookResponse =
                BookWithAuthorsResponse(
                    id = bookId,
                    title = updatedTitle,
                    price = updatedPrice,
                    // 更新対象外になって登録済の値が帰ってくる想定
                    publicationStatus = 0,
                    authorIds = newAuthorIds,
                )

            // bookService.updateBookが呼び出されたときに期待される値を返すようにモック化
            every { bookService.updateBook(eq(bookId), any(), eq(newAuthorIds)) } returns expectedBookResponse

            // When & Then
            mockMvc
                .perform(
                    patch("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                ).andExpect(status().isOk)
                .andExpect { result ->
                    val responseBody =
                        objectMapper.readValue(result.response.contentAsString, BookWithAuthorsResponse::class.java)
                    assertThat(responseBody).isEqualTo(expectedBookResponse)
                }

            // bookService.updateBookが1回呼び出されたことを検証
            verify(exactly = 1) { bookService.updateBook(eq(bookId), any(), eq(newAuthorIds)) }
        }

        @Test
        fun `patchBook nullの項目は更新マップに追加されず200が返されること`() {
            // Given
            val bookId = 1L
            val requestBody =
                PatchBookRequest(
                    title = null,
                    price = null,
                    publicationStatus = 1, // これのみ更新
                    authorIds = null,
                )
            val expectedBookResponse =
                BookWithAuthorsResponse(
                    id = bookId,
                    title = "元のタイトル",
                    price = 1000,
                    publicationStatus = 1,
                    authorIds = listOf(1L),
                )

            // bookService.updateBookが呼ばれたときに期待される値を返すようにモック化
            every { bookService.updateBook(eq(bookId), any(), isNull()) } returns expectedBookResponse

            // When & Then
            mockMvc
                .perform(
                    patch("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                ).andExpect(status().isOk)

            // bookService.updateBookが正しい引数で呼ばれたことを検証
            val expectedUpdatesMap = mapOf("publicationStatus" to 1)
            verify(exactly = 1) { bookService.updateBook(eq(bookId), eq(expectedUpdatesMap), isNull()) }
        }

        @Test
        fun `patchBook titleが空文字の場合、更新対象マップに追加されないこと`() {
            // Given
            val bookId = 1L
            val requestBody =
                PatchBookRequest(
                    title = "", // 空文字
                    price = null,
                    publicationStatus = null,
                    authorIds = null,
                )
            val expectedBookResponse =
                BookWithAuthorsResponse(
                    id = bookId,
                    title = "元のタイトル", // 更新されないことを想定
                    price = 1000,
                    publicationStatus = 0,
                    authorIds = listOf(1L),
                )

            // bookService.updateBookが呼ばれたときに期待される値を返すようにモック化
            every { bookService.updateBook(eq(bookId), any(), isNull()) } returns expectedBookResponse

            // When & Then
            mockMvc
                .perform(
                    patch("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                ).andExpect(status().isOk)

            // bookService.updateBookが正しい引数で呼ばれたことを検証
            // titleが更新対象マップに含まれないことを確認
            val expectedUpdatesMap = emptyMap<String, Any>()
            verify(exactly = 1) { bookService.updateBook(eq(bookId), eq(expectedUpdatesMap), isNull()) }
        }

        @Test
        fun `patchBook 空のリクエストボディで更新がスキップされ200が返されること`() {
            // Given
            val bookId = 1L
            val requestBody = PatchBookRequest(null, null, null, null, null)
            val originalBookResponse =
                BookWithAuthorsResponse(
                    id = bookId,
                    title = "元のタイトル",
                    price = 1000,
                    publicationStatus = 0,
                    authorIds = listOf(1L),
                )

            // 更新がない場合、元のDTOが返されると想定してモック化
            every { bookService.updateBook(eq(bookId), any(), isNull()) } returns originalBookResponse

            // When & Then
            mockMvc
                .perform(
                    patch("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                ).andExpect(status().isOk)
                .andExpect { result ->
                    val responseBody =
                        objectMapper.readValue(result.response.contentAsString, BookWithAuthorsResponse::class.java)
                    assertThat(responseBody).isEqualTo(originalBookResponse)
                }

            // bookService.updateBookが空の更新マップで呼ばれたことを検証
            val expectedUpdatesMap = emptyMap<String, Any>()
            verify(exactly = 1) { bookService.updateBook(eq(bookId), eq(expectedUpdatesMap), isNull()) }
        }
    }
