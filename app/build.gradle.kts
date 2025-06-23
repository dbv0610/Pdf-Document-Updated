
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.gmsGoogleServices)
    alias(libs.plugins.firebaseCrashlytics)
    id("kotlin-parcelize")
    id(id = "com.google.devtools.ksp")
}

android {
    namespace = "com.azg.pdf8"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.azg.pdf8"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val formattedDate = SimpleDateFormat("MMM.dd.yyyy").format(Date())
        base.archivesName = "PDF8-v$versionName($versionCode)_${formattedDate}"
    }

    signingConfigs {
        create("release") {
            keyAlias = "key"
            keyPassword = "123456"
            storeFile = rootProject.file("keystore/key.jks")
            storePassword = "123456"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
             signingConfig = signingConfigs.getByName("release")
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    flavorDimensions.add("version")
    productFlavors {
        create("dev") {

            applicationId = "com.azg.pdf8"
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"

            buildConfigField("boolean", "build_debug", "true")

        }

        create("product") {

            applicationId = ""
            manifestPlaceholders["ad_app_id"] = ""

            buildConfigField("boolean", "build_debug", "false")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.lottie)
    implementation(libs.inappupdate)
    implementation(libs.app.update.ktx)
    implementation(libs.billing.ktx)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.sdp.android)
    implementation(libs.shimmer)
    implementation(libs.ssp.android)
    implementation(libs.review.ktx)

    implementation(project(":lib"))

    implementation(libs.koin.android)
    implementation(libs.koin.android.compat)
    implementation(libs.koin.core)

    implementation(libs.androidx.lifecycle.process)


}