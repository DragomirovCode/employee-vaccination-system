package com.example.employee

import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.example"])
@AutoConfigurationPackage(basePackages = ["com.example"])
@EnableJpaRepositories(basePackages = ["com.example"])
class EmployeeTestApplication
