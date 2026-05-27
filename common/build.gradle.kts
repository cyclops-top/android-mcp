import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("top.cyclops", "android-mcp-${project.name}", project.version.toString())

    pom {
        name = project.name
        description = "Shared data types and configuration for the Android MCP library"
        url = "https://github.com/cyclops-top/android-mcp"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://spdx.org/licenses/Apache-2.0.html"
            }
        }
        developers {
            developer {
                id = "cyclops-top"
                name = "Justin cheng"
                url = "https://www.cyclops.top"
            }
        }
        scm {
            url = "https://github.com/cyclops-top/android-mcp"
            connection = "scm:git:git@github.com:cyclops-top/android-mcp.git"
            developerConnection = "scm:git:ssh://git@github.com:cyclops-top/android-mcp.git"
        }
    }
}