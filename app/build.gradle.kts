plugins { id("com.android.application") }

android {
    namespace = "ru.egergps.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.egergps.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-alpha"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
