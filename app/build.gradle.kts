plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ee.oyatl.ime.fusion"
    compileSdk = 36

    defaultConfig {
        applicationId = "ee.oyatl.ime.fusion"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("paid") {
            dimension = "edition"
            applicationIdSuffix = ""
            buildConfigField("boolean", "IS_PAID", "true")
        }
        create("free") {
            dimension = "edition"
            applicationIdSuffix = ".free"
            buildConfigField("boolean", "IS_PAID", "false")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    androidResources {
        noCompress += "dat"
        noCompress += "dict"
    }
    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.guava)
    implementation(libs.protobuf.java)
    implementation(libs.androidx.preference)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":app:keyboard"))
    implementation(project(":app:mozc"))
    implementation(project(":app:pinyin"))
    implementation(project(":app:zhuyin"))
    implementation(project(":app:cangjie"))
    implementation(project(":app:korean"))
    implementation(project(":app:korean:hangul"))
    implementation(project(":app:latin"))
    implementation(project(":app:viet"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}