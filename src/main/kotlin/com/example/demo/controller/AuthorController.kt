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

    @PostMapping
    fun createAuthor(@Valid @RequestBody insertAuthorRequest: InsertAuthorRequest): ResponseEntity<AuthorDto> {
        require(insertAuthorRequest.id == null) { "ID must be null for new author creation" }

        val authorDto = AuthorDto(
            id = null,
            name = insertAuthorRequest.name!!,
            birthDate = insertAuthorRequest.birthDate!!
        )

        val createdAuthor = authorService.registerAuthor(authorDto)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdAuthor)
    }

    @PatchMapping("/{id}")
    fun updateAuthor(@PathVariable id: Long, @RequestBody updateAuthorRequest: UpdateAuthorRequest): ResponseEntity<Any> {
        require(updateAuthorRequest.id == null || updateAuthorRequest.id == id) { "ID in request body must be null or match path variable ID" }

        val updates = mutableMapOf<String, Any?>()

        //nullでない値が提供された場合のみマップに追加
        updateAuthorRequest.name?.let { updates["name"] = it }
        updateAuthorRequest.birthDate?.let { updates["birthDate"] = it }

        val updatedCount = authorService.partialUpdateAuthor(id, updates)

        return if (updatedCount > 0) {
            ResponseEntity.ok(updatedCount)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
