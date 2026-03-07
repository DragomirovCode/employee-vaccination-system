package com.example.app

import com.example.auth.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController(
    private val authService: AuthService,
) {
    @GetMapping("/hello")
    fun hello(
        @RequestHeader("X-Auth-Token", required = false) token: String?,
    ): ResponseEntity<String> {
        authService.requireAuthenticated(token)
        return ResponseEntity.ok("Hello from app module")
    }
}
