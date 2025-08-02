package com.example.demo.repository

import com.example.demo.BaseIntegrationTest
import com.example.demo.dto.AuthorDto
import db.tables.Authors.AUTHORS
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DefaultDSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
        @JvmStatic
        fun updateTestData(): Stream<Arguments> {
            val initialAuthor = AuthorDto(id = 999L, firstName = "Hanako", middleName = "Ann", lastName = "Suzuki", birthDate = LocalDate.of(1995, 8, 25))

            return Stream.of(
                Arguments.of(
                    "全項目を更新",
                    initialAuthor,
                    initialAuthor.copy(firstName = "花子", middleName = "アン", lastName = "鈴木", birthDate = LocalDate.of(1995, 8, 26))
                ),
                Arguments.of("firstNameのみを更新", initialAuthor, initialAuthor.copy(firstName = "太郎")),
                Arguments.of("middleNameをnullから値有りに更新", initialAuthor.copy(middleName = null), initialAuthor.copy(middleName = "アン")),
                Arguments.of("middleNameのみを更新", initialAuthor, initialAuthor.copy(middleName = "マリー")),
                Arguments.of("birthDateのみを更新", initialAuthor, initialAuthor.copy(birthDate = LocalDate.now().minusDays(1)))
            )
        }
    }

    /**
     * 正常に登録ができるテストデータ
     */
    private fun correctAuthorsData(): List<AuthorDto> {
        return listOf(
            AuthorDto(id = 1L, firstName = "Hanako", middleName = "Ann", lastName = "Suzuki", birthDate = LocalDate.now().minusDays(1)),
            AuthorDto(id = 2L, firstName = "taro", middleName = null, lastName = "yamada", birthDate = LocalDate.of(1980, 5, 10))
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

    // ----------------- テストメソッド -----------------
    /**
     * DB定義の条件に違反しない正しいデータの登録を確認する
     */
    @Test
    fun `insertAuthor - correct insert authors`() {
        // テストデータ
        val inputAuthors = correctAuthorsData()

        // テストメソッドの実行
        inputAuthors.forEach { authorRepository.insertAuthor(it) }

        // 挿入したデータをすべて取得
        val insertedAuthors =
            ctx.selectFrom(AUTHORS).where(AUTHORS.ID.`in`(inputAuthors.map { it.id })).fetchInto(AuthorDto::class.java)
                .sortedBy { it.id } // 順番を保証するためにIDでソート

        // 取得したデータと元データを比較
        assertThat(insertedAuthors).hasSize(inputAuthors.size) // リストのサイズが一致するか
            .containsExactlyElementsOf(inputAuthors) // リストの内容が完全に一致するか

        // 存在しないデータがないことを検証
        val thirdAuthor = ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(999L)).fetchOneInto(AuthorDto::class.java)
        assertThat(thirdAuthor).isNull()
    }

    /**
     * 生年月日が現在の日付よりも未来の場合は登録されないことを確認する
    */
    @Test
    fun `insertAuthor - Violation of birthDate condition`() {
        // 現在日の生年月日を持つテストデータを作成
        val futureAuthor = AuthorDto(
            id = 1L, firstName = "未来", middleName = null, lastName = "future", birthDate = LocalDate.now()
        )

        // insertAuthorメソッドの実行がDataIntegrityViolationExceptionをスローすることを検証
        assertThrows<DataIntegrityViolationException> {
            authorRepository.insertAuthor(futureAuthor)
        }

        // 例外発生後、データベースにレコードが登録されていないことを確認
        // トランザクションがロールバックされ、レコード数が0件のままであることを確認
        val countAfterFailure = ctx.selectCount().from(AUTHORS).fetchOne(0, Long::class.java)
        assertThat(countAfterFailure).isEqualTo(0L)
    }


    @ParameterizedTest(name = "updateAuthor - update field {0}")
    @MethodSource("updateTestData")
    fun `updateAuthor - Correctly updating the field {0}`(
        testName: String,//各テストケースの実行時に表示される名前を定義(ex:updateAuthor - update field name)
        initialData: AuthorDto,
        updatedData: AuthorDto
    ) {
        // テストデータの作成、データ登録後にIDを返却
        val authorInDb = insertAndGetAuthor(initialData)

        // 更新用DTOのIDを、挿入されたIDに上書き
        val updatedAuthorWithId = updatedData.copy(id = authorInDb.id)

        // テストメソッドの実行
        val updatedCount = authorRepository.updateAuthor(updatedAuthorWithId)

        // 更新件数が1件のみであることの確認
        assertThat(updatedCount).isEqualTo(1)
        // 変更後のDBのデータが更新値と一致していることの確認
        val retrievedAuthor = ctx.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorInDb.id)).fetchOneInto(AuthorDto::class.java)
        assertThat(retrievedAuthor).isEqualTo(updatedAuthorWithId)
    }
}
