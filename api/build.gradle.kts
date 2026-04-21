import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.owasp.dependency.check)      // root: enables dependencyCheckAggregate
    alias(libs.plugins.gradle.versions) apply false  // applied per-subproject; root only has aggregate shim
}

// ---------------------------------------------------------------------------
// OWASP Dependency-Check — CVE scanning, fail at CVSS ≥ 7.0
// Shared configuration block — applies to root (aggregate) and all subprojects.
// ---------------------------------------------------------------------------
fun configureOwaspDependencyCheck(project: Project) {
    project.extensions.configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
        failBuildOnCVSS = 7.0f
        suppressionFile = rootProject.file("owasp-suppressions.xml").absolutePath
        formats = listOf("HTML", "JSON")
        outputDirectory = project.layout.buildDirectory.dir("reports/dependency-check").get().asFile

        nvd {
            val apiKey = project.providers.gradleProperty("nvd.api.key")
                .orElse(project.providers.environmentVariable("NVD_API_KEY"))
                .orNull
            if (!apiKey.isNullOrBlank()) {
                this.apiKey = apiKey
                delay = 1000
            } else {
                delay = 6000
            }
        }

        analyzers {
            experimentalEnabled   = false
            assemblyEnabled       = false
            nugetconfEnabled      = false
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
}

configureOwaspDependencyCheck(rootProject)

// ---------------------------------------------------------------------------
// Gradle Versions Plugin — informational outdated-dependency report
// Applied per-subproject so the task resolves IMS/IES actual declared deps.
// Commands:
//   ./gradlew dependencyUpdates                                — all modules (aggregate shim)
//   ./gradlew :integration-management-service:dependencyUpdates — IMS only
//   ./gradlew :integration-execution-service:dependencyUpdates  — IES only
// ---------------------------------------------------------------------------
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = Regex("^[0-9,.v-]+(-r)?$")
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

// Root aggregate shim — runs dependencyUpdates on every subproject in one command.
tasks.register("dependencyUpdates") {
    description = "Runs dependencyUpdates on all subprojects. Reports written to each module's build/reports/dependencyUpdates/."
    group = "help"
    dependsOn(subprojects.map { ":${it.name}:dependencyUpdates" })
}

// ---------------------------------------------------------------------------
// Shared project configuration
// ---------------------------------------------------------------------------

// Capture CVE-forced versions at root scope — libs accessor not available inside allprojects subproject closures.
val commonsLang3Version = libs.versions.commonsLang3.get()
val plexusUtilsVersion = libs.versions.plexusUtils.get()

allprojects {
    group = "com.integration"
    repositories {
        mavenCentral()
    }

    configurations.all {
        resolutionStrategy {
            // CVE-2025-48924 — commons-lang3 < 3.17.0 (transitive via jolt-core)
            force("org.apache.commons:commons-lang3:$commonsLang3Version")
            // CVE-2025-67030 — plexus-utils (transitive via build plugins) — suppressed in owasp-suppressions.xml
            force("org.codehaus.plexus:plexus-utils:$plexusUtilsVersion")
        }
    }
}

subprojects {
    apply(plugin = "java")

    // Gradle Versions plugin applied per-subproject so dependencyUpdates
    // resolves actual IMS/IES declared dependencies (not the empty root project).
    // Per-project outputDir avoids report file collisions between modules.
    apply(plugin = "com.github.ben-manes.versions")
    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
        outputFormatter = "html,json"
        outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath
        reportfileName = "dependency-updates"
    }

    // OWASP plugin per-subproject so developers can scan individual modules.
    // dependencyCheckAggregate at root is still preferred for CI (single report).
    apply(plugin = "org.owasp.dependencycheck")
    configureOwaspDependencyCheck(this)

    // Override Spring Boot BOM-managed Tomcat version for CVE-2026-34483/86/87/500
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
                "-Xlint:deprecation",
                "-Xlint:unchecked",
            )
        )
    }
}