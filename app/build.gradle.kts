import java.util.Properties

// AGP 9 부터는 Kotlin 지원이 AGP 에 내장되어 있다.
// 여기에 org.jetbrains.kotlin.android 플러그인을 추가하면
// "Cannot add extension with name 'kotlin'" 오류가 나므로 적용하지 않는다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// local.properties 는 .gitignore 에 들어 있어 커밋되지 않는다 — 비밀 키는 여기에만 둔다.
// 팀원 각자 local.properties 에 아래 두 줄을 추가해야 빌드/배포가 된다:
//   GEMINI_API_KEY=<발급받은 키>
//   USE_REAL_ROBOT=true   (실제 temi 에 배포할 때만. 에뮬레이터 개발 중엔 false 또는 생략)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val geminiApiKey: String = localProps.getProperty("GEMINI_API_KEY") ?: ""
val useRealRobot: Boolean = localProps.getProperty("USE_REAL_ROBOT")?.toBoolean() ?: false

android {
    namespace = "kr.ac.kw.followme"
    compileSdk = 37

    defaultConfig {
        applicationId = "kr.ac.kw.followme"
        // temi V2 = Android 6.0.1(API 23), temi V3 = Android 11(API 30)
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("boolean", "USE_REAL_ROBOT", "$useRealRobot")
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
        isCoreLibraryDesugaringEnabled = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 실제 temi 로봇 SDK (TemiRobotController)
    implementation(libs.temi.sdk)
    // Gemini API 호출용 (GeminiClient)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
