plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/src/**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_max-line-length" to "disabled",
                "max_line_length" to "off",
            ),
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_max-line-length" to "disabled",
                "max_line_length" to "off",
            ),
        )
    }

    format("xml") {
        target("**/src/**/*.xml")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}

subprojects {
    plugins.withId("com.android.application") {
        tasks.named("preBuild").configure {
            dependsOn(":spotlessApply")
        }
    }
}

tasks.named<Delete>("clean") {
    delete(layout.buildDirectory)

    project.allprojects.forEach { project ->
        delete(project.layout.buildDirectory)
    }
}
