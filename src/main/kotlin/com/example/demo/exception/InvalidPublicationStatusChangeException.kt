package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 出版状況を「出版済み」ぁら「未出版」に更新しようとした際にスローされるException
 *
 * @constructor
 * 例外メッセージはデフォルトで`book.publication.status.invalid.change`が使用される
 *
 * @param message
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPublicationStatusChangeException(message: String = "validation.unauthorized.update.status") :
    RuntimeException(message)