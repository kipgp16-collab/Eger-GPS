plugins { id("com.android.application") }

android {
    namespace = "ru.egergps.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.egergps.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"
    }
    signingConfigs {
        create("production") {
            val path = System.getenv("EGER_KEYSTORE_PATH")
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = System.getenv("EGER_STORE_PASSWORD")
                keyAlias = System.getenv("EGER_KEY_ALIAS")
                keyPassword = System.getenv("EGER_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("production")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
