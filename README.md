# BlueTap

A local Android utility for personalized Bluetooth device widgets.

> **Status: pre-Android-17 foundation complete.** Device assignment, customization
> and responsive widgets work. Connect/disconnect, live connection state and generic
> battery reporting are not implemented. Widget taps open the editor.

Read [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) before resuming development.

## What BlueTap will do

The goal is to let you:

1. Place a BlueTap widget on your home screen.
2. Assign one of your already-paired Bluetooth audio devices (e.g. earbuds or headphones) to it.
3. Tap the widget to connect or disconnect that device.

## Android 17

The next phase will verify the Android 17 (API 37) public Bluetooth connect/disconnect
APIs, permissions and behavior on a real device before implementing them. The current
app still compiles and targets API 36; no API 37 functionality is enabled.

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
  created automatically. There are no temporary investigation logs or reflection.
  The connector's legacy association-based contract must be revisited when wiring
  the selected bonded-device MAC; CDM endpoint matching is not solved or assumed.
- A `BluetoothConnector` interface and `ConnectionState` model that keep the UI and
  widget independent of Android Bluetooth classes.
- A placeholder `Android17BluetoothConnector` that is **not implemented** and not used.

### Visuals and widget editor

The app includes a BlueTap adaptive vector icon, Android 13+ themed icon, light/dark
palettes, and an optional wallpaper-color theme. Device classification prefers specific
Bluetooth class metadata, then conservative whole-word names (buds, headphones,
speaker, watch, controller, GameSir, etc.). Ambiguous names retain a generic link symbol.

Each widget has an independent appearance: Blue Tap, Dot Mono, Hyper Glass,
Material You, OLED, Minimal, Compact, or Outline. The editor previews 2×1, 3×1, 2×2,
wide, and large shapes; these are examples, not launcher cell guarantees. Preset,
accent, surface and alignment remain visible; advanced options are under collapsed
Details. Preview background and future-state demos affect only the preview.
Hyper Glass approximates translucency without blur. OLED uses pure black; Dot Mono
uses a monochrome near-black surface. Material You adds a soft circular artwork field.

Older widgets retain their devices, presets and saved appearance. New battery/artwork
flags default on, with battery omitted when unknown and artwork resolved automatically.
Widget appearance saves on **Save widget**. Widget taps open the editor; connection
control remains unimplemented. `SizeMode.Exact` uses the host's available dp dimensions:
Tiny, Sleek, SleekWide, Card, Wide, and Showcase. One-row layouts place artwork beside
readable names and state. Cards prioritize centered artwork and allow two-line names.
Padding, labels and artwork adjust to space and font scale before core information.
One provider supports vertical/horizontal resizing down to a requested 48dp height;
actual available sizes depend on the launcher.

One-row widgets (width at least 110dp, height below 125dp) use a dedicated
`RowWidgetSpec`: artwork plus a single text block, never a separate status column.
Rows below 72dp are slim; the Android 17 placeholder requires width at least 220dp,
height at least 72dp, and room remaining after the name. Known state/battery may use
a single secondary line when it fits. Names use 14sp, secondary text 11sp; fonts are
not shrunk to force content into the bounds. Shared Android text measurement chooses
one or two name lines and balanced word breaks. Height budgets reserve font padding
and 4dp of headroom. The Compose preview uses the same spec and font padding, with
160×56dp / 260×64dp samples for its two one-row previews.

### Device artwork and future state

`DeviceArtwork` supports none, built-in, custom and generic sources. The built-in model
registry is empty until licensed/app-owned product assets are available; original BlueTap
earbud silhouettes and generic vectors supply the fallback. No vendor images are bundled.

**Change image** uses Android's document picker without extra storage permissions.
`ImageDecoder` normalizes orientation and copies the image as a maximum 384px PNG into
private app storage, preserving transparency. No persistent gallery URI is needed.
The mapping belongs to the normalized Bluetooth address. Changes/removal apply immediately
to every widget using that device image, even if the editor is later cancelled. Individual
widgets can hide artwork or use the generic silhouette. Widget deletion leaves shared
artwork intact. Missing/unreadable files fall back to generic artwork.
Artwork edits collect only unreferenced, app-owned PNG files older than 24 hours;
mapped artwork remains until explicitly removed or app data is cleared. Runtime
decoding is bounded and runs off the main thread.

