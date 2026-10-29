plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ee.oyatl.ime.fusion.stroke"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":app:dictionary"))
    testImplementation(libs.junit)
}
