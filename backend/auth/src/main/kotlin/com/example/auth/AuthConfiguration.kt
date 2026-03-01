package com.example.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthConfiguration {
    @Bean
    fun authService(): AuthService = AuthService()
}
