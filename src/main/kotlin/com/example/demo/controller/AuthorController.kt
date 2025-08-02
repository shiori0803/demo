package com.example.demo.controller

import com.example.demo.dto.AuthorDto
import com.example.demo.repository.AuthorRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/authors")
class AuthorController(private val authorRepository: AuthorRepository) {

    /**
     * 新しい著者データを登録するAPIエンドポイント。
     * HTTP POSTリクエストを受け取り、リクエストボディから著者情報を取得します。
     * @param authorDto リクエストボディからマッピングされるAuthorDtoオブジェクト
     * @return 登録結果を示すResponseEntity
     */
    @PostMapping // HTTP POSTリクエストを処理するメソッド
    fun createAuthor(@RequestBody authorDto: AuthorDto): ResponseEntity<String> {
        // バリデーションの例: 生年月日が未来ではないことを確認
        if (authorDto.birthDate.isAfter(LocalDate.now())) {
            return ResponseEntity
                .badRequest()
                .body("生年月日は現在の日付よりも過去である必要があります。")
        }

        try {
            val authorId = authorRepository.insertAuthor(authorDto)
            return if (authorId != null) {
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("著者データが正常に登録されました。著者ID: $authorId")
            } else {
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("著者データの登録に失敗しました。")
            }
        } catch (e: Exception) {
            // データベース制約違反などのエラーハンドリング
            // UNIQUE制約違反の場合
            if (e.message?.contains("unique_author_details", ignoreCase = true) == true) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict ステータスを返す
                    .body("この著者データは既に存在します。")
            }
            // その他のデータベースエラー
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("著者データの登録中にエラーが発生しました: ${e.message}")
        }
    }

}