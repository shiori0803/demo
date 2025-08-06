package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 操作対象のデータが見つからなかったときにスローされる汎用Exception。
 * HTTP 404 Not Found ステータスコードを返します。
 *
 * @param itemType 見つからなかったデータの種類（例: "著者ID", "書籍ID"）
 * @param message この例外のメッセージ。デフォルトは`error.item.not.found`。
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
class ItemNotFoundException(
    val itemType: String,
    message: String = "error.item.not.found"
) : RuntimeException(message)