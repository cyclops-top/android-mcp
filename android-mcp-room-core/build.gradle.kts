import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "top.cyclops.mcp.room.plugin.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.androidx.sqlite)
}


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("top.cyclops", "android-mcp-room-core", project.version.toString())

    pom {
        name = project.name
        description = "Core interfaces for Room database MCP integration — provider abstractions for Room 2.x and 3.x"
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
