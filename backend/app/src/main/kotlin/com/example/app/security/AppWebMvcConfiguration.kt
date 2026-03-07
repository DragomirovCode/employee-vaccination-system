package com.example.app.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AppWebMvcConfiguration(
    private val appSecurityInterceptor: AppSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("X-Auth-Token", "Content-Type", "Authorization")
            .exposedHeaders("X-Auth-Token")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(appSecurityInterceptor).addPathPatterns("/hello")
    }
}
