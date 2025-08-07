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
 * 著者データ操作用サービスクラス
 */
@Service
class AuthorService(
    private val authorRepository: AuthorRepository,
) {
    /**
     * 指定された著者IDに紐づく全ての書籍情報と著者情報を取得
     *
     * @param authorId 取得対象の著者ID
     * @return 著者情報と書籍リストを含むオブジェクト
     * @throws ItemNotFoundException 指定された著者が見つからない場合
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
     * 著者情報登録
     *
     * @param authorDto 著者情報格納オブジェクト
     * @return 登録済みの著者情報
     * @throws ItemAlreadyExistsException 同一の著者が既に登録されている場合
     */
    fun registerAuthor(authorDto: AuthorDto): AuthorResponse {
        // authorテーブルへのデータ登録
        val insertedAuthorDto =
            try {
                authorRepository.insertAuthor(authorDto)
            } catch (e: DataIntegrityViolationException) {
                throw ItemAlreadyExistsException(itemType = "著者")
            } ?: throw IllegalStateException("登録した著者情報が見つかりません。")

        // 新しいレスポンスDTOを構築して登録結果を返す
        return AuthorResponse(
            id = insertedAuthorDto.id,
            name = insertedAuthorDto.name,
            birthDate = insertedAuthorDto.birthDate,
        )
    }

    /**
     * 既存の著者データを部分的に更新し、更新後の著者情報を取得します。
     * @param id 更新対象の著者ID
     * @param updates 更新するフィールド名と値のマップ
     * @return 更新後の著者情報
     * @throws AuthorNotFoundException 指定された著者が見つからない場合
     */
    fun partialUpdateAuthor(
        id: Long,
        updates: Map<String, Any?>,
    ): AuthorDto {
        // 更新する項目がない場合は、IllegalArgumentExceptionをスロー
        // この仕様はこのAPIの仕様（ビジネスロジック）なのでServiceクラスに実装
        if (updates.isEmpty()) {
            throw IllegalArgumentException("error.nothing.update")
        }

        val updatedCount = authorRepository.updateAuthor(id, updates)

        if (updatedCount > 0) {
            // ここでUnexpectedExceptionになるのは、登録処理後に登録した著者IDが取得できないというイレギュラーな場合
            return authorRepository.findById(id) ?: throw UnexpectedException(
                message = "著者情報更新APIにて著者データの登録が完了しましたが、著者IDが取得できません。",
            )
        } else {
            throw ItemNotFoundException(itemType = "著者ID")
        }
    }
}
