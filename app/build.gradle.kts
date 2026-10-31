plugins { id("com.android.application") }

android {
    namespace = "com.sys.service"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.sys.service"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "3.1.4"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/*" }
}

dependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0") }
