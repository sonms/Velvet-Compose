import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech)
}

// Consumed by Android library modules that target JVM 11.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

val velvetVersion = "0.0.1"

mavenPublishing {
    coordinates(
        groupId = "io.github.sonms",
        artifactId = "modifier-node-annotations",
        version = velvetVersion
    )

    pom {
        name.set("VelvetCompose Modifier Node Annotations")
        description.set("Annotations that drive Modifier.Node codegen: @ModifierNodeFactory, @Invalidates, @SkipWhenFalse/@SkipWhenTrue and @OnChange. SOURCE retention only 🧩")
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
