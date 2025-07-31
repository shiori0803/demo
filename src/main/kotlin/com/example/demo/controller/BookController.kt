package com.example.demo.controller
import com.example.demo.dto.BookDto
import com.example.demo.service.BookService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
@RestController
@RequestMapping("/books")
class BookController(private val bookService: BookService) {
    @GetMapping
    fun getAllBooks(): List<BookDto> {
        return bookService.getAllBooks()
    }
}
