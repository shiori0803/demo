package com.example.demo.exception

import com.example.demo.dto.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    // バリデーションエラー (MethodArgumentNotValidException) を処理
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { error ->
            "${error.field}: ${error.defaultMessage}"
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(), // 400 Bad Request
            message = "入力データに誤りがあります。",
            details = request.getDescription(false),
            errors = errors
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // AuthorAlreadyExistsException (重複データ) を処理
    @ExceptionHandler(AuthorAlreadyExistsException::class)
    fun handleAuthorAlreadyExistsException(
        ex: AuthorAlreadyExistsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.CONFLICT.value(), // 409 Conflict
            message = ex.message ?: "重複するデータが登録されようとしました。",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // IllegalArgumentException (例: IDがnullであるべきなのに値がある場合) を処理
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(), // 400 Bad Request
            message = ex.message ?: "不正な引数が指定されました。",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // AuthorNotFoundException (著者が見つからない)
    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFoundException(
        ex: AuthorNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(), // 404 Not Found
            message = ex.message ?: "指定された著者が見つかりません。",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    // BookNotFoundException (書籍が見つからない) を処理
    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFoundException(
        ex: BookNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(), // 404 Not Found
            message = ex.message ?: "指定された書籍が見つかりません。",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    } // 追加

    // InvalidPublicationStatusChangeException (出版ステータスの不正な変更) を処理
    @ExceptionHandler(InvalidPublicationStatusChangeException::class)
    fun handleInvalidPublicationStatusChangeException(
        ex: InvalidPublicationStatusChangeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(), // 400 Bad Request
            message = ex.message ?: "出版ステータスの変更が無効です。",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // その他の予期せぬエラーを処理
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500 Internal Server Error
            message = "予期せぬエラーが発生しました。",
            details = request.getDescription(false) + " - " + ex.localizedMessage
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
