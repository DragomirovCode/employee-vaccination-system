package com.example.app.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AppWebMvcConfiguration(
    private val appSecurityInterceptor: AppSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(appSecurityInterceptor).addPathPatterns("/hello")
    }
}
