import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "top.cyclops.mcp"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mcp.sdk)
//    implementation(libs.mcp.sdk.server)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appstartup)
}


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("top.cyclops", "android-mcp", project.version.toString())

    pom {
        name = "android-mcp"
        description = "Android MCP (Model Context Protocol) server library — embedded Ktor HTTP/SSE server with Hilt DI and annotation-based tool registration"
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