`WidgetDeviceStatus` prepares unknown, unavailable, disconnected, connecting, connected
and disconnecting visuals. Runtime state remains unknown ("Android 17 required") or
unavailable; bonding is never treated as a connection signal. The editor's explicitly
labeled state demos animate three dots; real Glance widgets use static dots, with no
animation update loops. Earbud artwork defaults to the full device; left/right variants
are reserved for an explicit reliable future source, with no detection heuristics.

**Show battery** is persisted independently for each widget, but this build has no battery
source and displays no values. `DeviceBattery` accepts only valid known percentages and
can represent independently known left/right readings. The public `BluetoothDevice`
SDK has no generic cached battery-level accessor; the AOSP battery broadcast/accessor
is a hidden/system API and is not used. No GATT connection is opened to obtain battery.
See the [public SDK reference](https://developer.android.com/reference/android/bluetooth/BluetoothDevice).

The app lists current launcher widgets and can request pinning through Android's
public API. After pinning, tap the new widget to select a device and customize it.
Launchers without pinning support can use their normal Widgets picker.

Availability refreshes on app resume, Refresh, editor saves/artwork edits and launcher
widget events. There is no continuous Bluetooth observer; an off-screen widget may
remain stale until a refresh. This will be revisited with real state reporting.

### Permissions

- `BLUETOOTH_CONNECT`, Android 12+: requested to read bonded devices and their names.
  Permission denial and Bluetooth-off states have setup guidance. The list refreshes
  when returning from Settings or tapping Refresh devices.
- `android.software.companion_device_setup` is an **optional feature**, retained for
  future CDM use. BlueTap does not request location or scanning permissions.

There is no network/storage permission, analytics or device-address logging. Private
settings/artwork are excluded from platform cloud backup and device transfer. Upgrades
and reboot preserve them; reinstalling or moving to a new phone requires setup again.

### Existing development widgets

Saved MAC addresses and names are reused. Old CDM association IDs are ignored and
removed when a widget is saved again. If the saved address is not in the bonded list
(including a different nearby endpoint for the same earbuds), the widget shows
"Device unavailable" until you select the paired device again. Entries without a
saved address need configuration again. BlueTap does not guess address mappings.

## Roadmap

BlueTap's planned post-Android-17 work includes live connection state, one-tap control,
event-driven widget updates, Quick Settings, favorites/nicknames, connection handoff,
multi-device widgets, NFC actions and explicit configuration portability. These are
future plans, dependent on platform validation.

See [ROADMAP.md](ROADMAP.md) for phases, priorities, deferred ideas and non-goals.

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

- JDK 17 (the version used by CI)
- Android SDK with platform 36 installed (point `ANDROID_HOME` at it, or create a
  `local.properties` file containing `sdk.dir=/path/to/Android/sdk`), Build Tools 35.0.0

Windows PowerShell from the repository:

```powershell
$env:ANDROID_HOME = 'C:\Users\rifaq\AppData\Local\Android\Sdk'
java -version # Use JDK 17; set JAVA_HOME to your installed JDK if needed.
.\gradlew.bat clean testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Use your own SDK path on other
machines; do not copy an SDK into the repository.

Unix equivalent:

```sh
./gradlew clean testDebugUnitTest assembleDebug lintDebug
```

Pinned toolchain: Gradle 8.14.3, AGP 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00,
Glance 1.1.1. CI validates the wrapper, runs tests, builds the debug APK, runs Android
lint, and retains APK/reports for 60 days. Wrapper distribution checksum verification
is enabled. No application dependency upgrade was made during the pause audit.

Reports: `app/build/reports/tests/testDebugUnitTest/index.html` and
`app/build/reports/lint-results-debug.html`.
The sandbox audit could not access the SDK. Its source/JVM/resource checks are not a
successful Android build; see the handoff for validation status.

## Requirements

- Minimum Android version: Android 12 (API 31)
- Bluetooth connect/disconnect (not implemented yet): Android 17 (API 37)

## License

[MIT](LICENSE)
