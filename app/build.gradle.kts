import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi",
        )
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        // 데모 앱이라 배포용 release 프레임워크는 만들지 않는다 (release LTO 링크가 무겁다).
        // Demo app — skip the release framework, its LTO link is heavy and isn't needed.
        it.binaries.framework(listOf(org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG)) {
            baseName = "VelvetComposeApp"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":wheelpicker"))
            implementation(project(":ratingbar"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            // Modifier.Node 코드젠 — 노드 클래스에 @ModifierNodeFactory 를 붙이면
            // Modifier 확장 함수 + ModifierNodeElement 가 생성된다. (modifier-node-sample 참고)
            // KSP 프로세서는 Android/JVM 전용이라 androidMain 에만 붙인다.
            implementation(project(":modifier-node-annotations"))

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

dependencies {
    add("kspAndroid", project(":modifier-node-processor"))
}

android {
    namespace = "com.sonms.velvetcompose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonms.velvetcompose"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
