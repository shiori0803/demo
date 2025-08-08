package com.example.demo.service

import com.example.demo.dto.BookAuthorDto
import com.example.demo.dto.BookDto
import com.example.demo.dto.response.BookWithAuthorsResponse
import com.example.demo.exception.InvalidPublicationStatusChangeException
import com.example.demo.exception.ItemAlreadyExistsException
import com.example.demo.exception.ItemNotFoundException
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.BookAuthorsRepository
import com.example.demo.repository.BookRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 書籍データ操作のビジネスロジックを担うサービスクラス。
 *
 * Kotlinのクラスはデフォルトで`final`のため、Spring AOPがプロキシを生成できるように、
 * `@Transactional`が付いているクラスとメソッドには`open`を付与する必要があります。
 *
 * @property bookRepository 書籍データへのアクセスを担うリポジトリ。
 * @property authorRepository 著者データへのアクセスを担うリポジトリ。
 * @property bookAuthorsRepository 書籍と著者の関連データへのアクセスを担うリポジトリ。
 */
@Service
open class BookService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
    private val bookAuthorsRepository: BookAuthorsRepository,
) {
    /**
     * `DataIntegrityViolationException`を`ItemAlreadyExistsException`に変換する共通処理。
     *
     * @param itemType 例外メッセージで使用する項目名。
     * @param block 実行するデータベース操作のラムダ式。
     * @return ラムダ式の実行結果。
     * @throws ItemAlreadyExistsException `DataIntegrityViolationException`が発生した場合。
     */
    private fun <T> wrapDataIntegrityViolationException(
        itemType: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: DataIntegrityViolationException) {
            throw ItemAlreadyExistsException(itemType = itemType)
        }

    /**
     * `BookDto`と著者IDリストから`BookWithAuthorsResponse`を生成する共通処理。
     *
     * @param book 書籍情報DTO。
     * @param authorIds 著者IDのリスト。
     * @return 生成された`BookWithAuthorsResponse`。
     */
    private fun createBookWithAuthorsResponse(
        book: BookDto,
        authorIds: List<Long>,
    ): BookWithAuthorsResponse =
        BookWithAuthorsResponse(
            id = book.id!!,
            title = book.title,
            price = book.price,
            publicationStatus = book.publicationStatus,
            authorIds = authorIds,
        )

    /**
     * 著者IDリストの存在チェックを行う共通処理。
     *
     * @param authorIds 著者IDのリスト。
     * @throws ItemNotFoundException 存在しない著者IDが含まれている場合。
     */
    private fun validateAuthorIds(authorIds: List<Long>) {
        if (authorIds.size.toLong() != authorRepository.existsAllByIds(authorIds)) {
            throw ItemNotFoundException(itemType = "著者ID")
        }
    }

    /**
     * 書籍情報を登録します。
     *
     * @param bookDto 登録する書籍情報DTO。
     * @param authorIds 書籍に紐づく著者IDのリスト。
     * @return 登録が完了した書籍と、それに紐づく著者IDリストを含むレスポンスDTO。
     * @throws IllegalArgumentException 著者IDリストが空の場合。
     * @throws ItemNotFoundException 指定された著者が見つからない場合。
     * @throws ItemAlreadyExistsException 同一の書籍が既に登録されている場合。
     * @throws IllegalStateException 登録に失敗した場合。
     */
    @Transactional
    open fun registerBook(
        bookDto: BookDto,
        authorIds: List<Long>,
    ): BookWithAuthorsResponse {
        if (authorIds.isEmpty()) {
            throw IllegalArgumentException("validation.authorIds.size.min")
        }
        validateAuthorIds(authorIds)

        val insertedBookDto =
            wrapDataIntegrityViolationException(itemType = "書籍") {
                bookRepository.insertBook(bookDto)
            } ?: throw IllegalStateException()

        authorIds.forEach { authorId ->
            val bookAuthorDto =
                BookAuthorDto(
                    bookId = insertedBookDto.id!!,
                    authorId = authorId,
                )
            wrapDataIntegrityViolationException(itemType = "著者と書籍の組み合わせ") {
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
        }

        val registeredAuthorIds = bookAuthorsRepository.findAuthorIdsByBookId(insertedBookDto.id!!)

        return createBookWithAuthorsResponse(insertedBookDto, registeredAuthorIds)
    }

    /**
     * 既存の書籍データを部分的に更新します (PATCHセマンティクス)。
     * この操作はトランザクションとして実行されます。
     *
     * @param id 更新対象の書籍ID。
     * @param updates 更新するフィールド名と値のマップ (title, price, publicationStatus)。
     * @param newAuthorIds 新しく関連付ける著者IDのリスト。nullの場合は著者関連を更新しません。
     * @return 更新された書籍データと、それに紐づく著者IDリストを含むレスポンスDTO。
     * @throws ItemNotFoundException 指定された書籍が見つからない場合。
     * @throws InvalidPublicationStatusChangeException 出版済から未出版への変更が試みられた場合。
     * @throws IllegalStateException 更新後の書籍データまたは著者データが取得できない予期せぬ事態が発生した場合。
     */
    @Transactional
    open fun updateBook(
        id: Long,
        updates: Map<String, Any?>,
        newAuthorIds: List<Long>?,
    ): BookWithAuthorsResponse {
        val existingBook = bookRepository.findById(id) ?: throw ItemNotFoundException(itemType = "書籍ID")

        val newPublicationStatus = updates["publicationStatus"] as? Int
        if (existingBook.publicationStatus == 1 && newPublicationStatus == 0) {
            throw InvalidPublicationStatusChangeException()
        }

        if (updates.isNotEmpty()) {
            val updatedCount = bookRepository.updateBook(id, updates)
            if (updatedCount == 0) {
                throw ItemNotFoundException(itemType = "書籍ID")
            }
        }

        if (!newAuthorIds.isNullOrEmpty()) {
            validateAuthorIds(newAuthorIds)
            bookAuthorsRepository.deleteBookAuthorsByBookId(id)
            newAuthorIds.forEach { authorId ->
                val bookAuthorDto =
                    BookAuthorDto(
                        bookId = id,
                        authorId = authorId,
                    )
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
        }

        val updatedBookDto = bookRepository.findById(id) ?: throw IllegalStateException()
        val finalAuthorIds = newAuthorIds ?: bookAuthorsRepository.findAuthorIdsByBookId(id)

        return createBookWithAuthorsResponse(updatedBookDto, finalAuthorIds)
    }
}
