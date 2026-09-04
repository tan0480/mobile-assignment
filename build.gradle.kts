plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    // Pulls in kotlin-parcelize-runtime, which the app itself never needs directly but Stripe's
    // 3DS2 module (payments-core/stripe-3ds2-android) does at runtime for its @Parcelize
    // classes — without this plugin applied somewhere in the build, `kotlinx.parcelize.Parceler`
    // is missing from the classpath and any 3D Secure card crashes with NoClassDefFoundError.
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.10" apply false
}
