import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.localconnect.qazx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  packaging {
    resources {
      // Prevent duplicate-license packaging failures when crypto/socket libs merge
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/INDEX.LIST"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
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
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
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
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Room 2.7.0 KSP codegen can emit localized (Arabic-Indic) digit literals into
// generated Kotlin when the OS regional format is Arabic, producing invalid code.
// Normalize every generated source right after the KSP task finishes.
abstract class NormalizeGeneratedDigitsTask : DefaultTask() {

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val generatedDir: Property<File>

  @TaskAction
  fun normalize() {
    val root = generatedDir.get()
    if (!root.exists()) return
    val arabicDigits = mapOf(
      '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
      '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )
    var fixedFiles = 0
    root.walkTopDown()
      .filter { file -> file.isFile && file.extension == "kt" }
      .forEach { file ->
        val text = file.readText()
        if (arabicDigits.keys.any { text.contains(it) }) {
          var normalized = text
          for ((arabic, latin) in arabicDigits) {
            normalized = normalized.replace(arabic, latin)
          }
          file.writeText(normalized)
          fixedFiles++
        }
      }
    if (fixedFiles > 0) {
      logger.lifecycle("normalizeGeneratedDigits: fixed $fixedFiles generated file(s)")
    }
  }
}

val generatedKspRoot: File = layout.buildDirectory.dir("generated/ksp").get().asFile

val normalizeGeneratedDigits = tasks.register(
  "normalizeGeneratedDigits",
  NormalizeGeneratedDigitsTask::class.java
) {
  generatedDir.set(generatedKspRoot)
  outputs.upToDateWhen { false }
}

tasks.matching {
  it.name.startsWith("ksp") && it.name.endsWith("Kotlin") && !it.name.contains("Test")
}.configureEach {
  finalizedBy(normalizeGeneratedDigits)
}

tasks.matching { it.name == "compileDebugKotlin" || it.name == "compileReleaseKotlin" }.configureEach {
  dependsOn(normalizeGeneratedDigits)
}
