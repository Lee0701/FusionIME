import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
}

configure<ApplicationExtension> {
    namespace = "ee.oyatl.ime.fusion"
    compileSdk = 37

    defaultConfig {
        applicationId = "ee.oyatl.ime.fusion"
        minSdk = 24
        targetSdk = 37
        versionCode = 20
        versionName = "20261024-20-09979d0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("free") {
            isDefault = true
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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.material)
    implementation(libs.guava)
    implementation(libs.protobuf.java)
    implementation(libs.androidx.preference)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":app:keyboard"))
    implementation(project(":app:mozc"))
    implementation(project(":app:pinyin"))
    implementation(project(":app:chewing"))
    implementation(project(":app:cangjie"))
    implementation(project(":app:korean"))
    implementation(project(":app:korean:hangul"))
    implementation(project(":app:latin"))
    implementation(project(":app:viet"))
    implementation(project(":app:jyutping"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.register("printVersionCode") {
    println(android.defaultConfig.versionCode)
}

tasks.register("printVersionName") {
    println(android.defaultConfig.versionName)
}

tasks.register("printPackageName") {
    println(android.defaultConfig.applicationId)
}

tasks.register("printFreePackageNameSuffix") {
    println(android.productFlavors["free"].applicationIdSuffix)
}
