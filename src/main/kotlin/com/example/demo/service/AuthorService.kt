package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.response.AuthorResponse
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.dto.response.BookResponse
import com.example.demo.repository.AuthorRepository
import com.example.demo.exception.AuthorAlreadyExistsException
import org.springframework.dao.DataIntegrityViolationException
import com.example.demo.exception.AuthorNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDate

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

    /**
     * 既存の著者データを部分的に更新し、更新後の著者情報を取得します。
     * @param id 更新対象の著者ID
     * @param updates 更新するフィールド名と値のマップ
     * @return 更新後の著者情報
     * @throws AuthorNotFoundException 指定された著者が見つからない場合
     */
    fun partialUpdateAuthor(id: Long, updates: Map<String, Any?>): AuthorDto {
        // 更新する項目がない場合は、IllegalArgumentExceptionをスロー
        if (updates.isEmpty()) {
            throw IllegalArgumentException("更新する項目がありません。")
        }

        // birthDateの未来日付チェックを追加
        updates["birthDate"]?.let {
            if (it is LocalDate && it.isAfter(LocalDate.now())) {
                throw IllegalArgumentException("birthDate: 生年月日は過去の日付である必要があります。")
            }
        }

        val updatedCount = authorRepository.updateAuthor(id, updates)

        if (updatedCount > 0) {
            return authorRepository.findById(id) ?: throw AuthorNotFoundException("更新後に著者が見つかりませんでした。")
        } else {
            throw AuthorNotFoundException("指定された著者が見つかりません。")
        }
    }

    /**
     * 指定された著者IDに紐づく全ての書籍情報と著者情報を取得し、
     * レスポンス用のDTOに変換して返却します。
     *
     * @param authorId 取得対象の著者ID
     * @return 著者情報と書籍リストを含むレスポンス用DTO。
     * @throws AuthorNotFoundException 指定された著者が見つからない場合
     */
    fun getAuthorWithBooksResponse(authorId: Long): AuthorWithBooksResponse {
        val authorDto = authorRepository.findById(authorId)

        // 著者が見つからない場合はAuthorNotFoundExceptionをスロー
        if (authorDto == null) {
            throw AuthorNotFoundException("指定された著者が見つかりません。")
        }

        val bookDtos = authorRepository.findBooksByAuthorId(authorId)

        val authorResponse = AuthorResponse(
            id = authorDto.id,
            name = authorDto.name,
            birthDate = authorDto.birthDate
        )
        val bookResponses = bookDtos.map { bookDto ->
            BookResponse(
                id = bookDto.id,
                title = bookDto.title,
                price = bookDto.price,
                publicationStatus = bookDto.publicationStatus
            )
        }

        return AuthorWithBooksResponse(authorResponse, bookResponses)
    }
}