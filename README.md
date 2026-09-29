# BlueTap

One-tap Android home-screen widgets for connecting and disconnecting paired Bluetooth devices.

> **Status: early development.** BlueTap does not connect to or disconnect from
> Bluetooth devices yet. The app and widget currently show placeholder content only.

## What BlueTap will do

The goal is to let you:

1. Place a BlueTap widget on your home screen.
2. Assign one of your already-paired Bluetooth audio devices (e.g. earbuds or headphones) to it.
3. Tap the widget to connect or disconnect that device.

## Android 17

Android 17 (API 37) adds public APIs that let apps connect and disconnect Bluetooth
devices. BlueTap intends to build on those APIs, so the connect/disconnect feature
will require Android 17 or later.

BlueTap will only use public Android SDK APIs. It does not and will not use hidden
APIs, reflection, root, Shizuku, Accessibility services or vendor-specific protocols.

## What exists today

- A minimal Jetpack Compose + Material 3 app screen.
- A Jetpack Glance home-screen widget showing "No device selected" and a
  "Set up device" button that opens the app.
- A `BluetoothConnector` interface and `ConnectionState` model that keep the UI and
  widget independent of Android Bluetooth classes.
- A placeholder `Android17BluetoothConnector` that is **not implemented** and not used.

## Project structure

```
app/src/main/java/dev/bluetap/app/
├── ui/          Main activity and Compose theme
├── widget/      Glance widget and its receiver
└── bluetooth/   BluetoothConnector interface, ConnectionState, PairedDevice
    └── android17/   Placeholder for the future API 37 implementation
```

The application ID and namespace (`dev.bluetap.app`) are temporary and can be
changed in `app/build.gradle.kts` before publishing.

## Building

Requirements:

- JDK 17 or newer
- Android SDK with platform 36 installed (point `ANDROID_HOME` at it, or create a
  `local.properties` file containing `sdk.dir=/path/to/Android/sdk`)

Build the debug APK:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

Run the unit tests:

```sh
./gradlew testDebugUnitTest
```

GitHub Actions runs both on every push and pull request.

## Requirements

- Minimum Android version: Android 12 (API 31)
- Bluetooth connect/disconnect (not implemented yet): Android 17 (API 37)

## License

[MIT](LICENSE)
