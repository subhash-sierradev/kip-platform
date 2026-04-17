plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    checkstyle
    jacoco
}

version = "0.0.1-rc.8"
description = "Integration Management Service - REST API (port 8085)"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
        mavenBom("org.junit:junit-bom:${libs.versions.junit.get()}")
    }
}

dependencies {
    implementation(project(":integration-execution-contract"))

    // Spring Boot Starters
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.cloud.starter.openfeign) {
        exclude(group = "commons-fileupload", module = "commons-fileupload")
    }

    // Database
    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // Validation
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)

    // Jackson
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
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

    // Transformation & Utilities
    implementation(libs.jolt.core)
    implementation(libs.commons.validator)
    implementation(libs.commons.lang3)
    implementation(libs.spring.retry)

    // Jakarta JSON
    implementation(libs.jakarta.json.api)
    runtimeOnly(libs.parsson)

    // Testing
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.assertj", module = "assertj-core")
    }
    testImplementation(libs.assertj.core)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Integration Test dependencies
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
}

// ---- Spring Boot Fat JAR ----
springBoot {
    mainClass.set("com.integration.management.IntegrationManagementServiceApplication")
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
    description = "Runs integration tests against real infrastructure (PostgreSQL + RabbitMQ via Testcontainers)."
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
