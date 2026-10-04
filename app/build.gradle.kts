import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fenji.scoretrace"
    compileSdk = 37
    // 已装 build-tools 37.0.0（其原生工具已替换为 ARM64 静态版），显式指定避免 AGP 去下载默认版本
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.fenji.scoretrace"
        // miuix-blur / backdrop 依赖 RuntimeShader，库自身声明 minSdk 33
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // DeepSeek API Key 从 local.properties 读取（该文件已被 .gitignore 忽略，不入库）
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val deepseekApiKey = localProperties.getProperty("deepseek.api.key", "")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    lint {
        // Media3 的 ExoPlayer / Compose UI API 标记为 @UnstableApi
        // （androidx.annotation.RequiresOptIn，由 lint 检查而非编译器强制）
        disable += "UnsafeOptInUsageError"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore（用户偏好设置）
    implementation(libs.androidx.datastore.preferences)

    // 媒体播放（Media3）
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)

    // 网络
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 图片加载（院校校徽）
    implementation(libs.coil.compose)

    // 液体玻璃：KernelSU 同款悬浮底栏（miuix-blur）
    implementation(libs.miuix.blur)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.ui.tooling)
}
