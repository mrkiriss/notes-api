rootProject.name = "notes-project"

pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlin_version").get()
    val ktorVersion = providers.gradleProperty("ktor_version").get()
    val detektVersion = providers.gradleProperty("detekt_version").get()
    val ktlintVersion = providers.gradleProperty("ktlint_version").get()

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
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
