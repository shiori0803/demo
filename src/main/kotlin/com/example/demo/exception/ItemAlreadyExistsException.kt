package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 重複するデータ登録時にスローされる汎用例外。
 *
 * @param itemType 重複したデータの種類（例: "著者", "書籍"）
 * @param message この例外のメッセージ。デフォルトは`item.already.exists`。
 */
@ResponseStatus(HttpStatus.CONFLICT)
class ItemAlreadyExistsException(
    val itemType: String, message: String = "error.item.already.exists"
) : RuntimeException(message)