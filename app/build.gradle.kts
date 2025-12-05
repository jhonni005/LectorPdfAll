plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zonadev.lectordocumentos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zonadev.lectordocumentos"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    
    //Maxima Velocidad (Release)
    buildTypes {
        release {
            // Habilita R8 (el optimizador de código de Google)
            isMinifyEnabled = false
            isShrinkResources = false

            // Reglas de optimización estándar
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // TRUCO: Usar la firma de debug para poder darle al botón "Play"
            // y que se instale sin pedirte contraseñas.
            // signingConfig = signingConfigs.getByName("debug")
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
        compose = true
    }
}

dependencies {

    implementation ("androidx.paging:paging-runtime:3.3.0")
    implementation ("androidx.paging:paging-compose:3.3.0")

    //implementation("com.github.mhiew:PdfiumAndroid:1.9.1")
    //implementation ("com.github.barteksc:pdfium-android:1.9.0")

    implementation("io.legere:pdfiumandroid:1.0.35")



    //  implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Jetpack Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Librería para ver PDFs
    //implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1")

    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}