import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  // alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// ---- Release signing ------------------------------------------------------
// The keystore is read from, in order:
//   1. signing.properties (repo root)  — storeFile / storePassword / keyAlias / keyPassword
//   2. environment variables           — SIGNING_STORE_FILE, SIGNING_STORE_PASSWORD, ...
//   3. the demo keystore committed at signing/hscguide-release.p12
// GitHub Actions has no secrets configured, so it uses (3). Anyone cloning the
// repo can build an APK that installs *over* the published releases, because
// every published release is signed with this same key.
val signingProps = Properties().apply {
  val f = rootProject.file("signing.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String, vararg env: String, default: String): String {
  signingProps.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
  env.forEach { name -> System.getenv(name)?.takeIf { it.isNotBlank() }?.let { return it } }
  return default
}

val keystorePath = signingValue(
  "storeFile",
  "SIGNING_STORE_FILE",
  "KEYSTORE_PATH",
  default = rootProject.file("signing/hscguide-release.p12").absolutePath,
)
val keystore = file(keystorePath)
val canSign = keystore.exists()
val keyAliasValue = signingValue("keyAlias", "SIGNING_KEY_ALIAS", "KEY_ALIAS", default = "hscguide")
val storePasswordValue =
  signingValue("storePassword", "SIGNING_STORE_PASSWORD", "STORE_PASSWORD", default = "hscguide2026")
val keyPasswordValue =
  signingValue("keyPassword", "SIGNING_KEY_PASSWORD", "KEY_PASSWORD", default = "hscguide2026")

if (!canSign) {
  logger.warn(
    "::warning::Release keystore not found at $keystorePath — the release build will be signed " +
      "with the debug key instead of the release key."
  )
}

android {
  // Namespace stays com.example: the Kotlin sources live in com.example and the
  // manifest declares activities relative to it (.MainActivity).
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.hasib.guide"
    minSdk = 24
    targetSdk = 36
    versionCode = 6
    versionName = "5.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Release signing. Keep this block: the APK published on GitHub Releases is
  // signed with it, and Android only installs an update when the signing key of
  // the new APK matches the key of the installed one.
  val debugKeystore = file("${rootDir}/debug.keystore")

  signingConfigs {
    create("release") {
      storeFile = keystore
      // The committed keystore is a PKCS#12 container (.p12). AGP defaults to
      // JKS, so the type has to be set explicitly.
      storeType = if (keystore.name.endsWith(".p12", ignoreCase = true)) "PKCS12" else "JKS"
      storePassword = storePasswordValue
      keyAlias = keyAliasValue
      keyPassword = keyPasswordValue
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
    // Optional: only used when a debug.keystore is committed. Otherwise Gradle
    // falls back to the auto-generated ~/.android/debug.keystore.
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName(if (canSign) "release" else "debugConfig")
    }
    debug {
      if (debugKeystore.exists()) signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    viewBinding = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  // implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  // implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.recyclerview)
  implementation(libs.material)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  // implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  // implementation(libs.androidx.compose.material3)
  // implementation(libs.androidx.compose.ui)
  // implementation(libs.androidx.compose.ui.graphics)
  // implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  // implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  // implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // পাসওয়ার্ড-সুরক্ষিত PDF আনলক করার জন্য (Maven Central)
  implementation("com.tom-roush:pdfbox-android:2.0.27.0")
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  "ksp"(libs.moshi.kotlin.codegen)
}
