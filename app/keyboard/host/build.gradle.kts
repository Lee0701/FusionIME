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

tasks.register<JavaExec>("generateKeyboardLayouts") {
    description = "Generates keyboard layout YAMLs fro sources"
    mainClass = "ee.oyatl.ime.fusion.keyboard.host.KeyboardLayoutGeneratorKt"
    classpath += sourceSets["main"].runtimeClasspath
    val dependencyAssets = layout.buildDirectory.dir("generated/dependencyAssets").get().asFile.path
    args = listOf("$dependencyAssets/keyboard")
}

dependencies {
    implementation(libs.kotaml.core)
    implementation(project(":app:keyboard:core"))
    implementation(project(":app:korean:hangul"))
    testImplementation(libs.junit)
}
