plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.yn.shappky"
  compileSdk {
    version = release(36) {
      minorApiLevel = 1
    }
  }

  defaultConfig {
    applicationId = "com.yn.shappky"
    minSdk = 24
    targetSdk = 36
    versionCode = 5
    versionName = "2.0.0"
    multiDexEnabled = true
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
  implementation("com.joaomgcd:taskerpluginlibrary:0.4.10")
}
