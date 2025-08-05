package com.example.demo.exception

import com.example.demo.dto.response.ErrorResponse
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler(private val messageSource: MessageSource) {

    private fun getMessage(key: String, args: Array<Any>? = null): String {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale())
    }

    // バリデーションエラー (MethodArgumentNotValidException) を処理
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { error ->
            "${error.field}: ${error.defaultMessage}"
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = getMessage("validation.input.error"), // こちらはメッセージキーをそのまま渡す
            details = request.getDescription(false),
            errors = errors
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // AuthorAlreadyExistsException (重複データ) を処理
    @ExceptionHandler(AuthorAlreadyExistsException::class)
    fun handleAuthorAlreadyExistsException(
        ex: AuthorAlreadyExistsException, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            message = getMessage("author.already.exists"),
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // IllegalArgumentException (例: IDがnullであるべきなのに値がある場合) を処理
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: getMessage("invalid.argument.generic"),
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // AuthorNotFoundException (著者が見つからない)
    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFoundException(
        ex: AuthorNotFoundException, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = getMessage("author.not.found"),
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    // BookAlreadyExistsException (重複データ) を処理
    @ExceptionHandler(BookAlreadyExistsException::class)
    fun handleBookAlreadyExistsException(
        ex: BookAlreadyExistsException, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.CONFLICT.value(), // 409 Conflict
            message = getMessage("book.already.exists"), details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // その他の予期せぬエラーを処理
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception, request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = getMessage("unexpected.error"),
            details = request.getDescription(false) + " - " + ex.localizedMessage
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}