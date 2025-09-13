plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.android.inputmethod.latin"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            ndkBuild {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        register("paid") {
            // Need to match with @string/authority
            buildConfigField("String", "DICTIONARY_DOMAIN", "\"ee.oyatl.ime.fusion.latin.dictionarypack.aosp\"")
            buildConfigField("String", "RESOURCE_PACKAGE_NAME", "\"ee.oyatl.ime.fusion\"")
        }
        register("free") {
            // Need to match with @string/authority
            buildConfigField("String", "DICTIONARY_DOMAIN", "\"ee.oyatl.ime.fusion.free.latin.dictionarypack.aosp\"")
            buildConfigField("String", "RESOURCE_PACKAGE_NAME", "\"ee.oyatl.ime.fusion.free\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    androidResources {
        noCompress += "dict"
    }
    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    ndkVersion = "29.0.14033849 rc4"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.jsr305)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.annotation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}