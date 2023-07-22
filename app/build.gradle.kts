import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.util.Locale

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    id("com.github.ben-manes.versions")

    // for release
}

val applicationName = "CodeReader"
val versionMajor = 0
val versionMinor = 1
val versionPatch = 3

android {
    compileSdk = 33

    namespace = "net.mm2d.codereader"
    defaultConfig {
        applicationId = "net.mm2d.codereader"
        minSdk = 23
        targetSdk = 33
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        vectorDrawables.useSupportLibrary = true
        base.archivesName.set("${applicationName}-${versionName}")
        multiDexEnabled = true
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "${applicationName}-${versionName}.apk"
            }
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            enableAndroidTestCoverage = true
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        abortOnError = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.android.play:core-ktx:1.8.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("androidx.browser:browser:1.5.0")
    implementation("androidx.webkit:webkit:1.7.0")
    implementation("com.google.mlkit:barcode-scanning:17.1.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    debugImplementation("com.facebook.flipper:flipper:0.201.0")
    debugImplementation("com.facebook.soloader:soloader:0.10.5")
    debugImplementation("com.facebook.flipper:flipper-network-plugin:0.201.0")
    debugImplementation("com.facebook.flipper:flipper-leakcanary2-plugin:0.201.0")

    // for release
}

fun isStable(version: String): Boolean {
    val versionUpperCase = version.uppercase(Locale.getDefault())
    val hasStableKeyword = listOf("RELEASE", "FINAL", "GA").any { versionUpperCase.contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return hasStableKeyword || regex.matches(version)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf { !isStable(candidate.version) }
}
