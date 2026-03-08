package com.example.auth.api.admin

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AuthAdminWebMvcConfiguration(
    private val authAdminSecurityInterceptor: AuthAdminSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(authAdminSecurityInterceptor)
            .addPathPatterns("/auth/users/**", "/auth/roles/**")
    }
}
