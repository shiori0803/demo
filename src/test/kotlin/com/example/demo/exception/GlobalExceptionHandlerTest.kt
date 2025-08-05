package com.example.demo.exception

import com.example.demo.dto.response.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.MethodParameter
import java.util.Locale

class GlobalExceptionHandlerTest {
    private lateinit var globalExceptionHandler: GlobalExceptionHandler
    private val mockWebRequest: WebRequest = mockk()
    private val mockMessageSource: MessageSource = mockk()

    @BeforeEach
    fun setUp() {
        // MessageSourceを注入してテスト対象を初期化
        globalExceptionHandler = GlobalExceptionHandler(mockMessageSource)
        // WebRequestのgetDescriptionメソッドが呼ばれたら、ダミーの文字列を返すようにモックを設定
        every { mockWebRequest.getDescription(any()) } returns "uri=/test-path"

        // 各メッセージキーに対応するメッセージを返すようにモックを設定
        every { mockMessageSource.getMessage("validation.input.error", any(), any<Locale>()) } returns "入力データに誤りがあります。"
        every { mockMessageSource.getMessage("author.already.exists", any(), any<Locale>()) } returns "この著者は既に登録されています。"
        every { mockMessageSource.getMessage("invalid.argument.generic", any(), any<Locale>()) } returns "不正な引数が指定されました。"
        every { mockMessageSource.getMessage("unexpected.error", any(), any<Locale>()) } returns "予期せぬエラーが発生しました。"
    }

    @Test
    fun `handleValidationExceptions should return BAD_REQUEST for MethodArgumentNotValidException`() {
        // Given
        val fieldError = FieldError("objectName", "fieldName", "default message")
        val bindingResult: BindingResult = mockk()
        every { bindingResult.fieldErrors } returns listOf(fieldError)

        // MethodParameterのモックを作成
        val mockMethodParameter: MethodParameter = mockk()
        val exception = MethodArgumentNotValidException(mockMethodParameter, bindingResult)

        // When
        val response: ResponseEntity<ErrorResponse> = globalExceptionHandler.handleValidationExceptions(exception, mockWebRequest)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body?.message).isEqualTo("入力データに誤りがあります。") // MessageSourceから取得したメッセージを検証
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).containsExactly("fieldName: default message")
    }

    @Test
    fun `handleAuthorAlreadyExistsException should return CONFLICT for AuthorAlreadyExistsException`() {
        // Given
        val exception = AuthorAlreadyExistsException("この著者は既に登録されています。")

        // When
        val response: ResponseEntity<ErrorResponse> = globalExceptionHandler.handleAuthorAlreadyExistsException(exception, mockWebRequest)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.status).isEqualTo(HttpStatus.CONFLICT.value())
        assertThat(response.body?.message).isEqualTo("この著者は既に登録されています。") // MessageSourceから取得したメッセージを検証
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
    }

    @Test
    fun `handleIllegalArgumentException should return BAD_REQUEST for IllegalArgumentException`() {
        // Given
        val exception = IllegalArgumentException("不正な引数が指定されました。")

        // When
        val response: ResponseEntity<ErrorResponse> = globalExceptionHandler.handleIllegalArgumentException(exception, mockWebRequest)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body?.message).isEqualTo("不正な引数が指定されました。") // MessageSourceから取得したメッセージを検証
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
    }

    @Test
    fun `handleGlobalException should return INTERNAL_SERVER_ERROR for generic Exception`() {
        // Given
        val exceptionMessage = "Something unexpected happened"
        val exception = RuntimeException(exceptionMessage)

        // When
        val response: ResponseEntity<ErrorResponse> = globalExceptionHandler.handleGlobalException(exception, mockWebRequest)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(response.body?.message).isEqualTo("予期せぬエラーが発生しました。") // MessageSourceから取得したメッセージを検証
        assertThat(response.body?.details).contains("uri=/test-path")
        assertThat(response.body?.details).contains(exceptionMessage)
        assertThat(response.body?.errors).isNull()
    }
}