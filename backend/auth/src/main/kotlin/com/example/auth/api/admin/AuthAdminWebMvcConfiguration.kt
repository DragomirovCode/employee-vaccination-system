package com.example.auth.api.admin

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AuthAdminWebMvcConfiguration(
    private val authAdminSecurityInterceptor: AuthAdminSecurityInterceptor,
) : WebMvcConfigurer {
    /**
     * Подключает интерсептор проверки административного доступа к auth-эндпоинтам.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(authAdminSecurityInterceptor)
            .addPathPatterns("/auth/users/**", "/auth/roles/**")
    }
}
