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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 著者関連APIを処理するコントローラクラス。
 *
 * @property authorService 著者データのビジネスロジックを担うサービス。
 */
@RestController
@RequestMapping("/api/authors")
class AuthorController(
    private val authorService: AuthorService,
) {
    /**
     * 著者情報検索API。
     * 指定された著者IDに紐づく著者情報と著作の一覧を取得します。
     *
     * @param authorId 情報を取得したい著者のID。
     * @return 成功時: `AuthorWithBooksResponse`、失敗時: `ErrorResponse`。
     */
    @GetMapping("/{authorId}/books")
    fun getBooksByAuthorId(
        @PathVariable authorId: Long,
    ): ResponseEntity<AuthorWithBooksResponse> {
        val result = authorService.getAuthorWithBooksResponse(authorId)
        return ResponseEntity.ok(result)
    }

    /**
     * 著者情報登録API。
     * 新規の著者情報を登録します。
     *
     * @param registerAuthorRequest 登録する著者情報を含むリクエストDTO。
     * @return 成功時: `AuthorResponse`、失敗時: `ErrorResponse`。
     */
    @PostMapping
    fun registerAuthor(
        @Valid @RequestBody registerAuthorRequest: RegisterAuthorRequest,
    ): ResponseEntity<AuthorResponse> {
        val authorDto =
            AuthorDto(
                id = null,
                name = registerAuthorRequest.name!!,
                birthDate = registerAuthorRequest.birthDate!!,
            )

        val registeredAuthorResponse = authorService.registerAuthor(authorDto)

        return ResponseEntity.status(HttpStatus.CREATED).body(registeredAuthorResponse)
    }

    /**
     * 著者情報更新API。
     * 登録済みの著者情報を部分的に更新します。
     *
     * @param authorId 更新したい著者のID。
     * @param patchAuthorRequest 更新する情報を含むリクエストDTO。
     * @return 成功時: `AuthorResponse`、失敗時: `ErrorResponse`。
     */
    @PatchMapping("/{authorId}")
    fun patchAuthor(
        @PathVariable authorId: Long,
        @Valid @RequestBody patchAuthorRequest: PatchAuthorRequest,
    ): ResponseEntity<AuthorResponse> {
        require(
            patchAuthorRequest.id == null || patchAuthorRequest.id == authorId,
        ) { "ID in request body must be null or match path variable ID" }

        val updates = mutableMapOf<String, Any?>()

        if (!patchAuthorRequest.name.isNullOrBlank()) {
            updates["name"] = patchAuthorRequest.name
        }
        if (patchAuthorRequest.birthDate != null) {
            updates["birthDate"] = patchAuthorRequest.birthDate
        }

        val updatedAuthorDto = authorService.partialUpdateAuthor(authorId, updates)

        val updatedAuthorResponse =
            AuthorResponse(
                id = updatedAuthorDto.id,
                name = updatedAuthorDto.name,
                birthDate = updatedAuthorDto.birthDate,
            )

        return ResponseEntity.ok(updatedAuthorResponse)
    }
}
