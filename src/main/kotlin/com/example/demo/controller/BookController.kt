package com.example.demo.controller

import com.example.demo.dto.BookDto
import com.example.demo.dto.request.InsertBookRequest
import com.example.demo.dto.request.UpdateBookRequest // 追加
import com.example.demo.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping // 追加
import org.springframework.web.bind.annotation.PatchMapping // 追加
import org.springframework.web.bind.annotation.PathVariable // 追加
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 書籍関連APIのコントローラ
 */
@RestController
@RequestMapping("/api/books")
class BookController(private val bookService: BookService) {

    /**
     * 新しい書籍を作成するAPI
     * POST /api/books
     */
    @PostMapping
    fun createBook(@Valid @RequestBody insertBookRequest: InsertBookRequest): ResponseEntity<BookDto> {
        // リクエストにIDが含まれている場合はエラーとする（新規作成のため）
        require(insertBookRequest.id == null) { "ID must be null for new book creation" }

        // Request DTOをServiceが受け取るBookDtoに変換
        val bookDto = BookDto(
            id = null, // DBで自動生成されるためnull
            title = insertBookRequest.title!!,
            price = insertBookRequest.price!!,
            publicationStatus = insertBookRequest.publicationStatus!!
        )

        // サービス層を呼び出し、エラーハンドリングはGlobalExceptionHandlerに委任
        val createdBook = bookService.registerBook(bookDto, insertBookRequest.authorIds!!)

        // 成功した場合、CREATEDステータスと作成された書籍データを返す
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook)
    }

    /**
     * 指定されたIDの書籍データを取得するAPI
     * GET /api/books/{id}
     */
    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<BookDto> {
        val bookDto = bookService.findBookById(id)
        return if (bookDto != null) {
            ResponseEntity.ok(bookDto)
        } else {
            // 書籍が見つからない場合はGlobalExceptionHandlerでBookNotFoundExceptionが捕捉される
            // ここでは直接NotFoundを返す
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 既存の書籍データを部分的に更新するAPI
     * PATCH /api/books/{id}
     */
    @PatchMapping("/{id}")
    fun updateBook(
        @PathVariable id: Long, @Valid @RequestBody updateBookRequest: UpdateBookRequest
    ): ResponseEntity<BookDto> {
        // リクエストボディのIDがパスのIDと一致しない、または存在する場合はエラーとする
        require(updateBookRequest.id == null || updateBookRequest.id == id) { "ID in request body must be null or match path variable ID" }

        // 更新するフィールドと値のマップを構築
        val updates = mutableMapOf<String, Any?>()

        // title (NOT NULLカラム) - nullでない値が提供された場合のみマップに追加
        updateBookRequest.title?.let { updates["title"] = it }

        // price (NOT NULLカラム) - nullでない値が提供された場合のみマップに追加
        updateBookRequest.price?.let { updates["price"] = it }

        // publicationStatus (NOT NULLカラム) - nullでない値が提供された場合のみマップに追加
        updateBookRequest.publicationStatus?.let { updates["publicationStatus"] = it }

        // サービス層を呼び出し、更新された書籍データを取得
        // newAuthorIdsは、nullの場合は更新しない、空リストの場合は関連を全て削除、と解釈される
        val updatedBook = bookService.updateBook(id, updates, updateBookRequest.authorIds)

        // 成功した場合、OKステータスと更新された書籍データを返す
        return ResponseEntity.ok(updatedBook)
    }
}
