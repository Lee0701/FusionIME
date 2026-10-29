plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    kotlin("plugin.serialization") version(libs.versions.kotlin)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies {
    implementation(libs.kotaml.core)
    implementation(project(":app:korean:hangul"))
}
