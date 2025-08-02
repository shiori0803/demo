package com.example.demo.controller
import com.example.demo.dto.BookDto
import com.example.demo.repository.BookRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
@RestController
@RequestMapping("/books")
class BookController(private val bookRepository: BookRepository) {
    @GetMapping
    fun getAllBooks(): List<BookDto> {
        return bookRepository.getAllBooks()
    }
}
