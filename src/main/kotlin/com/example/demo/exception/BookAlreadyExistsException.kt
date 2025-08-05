package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * HTTP 409 Conflict ステータスコードを返すことを示すカスタム例外。
 * 重複する書籍データ登録時に使用します。
 */
@ResponseStatus(HttpStatus.CONFLICT)
class BookAlreadyExistsException(message: String) : RuntimeException(message)