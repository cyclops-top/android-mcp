import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "top.cyclops.mcp.room.plugin"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

dependencies {
    implementation(project(":room-plugin-core"))
    implementation(project(":mcp"))
    implementation(project(":common"))
    implementation(libs.androidx.sqlite)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("top.cyclops", "android-mcp-${project.name}", project.version.toString())

    pom {
        name = project.name
        description = "Room database MCP tools — execute SQL, inspect schema, and list databases on Android via MCP"
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
