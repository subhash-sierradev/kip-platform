rootProject.name = "kip-api"

// Build performance settings (moved from gradle.properties)
gradle.startParameter.isParallelProjectExecutionEnabled = true
gradle.startParameter.isBuildCacheEnabled = true

include(
    "integration-execution-contract",
    "integration-management-service",
    "integration-execution-service",
    "translation-service"
)
