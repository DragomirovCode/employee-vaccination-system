package com.example.reporting.access

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ReportingWebMvcConfiguration(
    private val reportingSecurityInterceptor: ReportingSecurityInterceptor,
) : WebMvcConfigurer {
    /**
     * Подключает интерсептор безопасности ко всем reporting-эндпоинтам.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(reportingSecurityInterceptor).addPathPatterns("/reports/**")
    }
}
