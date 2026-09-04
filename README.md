# Gadget Mover

Gadget Mover is an Android marketplace application for discovering, listing, buying, selling, and managing gadgets. The app includes account and address management, product browsing and filtering, chat, reviews, orders, checkout, returns, wallet features, Stripe card payments, Supabase-backed data, and Google Maps support for location selection.

## Technology stack

- Kotlin and Jetpack Compose with Material 3
- Android Gradle Plugin `9.2.1`
- Kotlin `2.4.10` with Compose, serialization, and Parcelize plugins
- Java 17
- Android compile/target SDK `37`; minimum SDK `26`
- Navigation Compose `2.10.0`
- Supabase Kotlin BOM `3.8.0` for Auth, PostgREST, Storage, Realtime, Edge Functions, and Compose Auth
- Stripe Android `21.19.0` for PaymentSheet and card payments
- Google Maps Compose `6.6.0` and Play Services Location `21.4.0`
- Coil 3 for image loading

## Project structure

```text
app/src/main/java/com/example/gadgetmover/
├── data/          Repositories, Supabase client/models, and local caches
├── model/         Domain models and product filter schemas
├── navigation/    Routes and the Compose navigation graph
├── notification/  Notification support
├── screen/        Account, chat, home, explore, listing, product, checkout, and profile screens
├── ui/            Shared theme and UI styling
└── util/          Validation, formatting, payment, map, and other utilities

supabase/
├── schema.sql     Database tables, policies, functions, and related SQL
└── functions/     Supabase Edge Functions for payments, payment methods, and wallet top-ups
```

The application starts in `app/src/main/java/com/example/gadgetmover/MainActivity.kt`. The activity sets up the Compose content and the navigation graph in `app/src/main/java/com/example/gadgetmover/navigation/NavGraph.kt`. Screens use repositories in the `data` package to load and update application data.

## Local setup

1. Install Android Studio with Android SDK 37 and a Java 17 JDK.
2. Open this repository in Android Studio and allow Gradle to synchronize.
3. Create or update `local.properties` with values for the local Android SDK and configured services:

   ```properties
   sdk.dir=<path-to-your-Android-SDK>
   SUPABASE_URL=<your-Supabase-project-url>
   SUPABASE_ANON_KEY=<your-Supabase-anon-or-publishable-key>
   STRIPE_PUBLISHABLE_KEY=pk_test_<your-test-publishable-key>
   MAPS_API_KEY=<your-Google-Maps-key>
   GOOGLE_WEB_CLIENT_ID=<your-Google-web-client-id>
   RESEND_API_KEY=<your-Resend-configuration>
   RESEND_FROM_ADDRESS=<your-sender-address>
   ```

   Do not put real credentials in this README. Never put Supabase `service_role` keys or Stripe secret keys in the Android app. Card numbers and CVVs must not be stored locally.

4. Select the `app` run configuration and run it on an Android 8.0/API 26 or newer emulator or device.

The Maps picker needs a valid `MAPS_API_KEY` to display map tiles. Supabase and payment features also require the corresponding project configuration and deployed backend functions.

## Build and test

From the repository root on Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Instrumented tests require a running emulator or connected device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Build outputs and caches are generated files and should not be committed.

## Supabase backend

The backend source is under `supabase/`. The current Edge Function areas include:

- Stripe payment intents and payment status
- Stripe customer and saved payment-method management
- Wallet top-up intent and confirmation

Review `supabase/schema.sql` and its Row Level Security policies before applying database changes. Keep privileged service credentials in Supabase Edge Function secrets rather than in the Android client or `local.properties` committed to source control.

## Security and configuration notes

- `local.properties` contains machine-specific configuration and service values; do not copy its values into source files or documentation.
- The Android client may contain publishable/anonymous keys because they are client-side configuration, but access must still be restricted by backend authorization and Row Level Security.
- Stripe secret keys, Supabase service-role keys, Resend server credentials, and other privileged secrets must remain server-side.
- Use test-mode Stripe keys for development and do not store raw card details or CVVs.

## Contribution guidance

Keep commits focused on the feature area being changed. Coordinate edits to shared repositories, models, navigation, manifest configuration, and reusable UI components so that the same file is not independently replaced by multiple contributors. Do not commit IDE metadata, Gradle caches, build outputs, screenshots, dump files, or other generated artifacts.
