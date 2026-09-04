import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.gadgetmover"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.gadgetmover"
        // supabase-kt requires minSdk 26+ (or core library desugaring on older
        // levels); bumped from 24 since Android 7 share is negligible by 2026.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Sourced from local.properties (gitignored) rather than committed to source —
        // a mobile client can never truly keep a secret since the compiled APK can be
        // decompiled, so treat this as a rotate-after-testing demo key, not a production one.
        buildConfigField("String", "RESEND_API_KEY", "\"${localProperties.getProperty("RESEND_API_KEY", "")}\"")
        buildConfigField("String", "RESEND_FROM_ADDRESS", "\"${localProperties.getProperty("RESEND_FROM_ADDRESS", "Gadget Mover <onboarding@resend.dev>")}\"")
        // The anon key is designed to be embedded client-side — it's constrained entirely
        // by Postgres Row Level Security, unlike a true secret. Still sourced from
        // local.properties for consistency and easy rotation between environments.
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        // Stripe's publishable key is meant to be embedded client-side (same trust model as the
        // Supabase anon key above) — it can only ever *create* a PaymentIntent client-side
        // confirmation, never move money on its own. The secret key that actually talks to
        // Stripe's API lives only in the `create-payment-intent`/`get-payment-status` Supabase
        // Edge Functions' secrets, never here. Use a `pk_test_...` key — this app is test-mode only.
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${localProperties.getProperty("STRIPE_PUBLISHABLE_KEY", "")}\"")
        // The Web OAuth client id from Google Cloud Console — passed to supabase-kt's Compose Auth
        // plugin (SupabaseClient.kt) for native "Sign in with Google" via Credential Manager. This
        // is the Web client, not the Android one; see the setup steps this key's addition was
        // documented with. Not a secret in the traditional sense (Google's own docs say the Web
        // client id is safe to ship client-side), but still local.properties-sourced for the same
        // easy-rotation reasons as the keys above.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")

        // Google Maps SDK for Android reads its key from this manifest placeholder (AndroidManifest.xml's
        // com.google.android.geo.API_KEY meta-data) rather than BuildConfig, since the SDK's native
        // (non-Kotlin) init code reads it straight out of the manifest.
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Device fingerprint/face unlock for the "sign back in" app-lock and the wallet
    // payment/withdraw confirmation gates — see util/BiometricAuthenticator.kt. 1.1.0 is the
    // latest stable release on Google's Maven (checked directly; 1.2.0+ is still alpha-only).
    implementation("androidx.biometric:biometric:1.1.0")
    // Reads JPEG orientation off camera captures so the avatar crop screen (AvatarCropDialog.kt)
    // shows/crops a photo right-side-up regardless of how the device physically held the camera.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.10.0")

    implementation("io.coil-kt.coil3:coil-compose:3.6.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    // Live-subscribes to new rows in `public.notifications` so a system tray notification can be
    // posted while the app is running (see NotificationRepository.startRealtimeListening).
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    // Calls the `create-payment-intent`/`get-payment-status` Edge Functions (see supabase/functions/).
    implementation("io.github.jan-tennert.supabase:functions-kt")
    // Native "Sign in with Google" (Credential Manager) wired into auth-kt — see SupabaseClient.kt's
    // ComposeAuth install and LoginScreen.kt. Transitively pulls in androidx.credentials and
    // Google's Identity library, matching the BOM version above (published at the same 3.8.0).
    implementation("io.github.jan-tennert.supabase:compose-auth")
    implementation("io.ktor:ktor-client-okhttp:3.5.1")

    // Stripe Android SDK — PaymentSheet, test mode only (publishable key gated, see above).
    // Version checked against Maven Central directly rather than guessed (latest stable as of
    // this pass); Stripe ships its own Compose-compatible UI, not tied to this project's
    // compose-compiler/compose-bom version.
    implementation("com.stripe:stripe-android:21.19.0")

    // Google Maps SDK for Android + its Compose wrapper, for the address/meetup-location picker
    // (screen/components/LocationPickerScreen.kt) — needs MAPS_API_KEY in local.properties to
    // actually render tiles. play-services-location backs the picker's optional "use my
    // location" button. Versions checked against Maven Central directly.
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.maps.android:maps-compose:6.6.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
