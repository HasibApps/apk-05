import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---- Release signing ----
// Signing values are read from gradle.properties / env vars.
// A throwaway demo keystore is committed at signing/hscguide-release.p12 so that
// GitHub Actions can produce an installable, signed release APK without secrets.
val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String, default: String?): String? =
    signingProps.getProperty(key) ?: System.getenv(env) ?: default

val storeFilePath = signingValue(
    "storeFile", "SIGNING_STORE_FILE",
    rootProject.file("signing/hscguide-release.p12").absolutePath
)
val storePasswordValue = signingValue("storePassword", "SIGNING_STORE_PASSWORD", "hscguide2026")
val keyAliasValue = signingValue("keyAlias", "SIGNING_KEY_ALIAS", "hscguide")
val keyPasswordValue = signingValue("keyPassword", "SIGNING_KEY_PASSWORD", "hscguide2026")
val canSign = storeFilePath != null && file(storeFilePath).exists()

android {
    namespace = "com.hasib.guide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hasib.guide"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(storeFilePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "DebugProbesKt.bin")
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
}
