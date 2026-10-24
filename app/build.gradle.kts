
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tangyuanlongniang.pet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tangyuanlongniang.pet"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "1.1"
    }
}
