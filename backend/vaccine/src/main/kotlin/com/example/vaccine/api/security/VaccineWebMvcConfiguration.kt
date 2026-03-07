package com.example.vaccine.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class VaccineWebMvcConfiguration(
    private val vaccineSecurityInterceptor: VaccineSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(vaccineSecurityInterceptor).addPathPatterns("/vaccines/**", "/diseases/**")
    }
}
