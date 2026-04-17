plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    checkstyle
    jacoco
}

version = "0.0.2"
description = "Integration Execution Service - Processing Engine (port 8081)"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
        mavenBom("org.junit:junit-bom:${libs.versions.junit.get()}")
        mavenBom("com.azure:azure-sdk-bom:${libs.versions.azureSdkBom.get()}")
    }
}

dependencies {
    implementation(project(":integration-execution-contract"))

    // Spring Boot Starters
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    // Validation
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)

    // Jackson
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)

    // Spring Data
    implementation(libs.spring.data.commons)

    // MapStruct + Lombok
    implementation(libs.mapstruct)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)

    // Caching & HTTP
    implementation(libs.caffeine)
    implementation(libs.httpclient5)
    implementation(libs.httpcore5)

    // FreeMarker for template rendering
    implementation(libs.freemarker)

    // Transformation & Utilities
    implementation(libs.jolt.core)
    implementation(libs.commons.validator)
    implementation(libs.commons.lang3)
    implementation(libs.spring.retry)

    // Resilience4j
    implementation(libs.resilience4j.bulkhead)
    implementation(libs.resilience4j.ratelimiter)

    // Azure Key Vault
    implementation(libs.azure.keyvault.secrets)
    implementation(libs.azure.identity)

    // Jakarta JSON
    implementation(libs.jakarta.json.api)
    runtimeOnly(libs.parsson)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.assertj.core)

    // Testcontainers
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
}

// ---- Spring Boot Fat JAR ----
springBoot {
    mainClass.set("com.integration.execution.IntegrationExecutionServiceApplication")
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

// ---- JaCoCo (80% minimum) ----
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoMinCoverage = providers.gradleProperty("jacocoMinimumCoverage").get().toBigDecimal()

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
    jvmArgs("-Xmx1024m", "-XX:+EnableDynamicAgentLoading")
    modularity.inferModulePath.set(false)
    exclude("**/*IT.class")
}

// ---- Integration Tests (run separately — requires Docker for Testcontainers) ----
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests against real infrastructure (RabbitMQ via Testcontainers)."
    group = "verification"
    useJUnitPlatform()
    jvmArgs("-Xmx1024m", "-XX:+EnableDynamicAgentLoading")
    modularity.inferModulePath.set(false)
    include("**/*IT.class")
    shouldRunAfter(tasks.test)
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    // Pass through DOCKER_HOST if explicitly set (CI / Linux socket override)
    val dockerHost = System.getenv("DOCKER_HOST") ?: ""
    if (dockerHost.isNotBlank()) {
        environment("DOCKER_HOST", dockerHost)
    }
}

// Wire verify -> coverage check
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
