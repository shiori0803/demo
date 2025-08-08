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

/**
 * アプリケーション全体で発生する例外を処理するためのグローバルな例外ハンドラクラス。
 *
 * `@ControllerAdvice`アノテーションにより、すべてのコントローラで共通の例外処理を定義できます。
 *
 * @property messageSource メッセージプロパティファイルからメッセージを取得するためのサービス。
 */
@ControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: MessageSource,
) {
    /**
     * 指定されたキーに対応するメッセージを取得します。
     *
     * @param key メッセージキー。
     * @param args メッセージ内のプレースホルダーに埋め込む引数。
     * @return 取得したメッセージ文字列。
     */
    private fun getMessage(
        key: String,
        args: Array<Any>? = null,
    ): String = messageSource.getMessage(key, args, LocaleContextHolder.getLocale())

    /**
     * `@Valid`アノテーションによるバリデーションエラーを処理します。
     *
     * @param ex 発生した`MethodArgumentNotValidException`。
     * @param request HTTPリクエスト。
     * @return エラー詳細を含む`ErrorResponse`と`400 Bad Request`ステータス。
     */
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

    /**
     * データが見つからない場合に発生する`ItemNotFoundException`を処理します。
     *
     * @param ex 発生した`ItemNotFoundException`。
     * @param request HTTPリクエスト。
     * @return エラー詳細を含む`ErrorResponse`と`404 Not Found`ステータス。
     */
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

    /**
     * 不正な引数やビジネスロジック違反で発生する`IllegalArgumentException`を処理します。
     *
     * @param ex 発生した`IllegalArgumentException`。
     * @param request HTTPリクエスト。
     * @return エラー詳細を含む`ErrorResponse`と`400 Bad Request`ステータス。
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                message = getMessage(ex.message ?: "validation.invalid.argument.generic"),
                details = request.getDescription(false),
            )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    /**
     * データの重複登録時に発生する`ItemAlreadyExistsException`を処理します。
     *
     * @param ex 発生した`ItemAlreadyExistsException`。
     * @param request HTTPリクエスト。
     * @return エラー詳細を含む`ErrorResponse`と`409 Conflict`ステータス。
     */
    @ExceptionHandler(ItemAlreadyExistsException::class)
    fun handleItemAlreadyExistsException(
        ex: ItemAlreadyExistsException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                message = getMessage(ex.message!!, arrayOf(ex.itemType)),
                details = request.getDescription(false),
            )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    /**
     * その他の予期せぬエラーを処理します。
     *
     * @param ex 発生した`Exception`。
     * @param request HTTPリクエスト。
     * @return エラー詳細を含む`ErrorResponse`と`500 Internal Server Error`ステータス。
     */
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
