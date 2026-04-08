package com.example.auth.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

class LegacyAwarePasswordEncoder(
    private val delegate: PasswordEncoder = BCryptPasswordEncoder(),
) : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String =
        delegate.encode(rawPassword ?: "")
            ?: throw IllegalStateException("BCrypt encoder returned null hash")

    override fun matches(
        rawPassword: CharSequence?,
        encodedPassword: String?,
    ): Boolean {
        if (encodedPassword.isNullOrBlank()) {
            return false
        }

        return if (encodedPassword.startsWith("$2")) {
            delegate.matches(rawPassword ?: "", encodedPassword)
        } else {
            encodedPassword.contentEquals(rawPassword ?: "")
        }
    }

    override fun upgradeEncoding(encodedPassword: String?): Boolean =
        encodedPassword.isNullOrBlank() || !encodedPassword.startsWith("$2") || delegate.upgradeEncoding(encodedPassword)
}
