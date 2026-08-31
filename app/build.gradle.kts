import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.muse.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muse.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // 签名密钥凭据从本机 keystore.properties 读取，避免把口令写进仓库
    val ksProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val releaseKeystore = rootProject.file(ksProps.getProperty("storeFile", "muse-release.keystore"))
    if (releaseKeystore.exists() && ksProps.getProperty("storePassword") != null) {
        signingConfigs {
            create("release") {
                storeFile = releaseKeystore
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias", "muse")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isShrinkResources = true
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.windowsizeclass)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.exifinterface)

    debugImplementation(libs.androidx.ui.tooling)
}
