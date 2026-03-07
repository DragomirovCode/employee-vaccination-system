package com.example.employee.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EmployeeWebMvcConfiguration(
    private val employeeSecurityInterceptor: EmployeeSecurityInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(employeeSecurityInterceptor).addPathPatterns("/departments/**", "/employees/**")
    }
}
