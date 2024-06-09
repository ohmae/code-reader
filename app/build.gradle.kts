import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.util.Locale

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.gradleVersions)

    // for release
}

val applicationName = "CodeReader"
val versionMajor = 0
val versionMinor = 2
val versionPatch = 3

android {
    compileSdk = 34

    namespace = "net.mm2d.codereader"
    defaultConfig {
        applicationId = "net.mm2d.codereader"
        minSdk = 23
        targetSdk = 34
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"
        vectorDrawables.useSupportLibrary = true
        base.archivesName.set("$applicationName-$versionName")
        multiDexEnabled = true
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "$applicationName-$versionName.apk"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint {
        abortOnError = true
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.kotlinStdlib)
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.androidxCore)
    implementation(libs.androidxAppCompat)
    implementation(libs.androidxActivity)
    implementation(libs.androidxFragment)
    implementation(libs.androidxBrowser)
    implementation(libs.androidxWebkit)
    implementation(libs.androidxPreference)
    implementation(libs.androidxConstraintLayout)
    implementation(libs.bundles.androidxCamera)
    implementation(libs.mlkitBarcodeScanning)
    implementation(libs.material)
    implementation(libs.playReview)
    implementation(libs.playAppUpdate)
    implementation(libs.timber)

    debugImplementation(libs.leakcanary)
    debugImplementation(libs.bundles.flipper)

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
