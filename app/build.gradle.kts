import java.util.Properties

plugins {
    id("com.android.application")
}

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("signing.properties")
val hasSigningProperties = signingPropertiesFile.exists()

if (hasSigningProperties) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

android {
    namespace = "com.example.hideadb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hideadb"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasSigningProperties) {
            create("release") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigningProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.1")
}
