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

    /**
     * 著者データ登録
     * @param authorDto 新規登録する著者データを含むDTO
     * @return 登録されたレコード
     */
    fun registerAuthor(authorDto: AuthorDto): AuthorDto {
        try {
            val insertedId = authorRepository.insertAuthor(authorDto)
            return authorDto.copy(id = insertedId!!)
        } catch (e: DataIntegrityViolationException) {
            // 今回定義した制約はデータ重複のみなので一律重複エラーとして扱う
            throw AuthorAlreadyExistsException("この著者は既に登録されています。")
        }
    }

    /**
     * 著者データ更新
     */
    fun partialUpdateAuthor(id: Long, updates: Map<String, Any?>): Int {
        return authorRepository.updateAuthor(id, updates)
    }
}