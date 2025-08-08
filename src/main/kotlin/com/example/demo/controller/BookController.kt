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
 * 書籍関連APIを処理するコントローラクラス。
 *
 * @property bookService 書籍データのビジネスロジックを担うサービス。
 */
@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
) {
    /**
     * 書籍情報登録API。
     * 新規の書籍情報を登録します。
     *
     * @param registerBookRequest 登録する書籍情報を含むリクエストDTO。
     * @return 成功時: `BookWithAuthorsResponse`、失敗時: `ErrorResponse`。
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

        val createdBook = bookService.registerBook(bookDto, registerBookRequest.authorIds!!)

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook)
    }

    /**
     * 書籍情報更新API。
     * 既存の書籍データを部分的に更新します。
     *
     * @param id 更新したい書籍のID。
     * @param patchBookRequest 更新する情報を含むリクエストDTO。
     * @return 成功時: `BookWithAuthorsResponse`、失敗時: `ErrorResponse`。
     */
    @PatchMapping("/{id}")
    fun patchBook(
        @PathVariable id: Long,
        @Valid @RequestBody patchBookRequest: PatchBookRequest,
    ): ResponseEntity<BookWithAuthorsResponse> {
        val updates = mutableMapOf<String, Any?>()

        if (!patchBookRequest.title.isNullOrBlank()) {
            updates["title"] = patchBookRequest.title
        }

        if (patchBookRequest.price != null) {
            updates["price"] = patchBookRequest.price
        }

        if (patchBookRequest.publicationStatus != null) {
            updates["publicationStatus"] = patchBookRequest.publicationStatus
        }

        val updatedBookWithAuthors = bookService.updateBook(id, updates, patchBookRequest.authorIds)

        return ResponseEntity.ok(updatedBookWithAuthors)
    }
}
