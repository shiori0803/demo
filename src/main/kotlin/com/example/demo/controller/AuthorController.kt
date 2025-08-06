package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.request.PatchAuthorRequest
import com.example.demo.dto.request.RegisterAuthorRequest
import com.example.demo.dto.response.AuthorResponse
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.service.AuthorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/authors")
class AuthorController(private val authorService: AuthorService) {

    /**
     * 著者情報検索API
     * 著者の情報と著作の一覧を取得する
     *
     * @param authorId
     * @return 成功時：AuthorWithBooksResponse,失敗時：ErrorResponse
     */
    @GetMapping("/{authorId}/books")
    fun getBooksByAuthorId(@PathVariable authorId: Long): ResponseEntity<AuthorWithBooksResponse> {
        // 検索処理の実行
        val result = authorService.getAuthorWithBooksResponse(authorId)
        return ResponseEntity.ok(result)
    }

    /**
     * 著者情報登録API
     *
     * @param registerAuthorRequest
     * @return 成功時：AuthorResponse,失敗時：ErrorResponse
     */
    @PostMapping
    fun registerAuthor(@Valid @RequestBody registerAuthorRequest: RegisterAuthorRequest): ResponseEntity<AuthorResponse> {
        // リクエストの格納
        val authorDto = AuthorDto(
            id = null, name = registerAuthorRequest.name!!, birthDate = registerAuthorRequest.birthDate!!
        )

        // 登録処理の実行
        val createdAuthorDto = authorService.registerAuthor(authorDto)

        // レスポンスの格納
        val createdAuthorResponse = AuthorResponse(
            id = createdAuthorDto.id, name = createdAuthorDto.name, birthDate = createdAuthorDto.birthDate
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAuthorResponse)
    }

    @PatchMapping("/{id}")
    fun patchAuthor(
        @PathVariable id: Long, @RequestBody patchAuthorRequest: PatchAuthorRequest
    ): ResponseEntity<AuthorDto> {
        require(patchAuthorRequest.id == null || patchAuthorRequest.id == id) { "ID in request body must be null or match path variable ID" }

        val updates = mutableMapOf<String, Any?>()

        // nameがnullまたは空文字・空白でない場合のみマップに追加
        if (!patchAuthorRequest.name.isNullOrBlank()) {
            updates["name"] = patchAuthorRequest.name
        }
        // birthDateがnullでない場合のみマップに追加
        if (patchAuthorRequest.birthDate != null) {
            updates["birthDate"] = patchAuthorRequest.birthDate
        }

        val updatedAuthor = authorService.partialUpdateAuthor(id, updates)

        return ResponseEntity.ok(updatedAuthor)
    }


}
