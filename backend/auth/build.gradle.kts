import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    val bootBom = enforcedPlatform(libs.spring.boot.bom)

    implementation(bootBom)
    testImplementation(bootBom)
    testRuntimeOnly(bootBom)

    implementation(libs.spring.context)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
