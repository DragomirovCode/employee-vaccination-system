package com.example.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example"])
class AppApplication

fun main(args: Array<String>) {
    runApplication<AppApplication>(*args)
}
