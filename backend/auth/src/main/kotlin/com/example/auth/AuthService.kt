package com.example.auth

class AuthService {
    /**
     * Демонстрационная реализация.
     * В реальном проекте здесь обычно будут JWT/сессии/интеграция с IdP и т.д.
     */
    fun isTokenValid(token: String?): Boolean = token != null && token == "dev-token"
}
