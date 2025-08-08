package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 通常発生しないエラーが発生した場合にスローされる例外。
 * 予期せぬ内部エラーを示すためにHTTP 500を返します。
 *
 * @param message この例外のメッセージ。
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class UnexpectedException(
    message: String,
) : RuntimeException(message)
