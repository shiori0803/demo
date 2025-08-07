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
 * 書籍データ操作用サービスクラス
 *
 * memo
 * Kotlinのクラスはデフォルトで"final"のため明示的に"open"を付けないと他クラスから継承できない
 * Springの@Transactionalアノテーションは実行時にプロキシ（代理）オブジェクト※を生成する
 * プロキシオブジェクトはBookServiceクラスを継承したサブクラスとして作成される
 * そのためこのクラスには明示的に"open"を付けないとエラーになる
 * ※Springフレームワークが実行時に動的に生成する、BookServiceクラスを継承した新しいクラスのオブジェクト
 * @Transactionalは実行時に元のクラスを継承したプロキシ（代理）オブジェクトを生成することでトランザクション機能を実装している
 */
@Service
open class BookService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
    private val bookAuthorsRepository: BookAuthorsRepository,
) {
    // ----------------- プライベート関数 -----------------

    /**
     * DataIntegrityViolationExceptionをItemAlreadyExistsExceptionに変換する共通処理。
     * @param itemType 例外メッセージで使用する項目名
     * @param block 実行するデータベース操作のラムダ式
     * @return ラムダ式の実行結果
     * @throws ItemAlreadyExistsException DataIntegrityViolationExceptionが発生した場合
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
     * BookDtoとauthorIdsからBookWithAuthorsResponseを生成する共通処理。
     * @param book 書籍情報Dto
     * @param authorIds 著者IDリスト
     * @return 生成されたBookWithAuthorsResponse
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
     * @param authorIds 著者IDリスト
     * @throws ItemNotFoundException 存在しない著者IDが含まれている場合
     */
    private fun validateAuthorIds(authorIds: List<Long>) {
        if (authorIds.size.toLong() != authorRepository.existsAllByIds(authorIds)) {
            throw ItemNotFoundException(itemType = "著者ID")
        }
    }

    // ----------------- ビジネスロジック -----------------

    /**
     * 書籍情報登録
     *
     * @param bookDto 書籍情報格納オブジェクト
     * @param authorIds 書籍の著者IDリスト
     * @return BookWithAuthorsResponse 登録が完了した書籍とそれに紐づく著者のIDリスト
     * @throws ItemNotFoundException 指定の著者が見つからない場合
     * @throws ItemAlreadyExistsException 同一の書籍が既に登録されている場合
     */
    @Transactional
    open fun registerBook(
        bookDto: BookDto,
        authorIds: List<Long>,
    ): BookWithAuthorsResponse {
        // 著者IDリストが空の場合は例外をスロー
        if (authorIds.isEmpty()) {
            throw IllegalArgumentException("validation.authorIds.size.min")
        }
        // 著者IDの存在チェック
        validateAuthorIds(authorIds)

        // booksテーブルへのデータ登録
        val insertedBookDto =
            wrapDataIntegrityViolationException(itemType = "書籍") {
                bookRepository.insertBook(bookDto)
            } ?: throw IllegalStateException()

        // book_authorsテーブルへのデータ登録
        authorIds.forEach { authorId ->
            val bookAuthorDto =
                BookAuthorDto(
                    // idはDBから取得したもので非nullが保証されている
                    bookId = insertedBookDto.id!!,
                    authorId = authorId,
                )
            wrapDataIntegrityViolationException(itemType = "著者と書籍の組み合わせ") {
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
        }

        val registeredAuthorIds = bookAuthorsRepository.findAuthorIdsByBookId(insertedBookDto.id!!)

        // 新しいレスポンスDTOを構築して登録結果を返す
        return createBookWithAuthorsResponse(insertedBookDto, registeredAuthorIds)
    }

    /**
     * 既存の書籍データを部分的に更新します (PATCHセマンティクス)。
     * この操作はトランザクションとして実行されます。
     * @param id 更新対象の書籍ID
     * @param updates 更新するフィールド名と値のマップ (title, price, publicationStatus)
     * @param newAuthorIds 新しく関連付ける著者IDのリスト (nullの場合は著者関連を更新しない)
     * @return 更新された書籍データ (IDを含む)
     * @throws ItemNotFoundException 指定されたデータが見つからない場合
     * @throws InvalidPublicationStatusChangeException 出版済から未出版への変更が試みられた場合
     */
    @Transactional
    open fun updateBook(
        id: Long,
        updates: Map<String, Any?>,
        newAuthorIds: List<Long>?,
    ): BookWithAuthorsResponse {
        // 書籍の存在チェック
        val existingBook = bookRepository.findById(id) ?: throw ItemNotFoundException(itemType = "書籍ID")

        // 出版済み→未出版への変更をチェック
        val newPublicationStatus = updates["publicationStatus"] as? Int
        if (existingBook.publicationStatus == 1 && newPublicationStatus == 0) {
            throw InvalidPublicationStatusChangeException()
        }

        // booksテーブルへのデータ更新 (updatesマップが空でなければ実行)
        if (updates.isNotEmpty()) {
            val updatedCount = bookRepository.updateBook(id, updates)
            if (updatedCount == 0) {
                // updatesマップが空でないのに更新件数が0の場合、書籍が見つからないか、他の問題
                // findByIdで存在確認済みなので、ここに来ることは稀だが、念のため
                throw ItemNotFoundException(itemType = "書籍ID")
            }
        }

        // book_authorsテーブルへのデータ更新 (newAuthorIdsが指定された場合のみ)
        // 書籍に紐づく著者の項目が更新対象になっているか確認
        if (!newAuthorIds.isNullOrEmpty()) {
            // 更新する場合は更新する予定の著者IDが存在するかチェック
            validateAuthorIds(newAuthorIds)
            // 指定の書籍IDに紐づくレコードを削除する
            bookAuthorsRepository.deleteBookAuthorsByBookId(id)
            // 新規データを登録する
            // book_authorsテーブルへのデータ登録
            newAuthorIds.forEach { authorId ->
                val bookAuthorDto =
                    BookAuthorDto(
                        bookId = id,
                        authorId = authorId,
                    )
                bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
            }
        }

        // 更新後の書籍データと著者IDを取得して返す
        // 更新後書籍データの取得
        val updatedBookDto = bookRepository.findById(id) ?: throw IllegalStateException()
        // 更新後書籍に紐づく著者データの取得
        val finalAuthorIds = newAuthorIds ?: bookAuthorsRepository.findAuthorIdsByBookId(id)

        // 新しいレスポンスDTOを構築して登録結果を返す
        return createBookWithAuthorsResponse(updatedBookDto, finalAuthorIds)
    }
}
