package com.example.demo.service

import com.example.demo.dto.AuthorDto
import com.example.demo.dto.response.AuthorResponse
import com.example.demo.dto.response.AuthorWithBooksResponse
import com.example.demo.dto.response.BookResponse
import com.example.demo.exception.ItemAlreadyExistsException
import com.example.demo.exception.ItemNotFoundException
import com.example.demo.exception.UnexpectedException
import com.example.demo.repository.AuthorRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * 著者データ操作のビジネスロジックを担うサービスクラス。
 *
 * @property authorRepository 著者データへのアクセスを担うリポジトリ。
 */
@Service
class AuthorService(
    private val authorRepository: AuthorRepository,
) {
    /**
     * 指定された著者IDに紐づくすべての書籍情報と著者情報を取得します。
     *
     * @param authorId 取得対象の著者ID。
     * @return 著者情報と書籍リストを含むオブジェクト。
     * @throws ItemNotFoundException 指定された著者が見つからない場合。
     */
    fun getAuthorWithBooksResponse(authorId: Long): AuthorWithBooksResponse {
        val authorDto = authorRepository.findById(authorId)

        if (authorDto == null) {
            throw ItemNotFoundException(itemType = "著者ID")
        }

        val bookDtos = authorRepository.findBooksByAuthorId(authorId)

        val authorResponse =
            AuthorResponse(
                id = authorDto.id,
                name = authorDto.name,
                birthDate = authorDto.birthDate,
            )
        val bookResponses =
            bookDtos.map { bookDto ->
                BookResponse(
                    id = bookDto.id,
                    title = bookDto.title,
                    price = bookDto.price,
                    publicationStatus = bookDto.publicationStatus,
                )
            }

        return AuthorWithBooksResponse(authorResponse, bookResponses)
    }

    /**
     * 著者情報を登録します。
     *
     * @param authorDto 登録する著者情報を含むDTO。
     * @return 登録された著者情報を含むレスポンスDTO。
     * @throws ItemAlreadyExistsException 同一の著者が既に登録されている場合。
     * @throws IllegalStateException 登録に失敗した場合。
     */
    fun registerAuthor(authorDto: AuthorDto): AuthorResponse {
        val insertedAuthorDto =
            try {
                authorRepository.insertAuthor(authorDto)
            } catch (e: DataIntegrityViolationException) {
                throw ItemAlreadyExistsException(itemType = "著者")
            } ?: throw IllegalStateException("登録した著者情報が見つかりません。")

        return AuthorResponse(
            id = insertedAuthorDto.id,
            name = insertedAuthorDto.name,
            birthDate = insertedAuthorDto.birthDate,
        )
    }

    /**
     * 既存の著者データを部分的に更新します (PATCHセマンティクス)。
     *
     * @param id 更新対象の著者ID。
     * @param updates 更新するフィールド名と値のマップ。
     * @return 更新後の著者情報を含むDTO。
     * @throws ItemNotFoundException 指定された著者IDが見つからない場合。
     * @throws UnexpectedException データベースの更新後にデータが取得できないという予期せぬ事態が発生した場合。
     */
    fun partialUpdateAuthor(
        id: Long,
        updates: Map<String, Any?>,
    ): AuthorResponse {
        val existingAuthorDto = authorRepository.findById(id) ?: throw ItemNotFoundException(itemType = "著者ID")

        if (updates.isEmpty()) {
            return AuthorResponse(
                id = existingAuthorDto.id,
                name = existingAuthorDto.name,
                birthDate = existingAuthorDto.birthDate,
            )
        }

        val updatedCount = authorRepository.updateAuthor(id, updates)

        return if (updatedCount > 0) {
            val updatedAuthorDto =
                authorRepository.findById(id) ?: throw UnexpectedException(
                    message = "著者情報更新APIにて著者データの登録が完了しましたが、著者IDが取得できません。",
                )
            AuthorResponse(
                id = updatedAuthorDto.id,
                name = updatedAuthorDto.name,
                birthDate = updatedAuthorDto.birthDate,
            )
        } else {
            throw ItemNotFoundException(itemType = "著者ID")
        }
    }
}
