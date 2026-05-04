plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    checkstyle
    jacoco
}

version = "0.0.1-rc.1"
description = "Translation Service - Local AI Translation via Ollama (port 8083)"

dependencyManagement {
    imports {
        mavenBom("org.junit:junit-bom:${libs.versions.junit.get()}")
    }
}

dependencies {
    // Spring Boot Starters
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)

    // Validation
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)

    // Jackson (JSON serialization / UTF-8 content)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Caffeine cache
    implementation(libs.caffeine)

    // Apache HTTP Client (used by RestTemplate)
    implementation(libs.httpclient5)
    implementation(libs.httpcore5)

    // Utilities
    implementation(libs.commons.lang3)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
}

// ---- Spring Boot Fat JAR ----
springBoot {
    mainClass.set("com.integration.translation.TranslationServiceApplication")
    buildInfo()
}

tasks.bootJar {
    archiveClassifier.set("")
}

// ---- Checkstyle ----
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("checkstyle.xml")
    configProperties = mapOf("suppressions_file" to file("checkstyle-suppressions.xml").absolutePath)
    isShowViolations = true
}

tasks.withType<Checkstyle> {
    enabled = !project.hasProperty("checkstyle.skip")
    maxWarnings = 0
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}

// ---- JaCoCo (90% minimum — consistent with integration-execution-service and integration-management-service) ----
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // Exclude the Spring Boot entry-point — main() cannot be unit-tested and
    // it is excluded from coverage analysis in the sibling service modules.
    classDirectories.setFrom(
        files(classDirectories.files.map { dir ->
            fileTree(dir) {
                exclude("**/TranslationServiceApplication.class")
            }
        })
    )
}

val jacocoMinCoverage = "0.90".toBigDecimal()

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            element = "BUNDLE"
            limit { counter = "LINE"; value = "COVEREDRATIO"; minimum = jacocoMinCoverage }
            limit { counter = "BRANCH"; value = "COVEREDRATIO"; minimum = jacocoMinCoverage }
        }
    }
}

// ---- Tests ----
tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xmx512m", "-XX:+EnableDynamicAgentLoading")
    modularity.inferModulePath.set(false)
}

// Wire verify -> coverage check
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

