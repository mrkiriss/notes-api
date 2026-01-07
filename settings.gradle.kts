rootProject.name = "notes-project"

pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    val ktorVersion = providers.gradleProperty("ktorVersion").get()
    val detektVersion = providers.gradleProperty("detektVersion").get()
    val ktlintPluginVersion = providers.gradleProperty("ktlintPluginVersion").get()

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
