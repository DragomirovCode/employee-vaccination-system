package com.example.auth.security

import com.example.auth.AppRole
import com.example.auth.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class AppUserDetails(
    val userId: UUID,
    private val email: String,
    private val passwordHash: String,
    private val active: Boolean,
    val roles: Set<AppRole>,
) : UserDetails {
    val authenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = userId,
            roles = roles,
        )

    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isEnabled(): Boolean = active
}
