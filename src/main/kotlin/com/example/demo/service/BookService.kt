package com.example.demo.service

import com.example.demo.dto.BookAuthorDto
import com.example.demo.dto.BookDto
import com.example.demo.exception.AuthorNotFoundException
import com.example.demo.exception.BookNotFoundException
import com.example.demo.exception.InvalidPublicationStatusChangeException
import com.example.demo.repository.AuthorRepository
import com.example.demo.repository.BookAuthorsRepository
import com.example.demo.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.demo.dto.response.BookWithAuthorsResponse

/**
 * 書籍データ操作用サービスクラス
 * memo
 * Kotlinのクラスはデフォルトで"final"のため明示的に"open"を付けないと他クラスから継承できない
 * Springの@Transactionalアノテーションは実行時にプロキシ（代理）オブジェクトを生成する
 * プロキシオブジェクトは元の暮らすを継承したサブクラスとして作成される
 * そのためこのクラスには明示的に"open"を付けないとエラーになる
 */
@Service
open class BookService(
    private val bookRepository: BookRepository, private val authorRepository: AuthorRepository, // 著者存在チェック用
    private val bookAuthorsRepository: BookAuthorsRepository
) {

    /**
     * 新しい書籍を登録し、関連する著者情報を紐付けます。
     * この操作はトランザクションとして実行されます。
     * @param bookDto 登録する書籍データ
     * @param authorIds 関連付ける著者IDのリスト
     * @return 登録された書籍データ (IDと著者IDを含む)
     * @throws AuthorNotFoundException 存在しない著者IDが含まれている場合
     */
    @Transactional
    open fun registerBook(bookDto: BookDto, authorIds: List<Long>): BookWithAuthorsResponse {
        // 1. 著者IDの存在チェック
        authorIds.forEach { authorId ->
            if (!authorRepository.existsById(authorId)) {
                throw AuthorNotFoundException("著者ID: $authorId が見つかりません。")
            }
        }

        // 2. booksテーブルへのデータ登録
        val insertedBookId = bookRepository.insertBook(bookDto)
            ?: throw IllegalStateException("書籍の登録に失敗しました。")

        // 3. book_authorsテーブルへのデータ登録
        authorIds.forEach { authorId ->
            val bookAuthorDto = BookAuthorDto(
                bookId = insertedBookId, authorId = authorId
            )
            bookAuthorsRepository.insertBookAuthor(bookAuthorDto)
        }

        // 4. 新しいレスポンスDTOを構築して返す
        return BookWithAuthorsResponse(
            id = insertedBookId,
            title = bookDto.title,
            price = bookDto.price,
            publicationStatus = bookDto.publicationStatus,
            authorIds = authorIds
        )
    }

    /**
     * 指定されたIDの書籍データを取得します。
     * @param id 取得対象の書籍ID
     * @return 指定されたIDの書籍データ。見つからない場合はnull。
     */
    fun findBookById(id: Long): BookDto? {
        return bookRepository.findById(id)
    }

    /**
     * 既存の書籍データを部分的に更新します (PATCHセマンティクス)。
     * この操作はトランザクションとして実行されます。
     * @param id 更新対象の書籍ID
     * @param updates 更新するフィールド名と値のマップ (title, price, publicationStatus)
     * @param newAuthorIds 新しく関連付ける著者IDのリスト (nullの場合は著者関連を更新しない)
     * @return 更新された書籍データ (IDを含む)
     * @throws BookNotFoundException 指定された書籍IDが見つからない場合
     * @throws InvalidPublicationStatusChangeException 出版済から未出版への変更が試みられた場合
     * @throws AuthorNotFoundException 存在しない著者IDが含まれている場合 (newAuthorIdsが指定された場合)
     */
    @Transactional
    open fun updateBook(id: Long, updates: Map<String, Any?>, newAuthorIds: List<Long>?): BookDto {
        // 1. 書籍の存在チェックと現在のデータ取得
        val existingBook = bookRepository.findById(id) ?: throw BookNotFoundException("書籍ID: $id が見つかりません。")

        // 2. publicationStatusの変更ロジック
        val newPublicationStatus = updates["publicationStatus"] as? Int
        if (existingBook.publicationStatus == 1 && newPublicationStatus == 0) {
            throw InvalidPublicationStatusChangeException("出版済から未出版への変更はできません。")
        }

        // 3. 著者IDの存在チェック (newAuthorIdsが指定された場合のみ)
        newAuthorIds?.forEach { authorId ->
            if (!authorRepository.existsById(authorId)) {
                throw AuthorNotFoundException("著者ID: $authorId が見つかりません。")
            }
        }

        // 4. booksテーブルへのデータ更新 (updatesマップが空でなければ実行)
        // updatesマップが空の場合、bookRepository.updateBookは0を返すため、
        // その後にBookNotFoundExceptionをスローしないようにする。
        if (updates.isNotEmpty()) {
            val updatedCount = bookRepository.updateBook(id, updates)
            if (updatedCount == 0) {
                // updatesマップが空でないのに更新件数が0の場合、書籍が見つからないか、他の問題
                // findByIdで存在確認済みなので、ここに来ることは稀だが、念のため
                throw BookNotFoundException("書籍ID: $id の更新に失敗しました。")
            }
        }


        // 5. book_authorsテーブルへのデータ更新 (newAuthorIdsが指定された場合のみ)
        newAuthorIds?.let {
            bookAuthorsRepository.updateBookAuthors(id, it)
        }

        // 更新後の書籍データを取得して返す (最新の状態をクライアントに返すため)
        return bookRepository.findById(id) ?: throw IllegalStateException("更新後の書籍データの取得に失敗しました。")
    }
}