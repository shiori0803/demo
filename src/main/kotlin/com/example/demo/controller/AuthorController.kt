package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.request.InsertAuthorRequest
import com.example.demo.dto.request.UpdateAuthorRequest
import com.example.demo.service.AuthorService // AuthorService をインポート
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/authors")
class AuthorController(private val authorService: AuthorService) { // AuthorService を注入

    /**
     * 新しい著者を作成するAPI
     * POST /api/authors
     */
    @PostMapping
    fun createAuthor(@Valid @RequestBody insertAuthorRequest: InsertAuthorRequest): ResponseEntity<AuthorDto> { // 戻り値をAuthorDtoに

        // リクエストデータのバリデーションと受取
        // 新規作成リクエストにIDが含まれている場合はエラーとする
        require(insertAuthorRequest.id == null) { "ID must be null for new author creation" }
        val authorDto = AuthorDto(
            id = null, // ここを 0L から null に変更
            firstName = insertAuthorRequest.firstName!!,
            middleName = insertAuthorRequest.middleName,
            lastName = insertAuthorRequest.lastName!!,
            birthDate = insertAuthorRequest.birthDate!!
        )

        // サービス層を呼び出し、エラーハンドリングはGlobalExceptionHandlerに委任
        val createdAuthor = authorService.registerAuthor(authorDto)

        // 成功した場合、CREATEDステータスと作成された著者データを返す
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdAuthor)
    }


    /**
     * 既存の著者を更新するAPI
     * /api/authors/{id}
     */
    @PatchMapping("/{id}")
    fun updateAuthor(@PathVariable id: Long, @RequestBody updateAuthorRequest: UpdateAuthorRequest): ResponseEntity<Any> {
        require(updateAuthorRequest.id == null || updateAuthorRequest.id == id) { "ID in request body must be null or match path variable ID" }

        val updates = mutableMapOf<String, Any?>()

        updateAuthorRequest.firstName?.let { updates["firstName"] = it }
        updates["middleName"] = updateAuthorRequest.middleName // middleName は null で更新可能
        updateAuthorRequest.lastName?.let { updates["lastName"] = it }
        updateAuthorRequest.birthDate?.let { updates["birthDate"] = it }

        val updatedCount = authorService.partialUpdateAuthor(id, updates)

        return if (updatedCount > 0) {
            ResponseEntity.ok(updatedCount)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
