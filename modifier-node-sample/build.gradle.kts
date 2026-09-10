plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sonms.modifiernode.sample"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    implementation(project(":modifier-node-annotations"))
    ksp(project(":modifier-node-processor"))
    kspTest(project(":modifier-node-processor"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.reflect)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Robolectric 이 두 변형 모두에서 ComponentActivity 를 찾을 수 있어야 한다 (debug 전용이면 release 테스트가 깨진다).
    // Robolectric needs ComponentActivity in both variants — a debug-only manifest breaks the release unit tests.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    releaseImplementation(libs.androidx.compose.ui.test.manifest)
}
