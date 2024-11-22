
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // Use alias for KSP plugin
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            javaCompileOptions {
                annotationProcessorOptions {
                    arguments["room.schemaLocation"] = "$projectDir/schemas".toString()
                }

            }
        }

        packaging {
            resources {
                excludes.add("META-INF/DEPENDENCIES")
                excludes.add("META-INF/LICENSE")
                excludes.add("META-INF/LICENSE.txt")
                excludes.add("META-INF/NOTICE")
                excludes.add("META-INF/NOTICE.txt")
                excludes.add("META-INF/INDEX.LIST")
            }
        }
    }
    viewBinding {
        enable = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1") // Use KSP for Room
    implementation("androidx.room:room-ktx:2.6.1") // For Coroutines support

    implementation("com.google.cloud:google-cloud-speech:4.49.0")
    implementation ("androidx.databinding:databinding-runtime:8.7.2")

    implementation ("org.tensorflow:tensorflow-lite:2.16.1")  // Pastikan menggunakan versi terbaru
    implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.3")

}