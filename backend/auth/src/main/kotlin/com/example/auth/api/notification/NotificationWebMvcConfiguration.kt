package com.example.auth.api.notification

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class NotificationWebMvcConfiguration(
    private val notificationSecurityInterceptor: NotificationSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(notificationSecurityInterceptor).addPathPatterns("/notifications/**")
    }
}
