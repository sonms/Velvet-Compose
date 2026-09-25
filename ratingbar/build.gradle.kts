import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech)
    alias(libs.plugins.dokka)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

android {
    namespace = "com.sonms.ratingbar"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
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
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}

dokka {
    dokkaSourceSets.named("commonMain") {
        enableAndroidDocumentationLink.set(false)
    }
}

val velvetVersion = "0.0.3"

mavenPublishing {
    coordinates(
        groupId = "io.github.sonms",
        artifactId = "ratingbar",
        version = velvetVersion
    )

    pom {
        name.set("VelvetCompose RatingBar")
        description.set("A highly customizable RatingBar for Jetpack Compose with spring animation and haptic feedback support.")
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
