import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

fun versionCodeFromVersionName(versionName: String): Int {
    val parts = versionName.split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size >= 3) {
        return parts[0] * 10_000 + parts[1] * 100 + parts[2]
    }
    return 1
}

val wikiReaderBilingualVersionName =
    providers.gradleProperty("wikiReaderBilingualVersionName").orNull
        ?: System.getenv("GITHUB_REF_NAME")?.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) }
        ?: "2.5.5"
val wikiReaderBilingualVersionCode =
    providers.gradleProperty("wikiReaderBilingualVersionCode").orNull?.toIntOrNull()
        ?: versionCodeFromVersionName(wikiReaderBilingualVersionName)
val localReleaseSigningFile = rootProject.file("release-signing.properties")
val localReleaseSigning = Properties().apply {
    if (localReleaseSigningFile.isFile) {
        localReleaseSigningFile.inputStream().use(::load)
    }
}

fun releaseSigningValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: localReleaseSigning.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseSigningValue("NIHILDIGIT_RELEASE_STORE_FILE", "storeFile")
val releaseStorePassword = releaseSigningValue("NIHILDIGIT_RELEASE_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSigningValue("NIHILDIGIT_RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSigningValue("NIHILDIGIT_RELEASE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning =
    !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.name.contains("Release") &&
            (task.name.startsWith("assemble") ||
                task.name.startsWith("bundle") ||
                task.name.startsWith("package") ||
                task.name.startsWith("install"))
    }
    if (buildsRelease && !hasReleaseSigning) {
        throw GradleException(
            "Release signing is required. Provide NIHILDIGIT_RELEASE_* environment variables " +
                "or release-signing.properties with the same keystore used by GitHub Actions."
        )
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.nsh07.wikireader"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.nihildigit.wikireader.bilingual"
        minSdk = 26
        targetSdk = 36
        versionCode = wikiReaderBilingualVersionCode
        versionName = wikiReaderBilingualVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        androidResources {
            generateLocaleConfig = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.withType(Test::class) {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil3.coil.gif)
    implementation(libs.coil3.coil.svg)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.ehsannarmani.compose.charts)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.material.kolor)
    implementation(libs.okhttp)
    implementation(libs.openai.client)
    implementation(libs.retrofit2.converter.scalars)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.retrofit2.retrofit)
    implementation(libs.latex2unicode.x.x3)
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
