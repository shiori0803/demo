package com.example.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 400 Bad Request を返すことを示す
class InvalidPublicationStatusChangeException(message: String) : RuntimeException(message)