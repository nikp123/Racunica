plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

    id("com.google.devtools.ksp")
}

android {
    namespace = "com.github.nikp123.racunica"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.nikp123.racunica"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
      release {
          isMinifyEnabled = true
          proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              "proguard-rules.pro"
          )
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
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "mozilla/public-suffix-list.txt"
        }
    }
    signingConfigs {
      create("release") {
        storeFile = file("../private/keys/github-release.jks")
        storePassword = System.getenv("STORE_PASSWORD") ?: "missing-store-password"
        keyAlias = System.getenv("KEY_ALIAS") ?: "missing-key-alias"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "missing-key-password"
      }
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.kotlinx.coroutines.core) // JVM
    implementation(libs.kotlinx.coroutines.android) // Android
    implementation(libs.material)
    implementation(libs.skrapeit)
    implementation(libs.quickie.bundled)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room library (abstraction over SQLite)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(libs.androidx.room.ktx)
    implementation(kotlin("test"))
}
