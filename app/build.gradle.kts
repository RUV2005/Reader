plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.danmo.reader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.danmo.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // AppCompat 和 Fragment（DocumentPicker 需要）
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)

    // Apache POI - 文档解析
    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)

    // POI 依赖的 XML 库
    implementation(libs.xmlbeans)
    implementation(libs.commons.compress)
    implementation(libs.commons.codec)
    implementation(libs.commons.collections4)
    implementation(libs.commons.math3)
    implementation(libs.sparsebitset)

    // PDF 解析 - PdfBox
    implementation(libs.tom.roush.pdfbox.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 文件选择
    implementation(libs.androidx.documentfile)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}