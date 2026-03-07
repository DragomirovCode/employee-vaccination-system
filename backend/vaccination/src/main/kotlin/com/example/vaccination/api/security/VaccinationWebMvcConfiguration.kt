package com.example.vaccination.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class VaccinationWebMvcConfiguration(
    private val vaccinationWriteSecurityInterceptor: VaccinationWriteSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(vaccinationWriteSecurityInterceptor).addPathPatterns("/vaccinations/**", "/documents/**")
    }
}
