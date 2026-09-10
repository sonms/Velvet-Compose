plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":modifier-node-annotations"))
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

val velvetVersion = "0.0.1"

mavenPublishing {
    coordinates(
        groupId = "io.github.sonms",
        artifactId = "modifier-node-processor",
        version = velvetVersion
    )

    pom {
        name.set("VelvetCompose Modifier Node Processor")
        description.set("KSP processor that generates the ModifierNodeElement and the Modifier extension function for every @ModifierNodeFactory node 🚀")
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
