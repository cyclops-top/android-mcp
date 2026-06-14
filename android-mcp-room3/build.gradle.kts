import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "top.cyclops.mcp.room.plugin.room3"
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
    api(project(":android-mcp-room-core"))
    api(libs.androidx.room3.runtime)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("top.cyclops", "android-mcp-room3", project.version.toString())

    pom {
        name = project.name
        description = "Room 3 adapter helpers for Android MCP Room provider integration"
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
