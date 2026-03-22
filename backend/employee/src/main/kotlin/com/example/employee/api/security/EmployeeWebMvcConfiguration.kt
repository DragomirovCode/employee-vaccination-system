package com.example.employee.api.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EmployeeWebMvcConfiguration(
    private val employeeSecurityInterceptor: EmployeeSecurityInterceptor,
) : WebMvcConfigurer {
    /**
     * Подключает интерсептор безопасности к эндпоинтам подразделений и сотрудников.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(employeeSecurityInterceptor).addPathPatterns("/departments/**", "/employees/**")
    }
}
