plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.tapjacker.ng"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tapjacker.ng"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
}