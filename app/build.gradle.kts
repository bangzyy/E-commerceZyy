plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services) // Menggunakan alias dari libs.versions.toml
    id("kotlin-parcelize")
}

android {
    namespace = "com.pab.ecommerce_katalog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pab.ecommerce_katalog"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Firebase Dependencies (Corrected to use version catalog)
    implementation(platform(libs.firebase.bom)) // Hanya satu deklarasi BOM
    implementation(libs.firebase.auth.ktx)     // Menggunakan alias
    implementation(libs.firebase.firestore.ktx)  // Menggunakan alias
    implementation(libs.firebase.storage.ktx)  // Menggunakan alias
    implementation(libs.firebase.analytics)    // Menggunakan alias

    // Google Sign-In Dependency
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}