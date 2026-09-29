# BlueTap

One-tap Android home-screen widgets for connecting and disconnecting paired Bluetooth devices.

> **Status: early development.** You can associate a Bluetooth device with BlueTap and
> assign it to a widget, but BlueTap cannot connect to or disconnect from devices yet.

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

- **Device association** through Android's Companion Device Manager. BlueTap opens the
  system device chooser, and the chosen device is associated with the app. BlueTap does
  not pair, bond or connect to anything itself.
- **Per-widget configuration.** Adding a BlueTap widget opens a setup screen where you
  pick an associated device or add a new one. Each widget stores its own device, so
  several widgets can point to different devices. Tapping a widget lets you change it.
- **Widget states:** "No device selected", the assigned device's name, or
  "Device unavailable" if its association was removed. The widget does not show a
  connection status yet, because reading it is not implemented.
- A `BluetoothConnector` interface and `ConnectionState` model that keep the UI and
  widget independent of Android Bluetooth classes.
- A placeholder `Android17BluetoothConnector` that is **not implemented** and not used.

### Permissions

- `android.software.companion_device_setup` feature: required for device association.
- `BLUETOOTH_CONNECT`, **Android 12/12L only**: needed there to read the chosen device's
  name. Android 13+ gets the name from the association, so the permission is not
  requested. BlueTap does not request location or scanning permissions.

## Project structure

```
app/src/main/java/dev/bluetap/app/
├── ui/          Main activity, device setup screen and Compose theme
├── widget/      Glance widget, receiver, configuration activity and per-widget storage
└── bluetooth/   Companion Device Manager association, AssociatedDevice,
                 BluetoothConnector interface and ConnectionState
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
