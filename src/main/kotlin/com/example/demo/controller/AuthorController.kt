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

@RestController
@RequestMapping("/api/authors")
class AuthorController(
    private val authorService: AuthorService,
) {
    /**
     * 著者情報検索API
     * 著者の情報と著作の一覧を取得する
     *
     * @param authorId
     * @return 成功時：AuthorWithBooksResponse,失敗時：ErrorResponse
     */
    @GetMapping("/{authorId}/books")
    fun getBooksByAuthorId(
        @PathVariable authorId: Long,
    ): ResponseEntity<AuthorWithBooksResponse> {
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
    fun registerAuthor(
        @Valid @RequestBody registerAuthorRequest: RegisterAuthorRequest,
    ): ResponseEntity<AuthorResponse> {
        // リクエストの格納
        val authorDto =
            AuthorDto(
                id = null,
                name = registerAuthorRequest.name!!,
                birthDate = registerAuthorRequest.birthDate!!,
            )

        // 登録処理の実行
        val registeredAuthorResponse = authorService.registerAuthor(authorDto)

        return ResponseEntity.status(HttpStatus.CREATED).body(registeredAuthorResponse)
    }

    /**
     * 著者情報登録API
     *
     * @param authorId
     * @return 成功時：AuthorResponse,失敗時：ErrorResponse
     */
    @PatchMapping("/{authorId}")
    fun patchAuthor(
        @PathVariable authorId: Long,
        @RequestBody patchAuthorRequest: PatchAuthorRequest,
    ): ResponseEntity<AuthorResponse> {
        // リクエストボディのIDがパスのIDと一致しない、または存在する場合はエラーとする
        require(
            patchAuthorRequest.id == null || patchAuthorRequest.id == authorId,
        ) { "ID in request body must be null or match path variable ID" }

        // 更新するフィールドと値のマップを構築
        val updates = mutableMapOf<String, Any?>()

        // nameがnullまたは空文字・空白でない場合のみマップに追加
        if (!patchAuthorRequest.name.isNullOrBlank()) {
            updates["name"] = patchAuthorRequest.name
        }
        // birthDateがnullでない場合のみマップに追加
        if (patchAuthorRequest.birthDate != null) {
            updates["birthDate"] = patchAuthorRequest.birthDate
        }

        // サービス層を呼び出し、更新された著者データを取得
        val updatedAuthorDto = authorService.partialUpdateAuthor(authorId, updates)

        // レスポンスの格納
        val updatedAuthorResponse =
            AuthorResponse(
                id = updatedAuthorDto.id,
                name = updatedAuthorDto.name,
                birthDate = updatedAuthorDto.birthDate,
            )

        return ResponseEntity.ok(updatedAuthorResponse)
    }
}
