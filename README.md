# BlueTap

One-tap Android home-screen widgets for connecting and disconnecting paired Bluetooth devices.

> **Status: early development.** You can select an already-paired Bluetooth device
> for each widget, but BlueTap cannot connect to or disconnect from devices yet.

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

- **Paired device picker** using Android's bonded Bluetooth device list. Names fall
  back from the friendly name to the alias, then the MAC address. Devices are sorted
  by name and deduplicated by address. Pair new devices in Android's Bluetooth settings.
- **Per-widget configuration.** Adding a BlueTap widget opens a setup screen where you
  pick a paired device. Each widget stores its own MAC address and display name, so
  several widgets can point to different devices. Tapping a widget lets you change it.
- **Widget states:** "No device selected", the assigned device's name, or
  "Device unavailable" if it is no longer paired or the list cannot be read. The widget does not show a
  connection status yet, because reading it is not implemented.
- **Companion Device Manager code is retained** for a possible future association
  step, but is not called by normal setup or widget rendering. No association is
  created automatically. Temporary investigation logs and reflection have been removed.
- A `BluetoothConnector` interface and `ConnectionState` model that keep the UI and
  widget independent of Android Bluetooth classes.
- A placeholder `Android17BluetoothConnector` that is **not implemented** and not used.

### Permissions

- `BLUETOOTH_CONNECT`, Android 12+: requested to read bonded devices and their names.
  Permission denial and Bluetooth-off states have setup guidance. The list refreshes
  when returning from Settings or tapping Refresh devices.
- `android.software.companion_device_setup` is an **optional feature**, retained for
  future CDM use. BlueTap does not request location or scanning permissions.

### Existing development widgets

Saved MAC addresses and names are reused. Old CDM association IDs are ignored and
removed when a widget is saved again. If the saved address is not in the bonded list
(including a different nearby endpoint for the same earbuds), the widget shows
"Device unavailable" until you select the paired device again. Entries without a
saved address need configuration again. BlueTap does not guess address mappings.

## Project structure

```
app/src/main/java/dev/bluetap/app/
├── ui/          Main activity, device setup screen and Compose theme
├── widget/      Glance widget, receiver, configuration activity and per-widget storage
└── bluetooth/   BondedDeviceProvider, BondedDevice, optional CDM support,
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
