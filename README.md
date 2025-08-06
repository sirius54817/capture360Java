# capture360Java

A Java/Android project for working with 360° capture tools and SDKs.

This repository contains an Android app and related libraries for capturing and processing 360° imagery. The project uses Gradle and is intended to be opened in Android Studio or built from the command line with the included Gradle wrapper.

Contents
 - `app/` - Android application module
 - `CameraSDK-Android/` - camera SDK sources (third-party or internal)
 - `CaptureAndroid/` - capture-related utilities and sample code

Quick start

1. Open the project in Android Studio: `File → Open` and choose this repository root.
2. Or build from the command line (Linux/macOS):

	./gradlew assembleDebug

3. Install the debug APK on a connected device:

	./gradlew installDebug

Notes
- This project may include third-party SDKs. Check `app/src/main/AndroidManifest.xml` and `app/build.gradle` for required permissions and dependencies.
- If you need to change the Gradle JDK or Android SDK path, update `local.properties` accordingly.

Contributing

If you'd like to contribute, please open an issue or a pull request describing your change.

License & Contact

Add your license or contact information here.

