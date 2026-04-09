package com.example.vaccination.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class VaccinationWebMvcConfiguration(
    private val vaccinationWriteSecurityInterceptor: VaccinationWriteSecurityInterceptor,
) : WebMvcConfigurer {
    /**
     * Подключает интерсептор безопасности к эндпоинтам вакцинаций и документов.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(vaccinationWriteSecurityInterceptor)
            .addPathPatterns(
                "/vaccinations",
                "/vaccinations/**",
                "/documents",
                "/documents/**",
                "/employees/**/vaccinations",
            )
    }
}
