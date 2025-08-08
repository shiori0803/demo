package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 出版ステータスが不正に変更された場合にスローされる例外。
 * HTTP 400 Bad Request ステータスコードを返します。
 *
 * @param message この例外のメッセージ。デフォルトは`validation.unauthorized.update.status`。
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPublicationStatusChangeException(
    message: String = "validation.unauthorized.update.status",
) : IllegalArgumentException(message)
