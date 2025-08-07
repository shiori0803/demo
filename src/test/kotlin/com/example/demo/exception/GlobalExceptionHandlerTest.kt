@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.demo.exception

import com.example.demo.dto.response.ErrorResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import java.util.*

class GlobalExceptionHandlerTest {
    private lateinit var globalExceptionHandler: GlobalExceptionHandler
    private val mockWebRequest: WebRequest = mockk()
    private val mockMessageSource: MessageSource = mockk()

    @BeforeEach
    fun setUp() {
        globalExceptionHandler = GlobalExceptionHandler(mockMessageSource)
        every { mockWebRequest.getDescription(any()) } returns "uri=/test-path"

        every {
            mockMessageSource.getMessage(
                eq("validation.input.error"),
                any(),
                any<Locale>(),
            )
        } returns "入力データに誤りがあります。"
        every {
            mockMessageSource.getMessage(
                eq("error.item.not.found"),
                any<Array<Any>>(),
                any<Locale>(),
            )
        } answers { "指定された" + (it.invocation.args[1] as Array<Any>)[0] + "が見つかりません。" }
        every {
            mockMessageSource.getMessage(
                eq("error.item.already.exists"),
                any<Array<Any>>(),
                any<Locale>(),
            )
        } answers { "この" + (it.invocation.args[1] as Array<Any>)[0] + "は既に登録されています。" }
        every {
            mockMessageSource.getMessage(
                eq("validation.invalid.argument.generic"),
                any(),
                any<Locale>(),
            )
        } returns "不正な引数が指定されました。"
        every {
            mockMessageSource.getMessage(
                eq("validation.unauthorized.update.status"),
                any(),
                any<Locale>(),
            )
        } returns "出版済から未出版への変更はできません。"
        every {
            mockMessageSource.getMessage(
                eq("error.unexpected.error"),
                any(),
                any<Locale>(),
            )
        } returns "予期せぬエラーが発生しました。詳細：{0}"
        every { mockMessageSource.getMessage(eq("default message"), any(), any<Locale>()) } returns "default message"
        every {
            mockMessageSource.getMessage(
                eq("不正な引数が指定されました。"),
                any(),
                any<Locale>(),
            )
        } returns "不正な引数が指定されました。"
    }

    @Test
    fun `handleValidationExceptions should return BAD_REQUEST for MethodArgumentNotValidException`() {
        val fieldError = FieldError("objectName", "fieldName", "default message")
        val bindingResult: BindingResult = mockk()
        every { bindingResult.fieldErrors } returns listOf(fieldError)
        val mockMethodParameter: MethodParameter = mockk()
        val exception = MethodArgumentNotValidException(mockMethodParameter, bindingResult)

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleValidationExceptions(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body?.message).isEqualTo("入力データに誤りがあります。")
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).containsExactly("fieldName: default message")
        verify { mockMessageSource.getMessage(eq("validation.input.error"), any(), any()) }
        verify { mockMessageSource.getMessage(eq("default message"), any(), any()) }
    }

    @Test
    fun `handleItemNotFoundException should return NOT_FOUND for ItemNotFoundException`() {
        val exception = ItemNotFoundException(itemType = "著者ID")

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleItemNotFoundException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(response.body?.message).isEqualTo("指定された著者IDが見つかりません。")
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
        verify {
            mockMessageSource.getMessage(
                eq("error.item.not.found"),
                arrayOf("著者ID"),
                LocaleContextHolder.getLocale(),
            )
        }
    }

    @Test
    fun `handleItemAlreadyExistsException should return CONFLICT for ItemAlreadyExistsException`() {
        val exception = ItemAlreadyExistsException(itemType = "書籍")

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleItemAlreadyExistsException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.status).isEqualTo(HttpStatus.CONFLICT.value())
        assertThat(response.body?.message).isEqualTo("この書籍は既に登録されています。")
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
        verify {
            mockMessageSource.getMessage(
                eq("error.item.already.exists"),
                arrayOf("書籍"),
                LocaleContextHolder.getLocale(),
            )
        }
    }

    @Test
    fun `handleIllegalArgumentException should return BAD_REQUEST for IllegalArgumentException`() {
        val exception = IllegalArgumentException("不正な引数が指定されました。")

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleIllegalArgumentException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body?.message).isEqualTo("不正な引数が指定されました。")
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
        verify { mockMessageSource.getMessage(eq("不正な引数が指定されました。"), any(), any()) }
    }

    @Test
    fun `handleInvalidPublicationStatusChangeException should return BAD_REQUEST`() {
        val exception = InvalidPublicationStatusChangeException()

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleIllegalArgumentException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body?.message).isEqualTo("出版済から未出版への変更はできません。")
        assertThat(response.body?.details).isEqualTo("uri=/test-path")
        assertThat(response.body?.errors).isNull()
        verify { mockMessageSource.getMessage(eq("validation.unauthorized.update.status"), any(), any()) }
    }

    @Test
    fun `handleUnexpectedException should return INTERNAL_SERVER_ERROR`() {
        val exceptionMessage = "著者情報更新APIにて著者データの登録が完了しましたが、著者IDが取得できません。"
        val exception = UnexpectedException(exceptionMessage)

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleGlobalException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(response.body?.message).isEqualTo("予期せぬエラーが発生しました。詳細：{0}")
        assertThat(response.body?.details).contains("uri=/test-path")
        assertThat(response.body?.details).contains(exceptionMessage)
        assertThat(response.body?.errors).isNull()
        verify { mockMessageSource.getMessage(eq("error.unexpected.error"), any(), any()) }
    }

    @Test
    fun `handleGlobalException should return INTERNAL_SERVER_ERROR for generic Exception`() {
        val exceptionMessage = "Something unexpected happened"
        val exception = RuntimeException(exceptionMessage)

        val response: ResponseEntity<ErrorResponse> =
            globalExceptionHandler.handleGlobalException(exception, mockWebRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(response.body?.message).isEqualTo("予期せぬエラーが発生しました。詳細：{0}")
        assertThat(response.body?.details).contains("uri=/test-path")
        assertThat(response.body?.details).contains(exceptionMessage)
        assertThat(response.body?.errors).isNull()
        verify { mockMessageSource.getMessage(eq("error.unexpected.error"), any(), any()) }
    }
}
