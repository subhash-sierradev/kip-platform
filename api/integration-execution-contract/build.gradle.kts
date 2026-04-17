plugins {
    `java-library`
    checkstyle
}

version = "0.0.2"
description = "Pure contract module - DTOs, enums, and models"

dependencies {
    api(libs.jakarta.validation.api)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
    api(libs.hibernate.validator)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("checkstyle.xml")
    configProperties = mapOf("suppressions_file" to file("checkstyle-suppressions.xml").absolutePath)
}

tasks.withType<Checkstyle> {
    enabled = !project.hasProperty("checkstyle.skip")
}
