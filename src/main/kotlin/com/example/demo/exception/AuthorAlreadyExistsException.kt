package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * HTTP 409 Conflict ステータスコードを返すことを示すアノテーション
 */
@ResponseStatus(HttpStatus.CONFLICT)
class AuthorAlreadyExistsException(message: String) : RuntimeException(message)