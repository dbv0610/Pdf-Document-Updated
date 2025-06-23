plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.gmsGoogleServices)
    alias(libs.plugins.firebaseCrashlytics)
    id("kotlin-parcelize")
}

android {
    namespace = "com.ag.sampleadsfirstflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ag.sampleadsfirstflow"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    flavorDimensions.add("version")
    productFlavors {
        create("dev") {
            applicationId = "com.ag.sampleadsfirstflow"
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "A001", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "A002", "\"ca-app-pub-3940256099942544/9257395921\"")

            buildConfigField("String", "B001", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "B002", "\"ca-app-pub-3940256099942544/6300978111\"")

            buildConfigField("String", "I001", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I002", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I003", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I004", "\"ca-app-pub-3940256099942544/1033173712\"")

            buildConfigField("String", "N001", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N002", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N003", "\"ca-app-pub-3940256099942544/1044960115\"")
            buildConfigField("String", "N004", "\"ca-app-pub-3940256099942544/1044960115\"")
            buildConfigField("String", "N005", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N006", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N007", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N008", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N009", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N010", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N011", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N012", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N013", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N014", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N015", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N016", "\"ca-app-pub-3940256099942544/2247696110\"")

            buildConfigField("boolean", "build_debug", "true")
        }

        create("product") {
            applicationId = "com.ag.sampleadsfirstflow"
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "A001", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "A002", "\"ca-app-pub-3940256099942544/9257395921\"")

            buildConfigField("String", "B001", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "B002", "\"ca-app-pub-3940256099942544/6300978111\"")

            buildConfigField("String", "I001", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I002", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I003", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "I004", "\"ca-app-pub-3940256099942544/1033173712\"")

            buildConfigField("String", "N001", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N002", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N003", "\"ca-app-pub-3940256099942544/1044960115\"")
            buildConfigField("String", "N004", "\"ca-app-pub-3940256099942544/1044960115\"")
            buildConfigField("String", "N005", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N006", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N007", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N008", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N009", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N010", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N011", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N012", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N013", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N014", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N015", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "N016", "\"ca-app-pub-3940256099942544/2247696110\"")

            buildConfigField("boolean", "build_debug", "false")
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.app.update.ktx)
    implementation(libs.azmoduleads)
    implementation(libs.billing)
    implementation(libs.billing.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.inappupdate)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compat)
    implementation(libs.koin.core)
    implementation(libs.lottie)
    implementation(libs.material)
    implementation(libs.mediation.applovin)
    implementation(libs.mediation.facebook)
    implementation(libs.mediation.mintegral)
    implementation(libs.mediation.pangle)
    implementation(libs.mediation.vungle)
    implementation(libs.play.app.update)
    implementation(libs.play.services.ads)
    implementation(libs.sdp.android)
    implementation(libs.shimmer)
    implementation(libs.ssp.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
