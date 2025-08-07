package com.example.demo.controller

import com.example.demo.dto.BookDto
import com.example.demo.dto.request.PatchBookRequest
import com.example.demo.dto.request.RegisterBookRequest
import com.example.demo.dto.response.BookWithAuthorsResponse
import com.example.demo.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 書籍関連APIのコントローラ
 */
@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
) {
    /**
     * 書籍情報登録API
     *
     * @param registerBookRequest
     * @return 成功時：BookWithAuthorsResponse,失敗時：ErrorResponse
     */
    @PostMapping
    fun createBook(
        @Valid @RequestBody registerBookRequest: RegisterBookRequest,
    ): ResponseEntity<BookWithAuthorsResponse> {
        val bookDto =
            BookDto(
                id = null,
                title = registerBookRequest.title!!,
                price = registerBookRequest.price!!,
                publicationStatus = registerBookRequest.publicationStatus!!,
            )

        // サービス層の呼び出しと、新しいレスポンスDTOの取得
        val createdBook = bookService.registerBook(bookDto, registerBookRequest.authorIds!!)

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook)
    }

    /**
     * 既存の書籍データを部分的に更新するAPI
     * PATCH /api/books/{id}
     */
    @PatchMapping("/{id}")
    fun patchBook(
        @PathVariable id: Long,
        @Valid @RequestBody patchBookRequest: PatchBookRequest,
    ): ResponseEntity<BookWithAuthorsResponse> {
        // 更新するフィールドと値のマップを構築
        val updates = mutableMapOf<String, Any?>()

        // titleがnullまたは空文字・空白でない場合のみマップに追加
        if (!patchBookRequest.title.isNullOrBlank()) {
            updates["title"] = patchBookRequest.title
        }

        // priceがnullでない場合のみマップに追加
        if (patchBookRequest.price != null) {
            updates["price"] = patchBookRequest.price
        }

        // publicationStatusがnullでない場合のみマップに追加
        if (patchBookRequest.publicationStatus != null) {
            updates["publicationStatus"] = patchBookRequest.publicationStatus
        }

        // サービス層を呼び出し、更新された書籍データを取得
        val updatedBookWithAuthors = bookService.updateBook(id, updates, patchBookRequest.authorIds)

        // 成功した場合、OKステータスと更新された書籍データを返す
        return ResponseEntity.ok(updatedBookWithAuthors)
    }
}
