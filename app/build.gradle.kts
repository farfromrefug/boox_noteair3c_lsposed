
plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "com.akylas.noteair3c.lsposed"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            multiDexEnabled = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x42")
    }

    kotlin {
        jvmToolchain(17)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        checkReleaseBuilds = false
        disable +=
            listOf(
                "Internationalization",
                "UnsafeIntentLaunch",
                "SetJavaScriptEnabled",
                "UnspecifiedRegisterReceiverFlag",
                "Usability:Icons")
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "${android.namespace}-${variant.versionName}.${variant.versionCode}.apk"
                println("OutputFileName: $outputFileName")
                output.outputFileName = outputFileName
            }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
//    compileOnly(libs.libxposed.api)
//    implementation(libs.libxposed.service)
    dependencies { compileOnly(libs.api) }
    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.material)

}

