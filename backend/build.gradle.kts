import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.ktlint) apply false
    id("java")
}

allprojects {
    group = "com.example"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        // ktlint только для Kotlin-модулей
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        // Java toolchain
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        // Kotlin compiler options
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            compilerOptions.freeCompilerArgs.addAll("-Xjsr305=strict")
        }

        // ktlint config
        extensions.configure<KtlintExtension> {
            version.set(libs.versions.ktlint.get())
            android.set(false)
            outputToConsole.set(true)
            ignoreFailures.set(false)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
