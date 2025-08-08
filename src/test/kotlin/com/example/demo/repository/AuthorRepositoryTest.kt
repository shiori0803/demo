package com.example.demo.repository

import com.example.demo.BaseIntegrationTest
import com.example.demo.dto.AuthorDto
import com.example.demo.dto.BookDto
import db.Tables.BOOKS
import db.Tables.BOOK_AUTHORS
import db.tables.Authors.AUTHORS
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.util.stream.Stream

class AuthorRepositoryTest
    @Autowired
    constructor(
        private val authorRepository: AuthorRepository,
        private val ctx: DSLContext,
    ) : BaseIntegrationTest() {
        // 各テスト実行前にデータをクリーンアップする
        @BeforeEach
        fun beforeEach() {
            // Jooqを使ってAUTHORSテーブルの全レコードを削除
            ctx
                .truncate(AUTHORS)
                .restartIdentity()
                .cascade()
                .execute()
        }

        // ----------------- テストデータとヘルパー関数 -----------------

        companion object {
            // @MethodSourceは静的メソッドを探す。Kotlinだと"companion object"内に記載して@JvmStaticが必要
            @JvmStatic
            fun updateTestData(): Stream<Arguments> {
                val initialAuthor =
                    AuthorDto(
                        id = 999L,
                        name = "Hanako Ann Suzuki",
                        birthDate = LocalDate.of(1995, 8, 25),
                    )

                return Stream.of(
                    Arguments.of(
                        "全項目を更新",
                        initialAuthor,
                        initialAuthor.copy(
                            name = "花子 アン 鈴木",
                            birthDate = LocalDate.of(1995, 8, 26),
                        ),
                    ),
                    Arguments.of("nameのみを更新", initialAuthor, initialAuthor.copy(name = "太郎")),
                    Arguments.of(
                        "birthDateのみを更新",
                        initialAuthor,
                        initialAuthor.copy(birthDate = LocalDate.now().minusDays(1)),
                    ),
                )
            }

            /**
             * 正常に登録ができるテストデータ
             */
            @JvmStatic
            fun correctAuthorsData(): List<AuthorDto> =
                listOf(
                    AuthorDto(
                        id = 1L, // このIDはDBで無視されるか、自動生成されるIDに置き換えられる
                        name = "Hanako Ann Suzuki",
                        birthDate = LocalDate.now().minusDays(1),
                    ),
                    AuthorDto(
                        id = 2L,
                        name = "Taro Yamada", // 異なる名前
                        birthDate = LocalDate.now().minusDays(1),
                    ),
                    AuthorDto(
                        id = 3L,
                        name = "Hanako Ann Suzuki",
                        birthDate = LocalDate.now().minusDays(2), // 異なる生年月日
                    ),
                )
        }

        /**
         * テスト用の書籍データをDBに登録するヘルパーファンクション
         */
        private fun insertBooks(
            books: List<BookDto>,
            authorId: Long,
        ): List<BookDto> =
            books.map { bookDto ->
                // booksテーブルに登録し、IDを取得
                val insertedBookId =
                    ctx
                        .insertInto(BOOKS)
                        .set(BOOKS.TITLE, bookDto.title)
                        .set(BOOKS.PRICE, bookDto.price)
                        .set(BOOKS.PUBLICATION_STATUS, bookDto.publicationStatus)
                        .returning(BOOKS.ID)
                        .fetchOne()
                        ?.id
                assertThat(insertedBookId).isNotNull

                // book_authorsテーブルに関連を登録
                ctx
                    .insertInto(BOOK_AUTHORS)
                    .set(BOOK_AUTHORS.BOOK_ID, insertedBookId!!)
                    .set(BOOK_AUTHORS.AUTHOR_ID, authorId)
                    .execute()

                bookDto.copy(id = insertedBookId)
            }

        /**
         * テスト用のデータをDBに登録し、自動生成されたIDを反映したDTOを返すヘルパー関数
         */
        private fun insertAndGetAuthor(authorDto: AuthorDto): AuthorDto {
            val insertedId = authorRepository.insertAuthor(authorDto)?.id
            assertThat(insertedId).isNotNull()
            return authorDto.copy(id = insertedId)
        }

        /**
         * データベースのレコード数を取得するヘルパー関数
         */
        private fun getAuthorCountInDb(): Long {
            // fetchOne() が null を返す可能性があるため、null の場合は 0L を返すようにする
            return ctx.selectCount().from(AUTHORS).fetchOne(0, Long::class.java) ?: 0L
        }

        // --- findById ファンクションのテスト ---
        @Test
        fun `findById 正常にデータが登録できることを確認`() {
            // Given
            // テスト対象のファンクションを利用してデータ登録
            val authorDto = AuthorDto(id = null, name = "Test Author", birthDate = LocalDate.of(1990, 1, 1))
            val insertedAuthorDto = authorRepository.insertAuthor(authorDto)
            assertThat(insertedAuthorDto).isNotNull()

            // When
            // 登録したデータを取得
            val foundAuthor = authorRepository.findById(insertedAuthorDto!!.id!!)

            // Then
            // 登録したデータと取得したデータが一致することを確認
            assertThat(foundAuthor).isNotNull()
            assertThat(foundAuthor).isEqualTo(insertedAuthorDto)
        }

        @Test
        fun `findById 存在しない著者を取得した際に返却値がnullとなることを確認`() {
            // Given
            val nonExistentId = 999L

            // When
            val foundAuthor = authorRepository.findById(nonExistentId)

            // Then
            assertThat(foundAuthor).isNull()
        }

        // --- findBooksByAuthorId ファンクションのテスト ---

        @Test
        fun `findBooksByAuthorId 正常に書籍データが取得できることを確認`() {
            // Given
            // テスト用の著者と書籍をDBに登録
            val author = insertAndGetAuthor(AuthorDto(null, "Test Author", LocalDate.of(1980, 1, 1)))
            val booksToInsert =
                listOf(
                    BookDto(null, "Test Book 1", 1000, 1),
                    BookDto(null, "Test Book 2", 2000, 0),
                )
            val insertedBooks = insertBooks(booksToInsert, author.id!!)

            // When
            // テスト対象のファンクションを呼び出し
            val foundBooks = authorRepository.findBooksByAuthorId(author.id!!)

            // Then
            // 取得した書籍リストが期待値と一致することを確認
            assertThat(foundBooks).hasSize(2)
            assertThat(foundBooks).containsExactlyInAnyOrderElementsOf(insertedBooks)
        }

        @Test
        fun `findBooksByAuthorId 著者に書籍が紐づいていない場合は空のリストを返却することを確認`() {
            // Given
            // 書籍のない著者だけを登録
            val author = insertAndGetAuthor(AuthorDto(null, "Test Author", LocalDate.of(1980, 1, 1)))

            // When
            val foundBooks = authorRepository.findBooksByAuthorId(author.id!!)

            // Then
            // 空のリストが返却されることを確認
            assertThat(foundBooks).isEmpty()
        }

        @Test
        fun `findBooksByAuthorId 存在しない著者を指定した際に空のリストを返却することを確認`() {
            // Given
            val nonExistentAuthorId = 999L

            // When
            val foundBooks = authorRepository.findBooksByAuthorId(nonExistentAuthorId)

            // Then
            // 空のリストが返却されることを確認
            assertThat(foundBooks).isEmpty()
        }

        // --- insertAuthor ファンクションのテスト ---
        @ParameterizedTest
        @MethodSource("correctAuthorsData")
        fun `insertAuthor - 正常に登録が出来ることの確認`(authorDto: AuthorDto) {
            // テスト対象メソッドの実行
            val insertedAuthorDto = authorRepository.insertAuthor(authorDto.copy(id = null))
            assertThat(insertedAuthorDto).isNotNull()

            // 挿入したデータを取得し、元データと比較
            val insertedAuthor = authorRepository.findById(insertedAuthorDto!!.id!!)
            assertThat(insertedAuthor).isEqualTo(insertedAuthorDto)
        }

        @Test
        fun `insertAuthor 生年月日が現在の日付よりも未来の場合は登録されないことを確認`() {
            // 現在日の生年月日を持つテストデータを作成
            val futureAuthor =
                AuthorDto(
                    id = null,
                    name = "未来 著者",
                    birthDate = LocalDate.now(),
                )

            // insertAuthorメソッドの実行がDataIntegrityViolationExceptionをスローすることを検証
            assertThrows<DataIntegrityViolationException> {
                authorRepository.insertAuthor(futureAuthor)
            }

            // 例外発生後、データベースにレコードが登録されていないことを確認
            assertThat(getAuthorCountInDb()).isEqualTo(0L)
        }

        @Test
        fun `insertAuthor 重複する著者データが登録できないことを確認`() {
            // Given
            val commonName = "重複太郎"
            val commonBirthDate = LocalDate.of(1990, 1, 1)

            val originalAuthor =
                AuthorDto(
                    id = null,
                    name = commonName,
                    birthDate = commonBirthDate,
                )

            // 初回登録
            val savedAuthor = authorRepository.insertAuthor(originalAuthor)
            assertThat(savedAuthor).isNotNull

            // 2回目の登録
            val duplicateAuthor =
                AuthorDto(
                    id = 9999L, // ダミーID
                    name = commonName,
                    birthDate = commonBirthDate,
                )

            // DataIntegrityViolationException がスローされることを検証
            assertThrows<DataIntegrityViolationException> {
                authorRepository.insertAuthor(duplicateAuthor)
            }

            // データベースに登録されているレコードが最初の1件のみであることを確認
            assertThat(getAuthorCountInDb()).isEqualTo(1L)

            // データベースに登録されているデータが元のデータと一致することを確認
            val retrievedAuthor =
                ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(savedAuthor!!.id)).fetchOneInto(AuthorDto::class.java)
            assertThat(retrievedAuthor).isEqualTo(originalAuthor.copy(id = savedAuthor.id))
        }

        // --- updateAuthor ファンクションのテスト ---
        @Test
        fun `updateAuthor 更新可能な各カラムを一括で更新できるか確認`() {
            // Given
            val initialAuthor =
                AuthorDto(
                    id = null,
                    name = "Initial Name",
                    birthDate = LocalDate.of(2000, 1, 1),
                )
            // 更新前データ登録
            val savedAuthor = insertAndGetAuthor(initialAuthor)

            val updatedName = "Updated Full Name"
            val updatedBirthDate = LocalDate.of(1990, 12, 31)

            val updatesMap =
                mutableMapOf<String, Any?>(
                    "name" to updatedName,
                    "birthDate" to updatedBirthDate,
                )

            // テスト対象メソッド実行
            val updatedCount = authorRepository.updateAuthor(savedAuthor.id!!, updatesMap)

            // 1件のレコードが更新されたことを確認
            assertThat(updatedCount).isEqualTo(1)

            val retrievedAuthor =
                ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(savedAuthor.id)).fetchOneInto(AuthorDto::class.java)

            // 更新後のデータが期待通りであることを検証
            assertThat(retrievedAuthor?.name).isEqualTo(updatedName)
            assertThat(retrievedAuthor?.birthDate).isEqualTo(updatedBirthDate)
            assertThat(retrievedAuthor?.id).isEqualTo(savedAuthor.id)
        }

        @ParameterizedTest
        @MethodSource("updateTestData")
        fun `updateAuthor 更新可能な各カラムを一つずつ正しく更新できるか確認`(
            testName: String,
            initialData: AuthorDto,
            updatedData: AuthorDto,
        ) {
            // テストデータの作成、データ登録後にIDを返却
            val authorInDb = insertAndGetAuthor(initialData.copy(id = null))

            // 更新するフィールドと値のマップを構築
            val updatesMap = mutableMapOf<String, Any?>()

            // updatedData DTOから変更されたフィールドのみをマップに追加
            if (updatedData.name != initialData.name) {
                updatesMap["name"] = updatedData.name
            }
            if (updatedData.birthDate != initialData.birthDate) {
                updatesMap["birthDate"] = updatedData.birthDate
            }

            // テストメソッドの実行
            val updatedCount = authorRepository.updateAuthor(authorInDb.id!!, updatesMap)

            // 更新件数が1件のみであることの確認
            assertThat(updatedCount).isEqualTo(1)

            // 変更後のDBのデータが更新値と一致していることの確認
            val retrievedAuthor =
                ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorInDb.id)).fetchOneInto(AuthorDto::class.java)

            // DBから取得したretrievedAuthorのIDをupdatedDataにコピーして比較する
            assertThat(retrievedAuthor).isEqualTo(updatedData.copy(id = authorInDb.id))
        }

        @Test
        fun `updateAuthor 重複する著者データに登録できないことを確認`() {
            // Given
            val author1Name = "既存 著者A"
            val author1BirthDate = LocalDate.of(1980, 1, 1)
            val author2Name = "既存 著者B"
            val author2BirthDate = LocalDate.of(1990, 2, 2)

            // 最初の著者Aを登録
            val authorA = AuthorDto(id = null, name = author1Name, birthDate = author1BirthDate)
            insertAndGetAuthor(authorA)

            // 別の著者Bを登録
            val authorB = AuthorDto(id = null, name = author2Name, birthDate = author2BirthDate)
            val savedAuthorB = insertAndGetAuthor(authorB)

            // 著者Bの情報を、著者Aと同じ名前と生年月日に更新しようとする
            val updatesMapForB =
                mutableMapOf<String, Any?>(
                    "name" to author1Name,
                    "birthDate" to author1BirthDate,
                )

            // 更新操作がDataIntegrityViolationExceptionをスローすることを確認
            assertThrows<DataIntegrityViolationException> {
                authorRepository.updateAuthor(savedAuthorB.id!!, updatesMapForB)
            }

            // データベースのレコード数が変わっていないことを確認
            val countInDb = ctx.selectCount().from(AUTHORS).fetchOne(0, Long::class.java)
            assertThat(countInDb).isEqualTo(2L) // 2件のレコードがそのまま存在することを確認

            // 著者Bのデータが更新されていないことを確認
            val retrievedAuthorB =
                ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(savedAuthorB.id)).fetchOneInto(AuthorDto::class.java)
            assertThat(retrievedAuthorB).isEqualTo(savedAuthorB) // 著者Bのデータが変更されていないことを確認
        }

        // --- existsAllByIds ファンクションのテスト ---
        @Test
        fun `existsAllByIds 正常に指定したすべてのIDの件数を取得できること`() {
            // Given
            val authorIds = listOf(1L, 2L, 3L)
            authorIds.forEach { authorId ->
                ctx
                    .insertInto(AUTHORS)
                    .set(AUTHORS.ID, authorId)
                    .set(AUTHORS.NAME, "Test Name $authorId")
                    .set(AUTHORS.BIRTH_DATE, LocalDate.now().minusYears(30))
                    .execute()
            }

            // When
            val count = authorRepository.existsAllByIds(authorIds)

            // Then
            assertThat(count).isEqualTo(authorIds.size.toLong())
        }

        @Test
        fun `existsAllByIds 指定したIDの一部のみが存在する場合、存在するIDの件数を取得できること`() {
            // Given
            val existingId = 1L
            val nonExistentIds = listOf(999L, 1000L)
            val authorIds = listOf(existingId) + nonExistentIds

            ctx
                .insertInto(AUTHORS)
                .set(AUTHORS.ID, existingId)
                .set(AUTHORS.NAME, "Test Name")
                .set(AUTHORS.BIRTH_DATE, LocalDate.now().minusYears(30))
                .execute()

            // When
            val count = authorRepository.existsAllByIds(authorIds)

            // Then
            assertThat(count).isEqualTo(1L)
        }

        @Test
        fun `existsAllByIds 指定したIDが1件も存在しない場合、0を返却すること`() {
            // Given
            val nonExistentIds = listOf(999L, 1000L)
            val authorIds = nonExistentIds

            // When
            val count = authorRepository.existsAllByIds(authorIds)

            // Then
            assertThat(count).isEqualTo(0L)
        }

        @Test
        fun `existsAllByIds 空のリストを渡した場合、0を返却すること`() {
            // Given
            val authorIds = emptyList<Long>()

            // When
            val count = authorRepository.existsAllByIds(authorIds)

            // Then
            assertThat(count).isEqualTo(0L)
        }
    }
