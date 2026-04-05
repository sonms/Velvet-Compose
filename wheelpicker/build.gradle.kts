plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.sonms.wheelpicker"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val velvetVersion = "0.0.1"

mavenPublishing {
    coordinates(
        groupId = "io.github.sonms",
        artifactId = "wheelpicker",
        version = velvetVersion
    )

    pom {
        name.set("VelvetCompose WheelPicker")
        description.set("Bring the classic iOS 3D wheel picker to your Jetpack Compose app\uD83D\uDE80 Features infinite scrolling and dynamic 3D graphic layers.")
        url.set("https://github.com/sonms/Velvet-Compose")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("sonms")
                name.set("sonms")
                email.set("smsms5676@naver.com")
            }
        }

        scm {
            url.set("https://github.com/sonms/Velvet-Compose")
            connection.set("scm:git:git://github.com/sonms/Velvet-Compose.git")
            developerConnection.set("scm:git:ssh://git@github.com/sonms/Velvet-Compose.git")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}