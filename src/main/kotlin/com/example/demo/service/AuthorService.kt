package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.repository.AuthorRepository
import com.example.demo.exception.AuthorAlreadyExistsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * 著者データ操作用サービスクラス
 */
@Service
class AuthorService(private val authorRepository: AuthorRepository) {

    fun registerAuthor(authorDto: AuthorDto): AuthorDto {
        try {
            val insertedId = authorRepository.insertAuthor(authorDto)
            return authorDto.copy(id = insertedId!!)
        } catch (e: DataIntegrityViolationException) {
            throw AuthorAlreadyExistsException("この著者は既に登録されています。")
        }
    }

    fun partialUpdateAuthor(id: Long, updates: Map<String, Any?>): Int {
        return authorRepository.updateAuthor(id, updates)
    }
}