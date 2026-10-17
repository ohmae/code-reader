import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.ManagedVirtualDevice.PageAlignment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineProfile)
}

android {
    namespace = "net.mm2d.codereader.baseline.profile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    targetProjectPath = ":app"
    testOptions.managedDevices.allDevices {
        @Suppress("UnstableApiUsage")
        create<ManagedVirtualDevice>("pixel10Api37") {
            device = "Pixel 10"
            apiLevel = 37
            systemImageSource = "google"
            pageAlignment = PageAlignment.FORCE_16KB_PAGES
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

baselineProfile {
    managedDevices += "pixel10Api37"
    useConnectedDevices = false
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(libs.androidxJunit)
    implementation(libs.benchmarkMacroJunit4)
    implementation(libs.testRule)
    implementation(libs.uiAutomator)
}

androidComponents {
    onVariants { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        @Suppress("UnstableApiUsage")
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)!!.applicationId },
        )
    }
}
