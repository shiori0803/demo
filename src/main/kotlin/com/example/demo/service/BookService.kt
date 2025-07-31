package com.example.demo.service
import com.example.demo.dto.BookDto
import db.Tables.BOOKS
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class BookService(private val dslContext: DSLContext) {
    /**
     * 著作全件検索
     */
    fun getAllBooks(): List<BookDto> {
        return dslContext.select(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE)
            .from(BOOKS)
            .fetchInto(BookDto::class.java)
    }
}
