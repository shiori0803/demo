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
class GlobalExceptionHandler(
    private val messageSource: MessageSource,
) {
    private fun getMessage(
        key: String,
        args: Array<Any>? = null,
    ): String = messageSource.getMessage(key, args, LocaleContextHolder.getLocale())

    // バリデーションエラー (MethodArgumentNotValidException) を処理
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errors =
            ex.bindingResult.fieldErrors.map { error ->
                val resolvedMessage = getMessage(error.defaultMessage ?: "validation.input.error")
                "${error.field}: $resolvedMessage"
            }

        val errorResponse =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                message = getMessage("validation.input.error"),
                details = request.getDescription(false),
                errors = errors,
            )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // ItemNotFoundException を処理するハンドラを追加
    @ExceptionHandler(ItemNotFoundException::class)
    fun handleItemNotFoundException(
        ex: ItemNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                message = getMessage(ex.message!!, arrayOf(ex.itemType)),
                details = request.getDescription(false),
            )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    // IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                // 　ex.messageに値が無ければデフォルトで"validation.invalid.argument.generic"を利用
                message = getMessage(ex.message ?: "validation.invalid.argument.generic"),
                details = request.getDescription(false),
            )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ItemAlreadyExistsException::class)
    fun handleItemAlreadyExistsException(
        ex: ItemAlreadyExistsException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                // 例外からitemTypeを取得し、メッセージにバインド
                message = getMessage(ex.message!!, arrayOf(ex.itemType)),
                details = request.getDescription(false),
            )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // その他の予期せぬエラーを処理
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = getMessage("error.unexpected.error"),
                details = request.getDescription(false) + " - " + ex.localizedMessage,
            )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
