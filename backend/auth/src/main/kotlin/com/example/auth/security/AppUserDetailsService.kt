package com.example.auth.security

import com.example.auth.AppRole
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppUserDetailsService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
) : UserDetailsService {
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val normalizedEmail = username.trim()
        val user =
            userRepository.findByEmail(normalizedEmail)
                ?: throw UsernameNotFoundException("User not found")

        val userId = user.id ?: throw UsernameNotFoundException("User not found")
        val roles =
            userRoleRepository
                .findRoleCodesByUserId(userId)
                .mapNotNull(AppRole::fromCode)
                .toSet()

        return AppUserDetails(
            userId = userId,
            email = user.email,
            passwordHash = user.passwordHash,
            active = user.isActive,
            roles = roles,
        )
    }
}
