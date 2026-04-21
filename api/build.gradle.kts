import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.gradle.versions)
}

// ---------------------------------------------------------------------------
// OWASP Dependency-Check — CVE scanning, fail at CVSS ≥ 7.0
// ---------------------------------------------------------------------------
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
    formats = listOf("HTML", "JSON")
    outputDirectory = layout.buildDirectory.dir("reports/dependency-check").get().asFile

    nvd {
        // Set NVD_API_KEY env var or nvd.api.key Gradle property to avoid rate-limiting.
        // See: https://nvd.nist.gov/developers/request-an-api-key
        val apiKey = providers.gradleProperty("nvd.api.key")
            .orElse(providers.environmentVariable("NVD_API_KEY"))
            .orNull
        if (!apiKey.isNullOrBlank()) {
            this.apiKey = apiKey
            delay = 1000
        } else {
            delay = 6000   // ms between NVD API calls (free-tier safe)
        }
    }

    analyzers {
        experimentalEnabled = false
        assemblyEnabled       = false   // .NET — not applicable
        nugetconfEnabled      = false   // NuGet — not applicable
        golangDepEnabled      = false
        golangModEnabled      = false
        dartEnabled           = false
        swiftEnabled          = false
        cocoapodsEnabled      = false
        rubygemsEnabled       = false
        pyDistributionEnabled = false
        pyPackageEnabled      = false
        cmakeEnabled          = false
        autoconfEnabled       = false
        composerEnabled       = false
    }
}

// ---------------------------------------------------------------------------
// Gradle Versions Plugin — informational outdated-dependency report
// ---------------------------------------------------------------------------
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = Regex("^[0-9,.v-]+(-r)?$")
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
    outputFormatter = "html,json"
    outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath
    reportfileName = "dependency-updates"
}

// ---------------------------------------------------------------------------
// Shared project configuration
// ---------------------------------------------------------------------------
allprojects {
    group = "com.integration"
    repositories {
        mavenCentral()
    }
    // Force-upgrade transitive dependencies with known CVEs.
    configurations.all {
        resolutionStrategy {
            // CVE-2025-48924 — commons-lang3 < 3.17.0 (transitive via jolt-core)
            force("org.apache.commons:commons-lang3:3.18.0")
            // CVE-2025-67030 — plexus-utils (transitive via build plugins) — suppressed in owasp-suppressions.xml
            force("org.codehaus.plexus:plexus-utils:4.0.2")
        }
    }
}

subprojects {
    apply(plugin = "java")

    // Override Spring Boot BOM-managed versions for security patches.
    // ext["x.version"] is the correct mechanism — resolutionStrategy.force
    // does not override Spring Boot dependency management plugin.
    ext["tomcat.version"] = "11.0.21"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:deprecation",   // flag uses of deprecated APIs
                "-Xlint:unchecked",     // flag unchecked generic casts
            )
        )
    }
}
