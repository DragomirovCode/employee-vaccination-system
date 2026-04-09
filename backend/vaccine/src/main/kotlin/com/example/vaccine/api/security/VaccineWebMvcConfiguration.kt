package com.example.vaccine.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class VaccineWebMvcConfiguration(
    private val vaccineSecurityInterceptor: VaccineSecurityInterceptor,
) : WebMvcConfigurer {
    /**
     * Подключает интерсептор безопасности к эндпоинтам справочников вакцин и заболеваний.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(vaccineSecurityInterceptor)
            .addPathPatterns(
                "/vaccines",
                "/vaccines/**",
                "/diseases",
                "/diseases/**",
            )
    }
}
