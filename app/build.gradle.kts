import java.net.HttpURLConnection
import java.net.URL

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.yassernull.shappky"
  compileSdk {
    version = release(36) {
      minorApiLevel = 1
    }
  }

  defaultConfig {
    applicationId = "com.yassernull.shappky"
    minSdk = 24
    targetSdk = 36
    versionCode = 5
    versionName = "2.0.0"
    multiDexEnabled = true
  }

  flavorDimensions += "abi"
  productFlavors {
    create("arm64-v8a") {
      dimension = "abi"
      ndk { abiFilters += listOf("arm64-v8a") }
    }
    create("armeabi-v7a") {
      dimension = "abi"
      ndk { abiFilters += listOf("armeabi-v7a") }
    }
    create("x86_64") {
      dimension = "abi"
      ndk { abiFilters += listOf("x86_64") }
    }
    create("x86") {
      dimension = "abi"
      ndk { abiFilters += listOf("x86") }
    }
    create("universal") {
      dimension = "abi"
      ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
    aidl = true
  }
  lint {
    baseline = file("lint-baseline.xml")
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

val toyboxUrls = mapOf(
  "arm64-v8a" to "https://landley.net/toybox/bin/toybox-aarch64",
  "armeabi-v7a" to "https://landley.net/toybox/bin/toybox-armv7l",
  "x86_64" to "https://landley.net/toybox/bin/toybox-x86_64",
  "x86" to "https://landley.net/toybox/bin/toybox-i686",
)

tasks.register<DownloadToyboxTask>("downloadToybox") {
  outputDir.set(file("src/main/jniLibs"))
  urls.set(toyboxUrls)
}

abstract class DownloadToyboxTask : DefaultTask() {
  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  abstract val urls: MapProperty<String, String>

  @TaskAction
  fun download() {
    val out = outputDir.get().asFile
    out.mkdirs()

    for ((abi, url) in urls.get()) {
      val dir = File(out, abi)
      dir.mkdirs()
      val toyboxFile = File(dir, "libtoybox.so")

      if (toyboxFile.exists()) {
        logger.lifecycle("Toybox already exists for $abi, skipping download")
        continue
      }

      logger.lifecycle("Downloading toybox for $abi from $url")
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.inputStream.use { input ->
        toyboxFile.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      toyboxFile.setExecutable(true)
      logger.lifecycle("Downloaded toybox for $abi -> ${toyboxFile.absolutePath}")
    }
  }
}

tasks.named("preBuild") {
  dependsOn("downloadToybox")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(platform(libs.androidx.compose.bom))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.android.material)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  // libsu
  implementation(libs.libsu.core)
  implementation(libs.libsu.service)
  implementation(libs.libsu.nio)
  // shizuku
  implementation(libs.shizuku.api)
  implementation(libs.shizuku.provider)
  // Tasker Plugin Library
  implementation(libs.tasker.plugin.library)
}
