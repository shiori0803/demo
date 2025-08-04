package com.example.demo.repository

import com.example.demo.BaseIntegrationTest
import com.example.demo.dto.AuthorDto
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.stream.Stream

@SpringBootTest
@Testcontainers
class AuthorRepositoryTest @Autowired constructor(
    private val authorRepository: AuthorRepository, private val ctx: DSLContext
) : BaseIntegrationTest() {


    // 各テスト実行前にデータをクリーンアップする
    @BeforeEach
    fun beforeEach() {
        // Jooqを使ってAUTHORSテーブルの全レコードを削除
        ctx.truncate(AUTHORS).restartIdentity().cascade().execute()
    }

    // ----------------- テストデータとヘルパー関数 -----------------

    companion object {
        // @MethodSourceは静的メソッドを探す。Kotlinだと"companion object"内に記載して@JvmStaticが必要
        @JvmStatic
        fun updateTestData(): Stream<Arguments> {
            val initialAuthor = AuthorDto(
                id = 999L, name = "Hanako Ann Suzuki", birthDate = LocalDate.of(1995, 8, 25)
            )

            return Stream.of(
                Arguments.of(
                    "全項目を更新", initialAuthor, initialAuthor.copy(
                        name = "花子 アン 鈴木", birthDate = LocalDate.of(1995, 8, 26)
                    )
                ), Arguments.of("nameのみを更新", initialAuthor, initialAuthor.copy(name = "太郎")), Arguments.of(
                    "birthDateのみを更新", initialAuthor, initialAuthor.copy(birthDate = LocalDate.now().minusDays(1))
                )
            )
        }
    }

    /**
     * 正常に登録ができるテストデータ
     */
    private fun correctAuthorsData(): List<AuthorDto> {
        return listOf(
            AuthorDto(
                id = 1L, // このIDはDBで無視されるか、自動生成されるIDに置き換えられる
                name = "Hanako Ann Suzuki", birthDate = LocalDate.now().minusDays(1)
            ), AuthorDto(
                id = 2L, name = "Taro Yamada", // 異なる名前
                birthDate = LocalDate.now().minusDays(1)
            ), AuthorDto(
                id = 3L, name = "Hanako Ann Suzuki", birthDate = LocalDate.now().minusDays(2) // 異なる生年月日
            )
        )
    }

    /**
     * テスト用のデータをDBに登録し、自動生成されたIDを反映したDTOを返すヘルパー関数
     */
    private fun insertAndGetAuthor(authorDto: AuthorDto): AuthorDto {
        val insertedId = authorRepository.insertAuthor(authorDto)
        assertThat(insertedId).isNotNull
        return authorDto.copy(id = insertedId!!)
    }

    /**
     * データベースから全ての著者データを取得し、IDをnullにしてソートするヘルパー関数
     */
    private fun getAllAuthorsFromDbNormalized(): List<AuthorDto> {
        return ctx.selectFrom(AUTHORS).fetchInto(AuthorDto::class.java)
            .map { it.copy(id = null) } // IDをnullにして比較対象から除外
            .sortedBy { it.name + it.birthDate } // 比較のためソート
    }

    /**
     * データベースのレコード数を取得するヘルパー関数
     */
    private fun getAuthorCountInDb(): Long {
        // fetchOne() が null を返す可能性があるため、null の場合は 0L を返すようにする
        return ctx.selectCount().from(AUTHORS).fetchOne(0, Long::class.java) ?: 0L
    }

    // ----------------- テストメソッド -----------------
    /**
     * DB定義の条件に違反しない正しいデータの登録を確認する
     */
    @Test
    fun `insertAuthor - insert correct authors`() {
        // テストデータ
        val inputAuthors = correctAuthorsData()

        // テスト対象メソッドの実行
        inputAuthors.forEach { authorRepository.insertAuthor(it.copy(id = null)) }

        // 挿入したデータをすべて取得し、元データと比較
        val insertedAuthorsNormalized = getAllAuthorsFromDbNormalized()
        val expectedAuthorsNormalized = inputAuthors.map { it.copy(id = null) }.sortedBy { it.name + it.birthDate }

        assertThat(insertedAuthorsNormalized).hasSize(inputAuthors.size)
            .containsExactlyElementsOf(expectedAuthorsNormalized)

        // 存在しないデータがないことを検証
        val nonExistentAuthor = ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(999L)).fetchOneInto(AuthorDto::class.java)
        assertThat(nonExistentAuthor).isNull()
    }

    /**
     * 生年月日が現在の日付よりも未来の場合は登録されないことを確認する
     */
    @Test
    fun `insertAuthor - Violation of birthDate condition`() {
        // 現在日の生年月日を持つテストデータを作成
        val futureAuthor = AuthorDto(
            id = null, name = "未来 著者", birthDate = LocalDate.now()
        )

        // insertAuthorメソッドの実行がDataIntegrityViolationExceptionをスローすることを検証
        assertThrows<DataIntegrityViolationException> {
            authorRepository.insertAuthor(futureAuthor)
        }

        // 例外発生後、データベースにレコードが登録されていないことを確認
        assertThat(getAuthorCountInDb()).isEqualTo(0L)
    }

    /**
     * 名前と生年月日が完全に重複するデータが登録出来ないことを確認する。
     * nameカラムとbirth_dateカラムにUNIQUE制約が適用されていることを検証する。
     */
    @Test
    fun `insertAuthor - cannot save duplicate name and birthDate`() {
        // Given
        val commonName = "重複太郎"
        val commonBirthDate = LocalDate.of(1990, 1, 1)

        val originalAuthor = AuthorDto(
            id = null, name = commonName, birthDate = commonBirthDate
        )

        // 初回登録
        val savedAuthorId = authorRepository.insertAuthor(originalAuthor)
        assertThat(savedAuthorId).isNotNull

        // 2回目の登録
        val duplicateAuthor = AuthorDto(
            id = 9999L, // ダミーID
            name = commonName, birthDate = commonBirthDate
        )

        // DataIntegrityViolationException がスローされることを検証
        assertThrows<DataIntegrityViolationException> {
            authorRepository.insertAuthor(duplicateAuthor)
        }

        // データベースに登録されているレコードが最初の1件のみであることを確認
        assertThat(getAuthorCountInDb()).isEqualTo(1L)

        // データベースに登録されているデータが元のデータと一致することを確認
        val retrievedAuthor =
            ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(savedAuthorId)).fetchOneInto(AuthorDto::class.java)
        assertThat(retrievedAuthor).isEqualTo(originalAuthor.copy(id = savedAuthorId))
    }

    /**
     * 各更新可能なカラムを一括で更新できるか確認
     */
    @Test
    fun `updateAuthor - can update all updatable columns at once`() {
        // Given
        val initialAuthor = AuthorDto(
            id = null, name = "Initial Name", birthDate = LocalDate.of(2000, 1, 1)
        )
        // 更新前データ登録
        val savedAuthor = insertAndGetAuthor(initialAuthor)

        val updatedName = "Updated Full Name"
        val updatedBirthDate = LocalDate.of(1990, 12, 31)

        val updatesMap = mutableMapOf<String, Any?>(
            "name" to updatedName, "birthDate" to updatedBirthDate
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


    /**
     * 各更新可能なカラムを一つずつ正しく更新できるか確認
     */
    @ParameterizedTest(name = "updateAuthor - update field {0}")
    @MethodSource("updateTestData")
    fun `updateAuthor - Correctly updating the field {0}`(
        testName: String, initialData: AuthorDto, updatedData: AuthorDto
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

    /**
     * 名前と生年月日が一致するデータに更新できないことを確認
     * 既存のUNIQUE制約が更新操作でも正しく機能することを検証する
     */
    @Test
    fun `updateAuthor - cannot update to duplicate name and birthDate`() {
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
        val updatesMapForB = mutableMapOf<String, Any?>(
            "name" to author1Name, "birthDate" to author1BirthDate
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
}
