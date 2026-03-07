import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    val bootBom = enforcedPlatform(libs.spring.boot.bom)

    implementation(bootBom)
    testImplementation(bootBom)
    testRuntimeOnly(bootBom)

    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.kotlin.reflect)
    implementation(libs.swagger.annotations)
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("com.github.librepdf:openpdf:1.3.39")

    implementation(project(":employee"))
    implementation(project(":vaccine"))
    implementation(project(":vaccination"))
    implementation(project(":auth"))

    testImplementation(project(":auth"))
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.h2)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
