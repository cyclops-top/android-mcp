plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
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
}

dependencies {
    implementation(project(":room-plugin-core"))
    implementation(project(":mcp"))
    implementation(project(":common"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}
