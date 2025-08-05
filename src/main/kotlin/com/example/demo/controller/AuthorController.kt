package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.request.InsertAuthorRequest
import com.example.demo.dto.request.PatchAuthorRequest
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.service.AuthorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/authors")
class AuthorController(private val authorService: AuthorService) { // AuthorService を注入

    @PostMapping
    fun createAuthor(@Valid @RequestBody insertAuthorRequest: InsertAuthorRequest): ResponseEntity<AuthorDto> {
        require(insertAuthorRequest.id == null) { "ID must be null for new author creation" }

        val authorDto = AuthorDto(
            id = null, name = insertAuthorRequest.name!!, birthDate = insertAuthorRequest.birthDate!!
        )

        val createdAuthor = authorService.registerAuthor(authorDto)

        return ResponseEntity.status(HttpStatus.CREATED).body(createdAuthor)
    }

    @PatchMapping("/{id}")
    fun patchAuthor(@PathVariable id: Long, @RequestBody patchAuthorRequest: PatchAuthorRequest): ResponseEntity<AuthorDto> {
        require(patchAuthorRequest.id == null || patchAuthorRequest.id == id) { "ID in request body must be null or match path variable ID" }

        val updates = mutableMapOf<String, Any?>()

        //nullでない値が提供された場合のみマップに追加
        patchAuthorRequest.name?.let { updates["name"] = it }
        patchAuthorRequest.birthDate?.let { updates["birthDate"] = it }

        val updatedAuthor = authorService.partialUpdateAuthor(id, updates)

        return ResponseEntity.ok(updatedAuthor)
    }

    @GetMapping("/{authorId}/books")
    fun getBooksByAuthorId(@PathVariable authorId: Long): ResponseEntity<AuthorWithBooksResponse> {
        val result = authorService.getAuthorWithBooksResponse(authorId)
        return ResponseEntity.ok(result)
    }
}
