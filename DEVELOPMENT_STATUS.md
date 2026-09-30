# BlueTap Development Status

Audit date: 2026-09-30. Planned pause: approximately 45 days.
Status: **pre-Android-17 foundation complete; audit changes await host Android build
and phone smoke testing.** No release/tag has been created.

## Current status

Implemented: Android 12+, bonded-device discovery and friendly names, conservative
classification, per-widget MAC assignment, independent appearance, eight presets,
exact-size Glance layouts, editor previews, local shared artwork, Material You,
light/dark themes, adaptive/themed icon and widget management/pinning.

The owner has tested the foundation and revised one-row layouts on a real launcher.
This audit did not run a new phone session. Current widget taps open the editor.
Connect/disconnect, live connection state and generic battery reporting do not work yet.

## Real-device findings

- CDM's chooser was unsuitable for selecting already-bonded devices. It listed nearby
  discoverable endpoints, sometimes only as MACs, and omitted the bonded earbuds.
- A CDM endpoint differed from the earbuds' bonded classic Bluetooth address. Do not
  correlate these by name or assume association means the audio endpoint is selected.
- `BluetoothAdapter.bondedDevices` became the source of truth. Friendly names work,
  including OnePlus Buds 4, CMF Buds 2 Plus, Echo Dot and GameSir.
- Launcher one-row widgets provide less vertical space than the first previews assumed.
  They now use **two columns: artwork | grouped name/status**, at every width.
- Device identity wins over placeholder status. 2x2 is a first-class device tile.

## Current architecture

| Area | Source and responsibility |
| --- | --- |
| Device provider | `bluetooth/BondedDeviceProvider.kt`: permission/adapter/off/error states; one bonded snapshot, mapping and sorting. `BondedDevice.kt` normalizes/deduplicates addresses. |
| Classification | `DeviceKind.kt`: reliable specific class metadata first, conservative name fallback. Buds/headset -> headphones; GameSir/controller -> controller; ambiguous names -> generic. |
| Widget configuration | `WidgetConfigActivity` validates ownership of the widget ID; editor keeps pending device/style state; completed Save commits them together. |
| Appearance | `WidgetAppearance.kt`: stable enum names, preset defaults and visibility. `WidgetVisuals.kt`: palettes/surfaces/radii. |
| Persistence | `WidgetConfigStore.kt` + `SafePreferences.kt`: per-ID SharedPreferences, tolerant typed reads, ID restore/removal. |
| Artwork | `DeviceArtwork.kt`: per-MAC mapping, private PNG copies, built-in registry (empty), generic fallback, bounded decode and safe orphan cleanup. |
| Layout | `WidgetLayoutRules.kt`, `RowWidgetSpec.kt`, `RowTextMeasurement.kt`: shared size/font/visibility/measurement rules. |
| Glance | `BlueTapWidget.kt`: `SizeMode.Exact` / `LocalSize`, independent rendering; IO snapshot load; refresh token reloads cached state. |
| Preview | `ui/WidgetPreview.kt`: separate Compose rendering using the same rules and actual text measurement. Preview state is illustrative only. |
| Future connector | `BluetoothConnector` / `ConnectionState`; Android 17 placeholder is unwired and unchanged. Its legacy `AssociatedDevice` contract needs review before MAC-based integration. |

### Layout reference (dp, not launcher cell guarantees)

- Classification: Tiny if width <130 or height <58; otherwise height <125 selects
  Sleek below width 260, SleekWide at/above 260. Showcase requires width >=240 and
  height >=230. Wide requires width >=260 and width/height >=1.4. Otherwise Card.
- **Dedicated row override:** width >=110 and height <125 uses `RowWidgetSpec`, even
  if the base class is Tiny. Slim below height 72, Spacious otherwise. No third column.
- Placeholder status requires width >=220, height >=72 and room after the name.
  Known future state/battery gets higher priority, but still must fit.
- Names 14sp, secondary line 11sp; font-scale-aware height budget, font padding and
  4dp headroom. Shared measurement chooses one/two lines and balanced word breaks.
  Artwork nominal 36/44/52dp for widths <220/<320/otherwise, constrained by height
  and name fit. Padding and spacing are defined in the spec, not renderer branches.
- Preview rows are 160x56 and 260x64dp; other previews 170x170, 290x170 and 260x232.
  Launcher dimensions/font rendering still need phone tests; previews are examples.
- Dot Mono and OLED keep black/monochrome constraints. Hyper Glass uses a soft
  surface, Material You a circular artwork field; no actual blur or new presets.

### Persistence and migration contract

`widget_config` preferences use `widget_<id>_<field>`:

`mac_address`, `name`, `kind`, `preset`, `accent`, `background`, `custom_background`,
`show_label`, `show_name`, `show_status`, `alignment`, `compact`, `show_battery`,
`show_artwork`, `artwork_source`.

- Enum names are serialized, so do not rename casually. Unknown values/type mismatches
  fall back safely. Missing appearance is the default Blue Tap preset; new battery and
  artwork flags default true, with unknown battery omitted. Custom backgrounds are opaque.
- Device and appearance writes are independent; completed editor saves commit both
  together. Editing one widget does not mutate another. App theme preference is
  `app_settings/dynamic_color` (default false).
- Legacy `association_id` is ignored and removed on device save. Existing MAC/name
  fields survive. Missing MAC requires configuration; unmatched/invalid MAC remains
  unavailable until re-selected. No endpoint matching/migration heuristic.
- `onDeleted` removes only that widget's keys. `onRestored` remaps IDs from a snapshot,
  including overlapping pairs. Shared artwork is never removed by widget deletion.
- `device_artwork` preferences map normalized uppercase MAC -> owned UUID PNG filename
  under `filesDir/device_artwork`. No external URI permission is retained or needed.
- Artwork choice/removal is an **immediate shared-device edit**, explicitly described
  in the editor. Cancel rolls back pending widget settings, not these artwork operations.
  Preview size/background/demo state never changes the runtime Bluetooth state.

## Intentional limitations

- No implemented Android 17 connection control. `Ready` means the device is still
  bonded/readable, not connected. Runtime visual state is UNKNOWN or UNAVAILABLE.
- No generic public battery provider. `showBattery` persists but creates no empty
  battery slot without a value. Future integration point: supply `WidgetDeviceStatus`
  to the renderer from a verified public provider; `DeviceBattery` rejects invalid data.
- Future visual states exist: unavailable, disconnected, connecting, connected,
  disconnecting, unknown. Editor dots animate; real widgets use static dots without
  timers, alarms or WorkManager animation loops.
- **Never infer single-earbud state from audio channel configuration.** Full-device
  artwork is the default; dormant left/right variants require explicit reliable data.
- Fast Pair system artwork is not a usable normal-app source. Built-in product assets
  require app ownership/licensing; no proprietary imagery is bundled.
- CDM code is retained only for a future verified association requirement. It is not
  reached from the app picker, editor or widget renderer, and creates no association
  automatically. Keep its tests until that future decision is made.

## Pause audit changes

- Tolerant preference reads prevent malformed stored types from crashing app/widgets.
- Completed widget saves persist device/style together; launcher refresh failure no
  longer reports an already-committed save as unsaved. Deleted IDs are rechecked.
- Added launcher ID remapping; cancellation propagates instead of being mistaken for
  a storage failure. Artwork editing blocks Back/Save while the short IO operation runs.
- Artwork replacement/removal respects all mappings. Cleanup only removes owned,
  unmapped UUID PNGs older than 24h, under a shared import lock. Mapped/fresh/foreign
  files survive. Runtime decode rejects >4MiB files and samples images to <=384px.
- Glance disk/decode/Bluetooth snapshot work and main picker refresh run on IO.
  Refreshes use a monotonic token rather than same-millisecond timestamps.
- Classification now sees a usable alias when the platform name is blank; MAC
  deduplication trims whitespace before normalization.
- Toggle rows have labels, switch semantics and >=48dp targets. Decorative images
  no longer repeat visible device names. Save can grow with text; setup actions wrap.
  Preview Outline border opacity matches runtime. No preset/layout redesign.
- Removed ten unused strings; common actions/errors now use resources. No production
  address logs or diagnostic dumps remain. No source resource stubs are used.
- Widget receiver is non-exported; launcher/config activities remain exported as
  required, with widget-ID ownership checks. Backup/transfer excludes local addresses,
  settings and artwork. This intentionally requires setup after reinstall/new phone.
- Pinned app dependencies unchanged. Added Gradle distribution checksum, shell line
  endings, ignore rules, wrapper validation/CI report retention and Node 24 actions.

## Known issues / follow-ups

1. **Audit Android build, lint and device smoke checks are pending.** Sandbox cannot
   read the installed SDK. Do not treat source checks as an APK build.
2. No continuous Bluetooth/unpair/permission observer. App resume/Refresh, save/artwork
   edits and host update/resize refresh snapshots. Widgets may show stale availability
   until then. Add event-driven updates alongside real state reporting, not polling.
3. CDM exact-endpoint association and the connector's association-based interface are
   unresolved future integration decisions; never guess a randomized/classic mapping.
4. Native image codec failures/corruption, launcher restore, process-death timing,
   large text/TalkBack and OEM launchers require instrumentation/phone validation.
   JVM tests cover store/file/layout decisions, not Android codec or host behavior.
5. Mapped artwork is deliberately retained for devices no longer paired. Remove it
   explicitly; there is no global quota. Orphan cleanup runs on later artwork edits,
   not a background job. Corrupt custom files fall back visually until replaced/removed.
6. UI is English-only; model/preset labels still contain English strings. Do not claim
   full localization. Application ID/version are development placeholders, not a release.

### TODO inventory

The sole TODO is `TODO(api37)` in `Android17BluetoothConnector.kt`; its two
`NotImplementedError` results are intentional. No other TODO/FIXME/HACK/XXX remains
in production/test source. That file was not edited during this audit.

## Android 17 resume plan

1. Install an appropriate Android 17 SDK and obtain/update the real test device.
   Preserve the API 36 foundation baseline; change toolchain only after reviewing APIs.
2. Verify public `BluetoothDevice.connect()` / `disconnect()` availability, permission,
   association requirements, threading and error behavior from SDK docs and the phone.
3. Revisit the connector contract for saved bonded MACs, then implement
   `Android17BluetoothConnector`. If association is required, explicitly associate the
   exact selected device; do not revive the unrestricted CDM picker as primary setup.
4. Resolve a saved MAC via the public adapter API with permission/address/off checks.
5. Wire the full-widget tap action; retain an accessible editor route.
6. Observe real state (bonding is not connection), handle transitional/error states.
7. Update widgets after actual transitions without animation/polling update loops.
8. Replace the placeholder only when the real capability/state is available; maintain
   honest Android 12-16 behavior and unavailable-device handling.
9. Test OnePlus Buds 4 and CMF Buds 2 Plus, multiple widgets and rapid repeated taps.
10. Investigate legitimate battery APIs only after core connection/state behavior works.

## Product roadmap

Technical resume instructions and current implementation/validation status remain
in this document. Longer-term product direction, phased priorities, deferred ideas
and explicit non-goals live in [ROADMAP.md](ROADMAP.md). Roadmap features are planned,
not implemented or promised for a particular date.

## Important constraints

Public SDK only. No hidden Bluetooth APIs, `BLUETOOTH_PRIVILEGED`, reflection, root,
Shizuku, Accessibility automation, OEM protocols, private Fast Pair scraping, fake
connection/battery state or mono/stereo earbud inference. No backend/analytics,
Room/Hilt or speculative association cleanup. Remain local and small.

## Build commands

Toolchain: JDK 17; Gradle 8.14.3; AGP 8.13.0; Kotlin 2.2.20; Compose BOM 2025.09.00;
Activity Compose 1.10.1; Glance 1.1.1; JUnit 4.13.2. SDK min31/compile36/target36;
Build Tools 35.0.0. Application version 0.1.0 (1). No dependency sweep during audit.

Host PowerShell:

```powershell
Set-Location 'C:\Users\rifaq\Documents\Projects\blue-tap'
$env:ANDROID_HOME = 'C:\Users\rifaq\AppData\Local\Android\Sdk'
java -version # JDK 17; set JAVA_HOME to your installed JDK if needed.
.\gradlew.bat clean testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug
```

Do not commit `local.properties`, SDKs, build outputs, signing material or personal
device logs. `.gitignore` covers these; `gradlew` remains executable with LF endings.
CI uses the same tasks on Ubuntu with JDK 17, validates the wrapper, and uploads APK
and reports for 60 days. It has not been executed from this sandbox session.

## APK path

`C:\Users\rifaq\Documents\Projects\blue-tap\app\build\outputs\apk\debug\app-debug.apk`

An existing file at that path predates this audit. Install only after the host build
succeeds; no fresh APK was produced in the sandbox.

## Useful adb commands

```powershell
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
& $adb devices
& $adb shell getprop ro.build.version.sdk
& $adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $adb shell am start -n dev.bluetap.app/.ui.MainActivity
& $adb shell am start -a android.settings.BLUETOOTH_SETTINGS
& $adb logcat -b crash -d
$appPid = (& $adb shell pidof -s dev.bluetap.app).Trim()
if ($appPid) { & $adb logcat "--pid=$appPid" }
& $adb shell dumpsys appwidget # Inspect locally; redact IDs/device details before sharing.
```

There is no `BlueTapAssocDiag` tag anymore. Use process/crash logs; do not reintroduce
address dumps. Reboot testing may be done manually after saving widgets.

## Test status

Latest audit checks: **81/81 JVM tests passed** (74 existing + 7 regression tests),
using cached Kotlin 2.2.20/Compose compiler dependencies and Android mock classes.
All production/test Kotlin source compiled in that limited harness. AAPT2 resource
compilation, XML parsing, wrapper checksum and `git diff --check` passed.

These checks do not perform Android manifest/resource linking, dexing, packaging,
Android lint, image codec execution or launcher rendering. Temporary generated R
symbols used for source checking are ignored build artifacts, never source inputs.
Automatic approval review blocked deleting `app/build/source-verification` (reason:
"blocked by policy"). It remains ignored; the normal host Gradle `clean` removes it.
The installed SDK is sandbox-inaccessible; **the full Gradle build/lint is pending
host validation**. Record the host test result here before the pause commit.

New regressions: malformed appearance/identity/artwork preference types; atomic
device+appearance save independence; overlapping widget-ID restore; orphan cleanup
ownership/age rules; shared-file removal protection. Existing tests retain coverage
of names, classification, migrations, address independence, deletion, state, presets,
font scale, tight row heights and visibility priority.

### Files touched by this audit

Paths below are relative to the repository; this excludes unchanged files from the
earlier, still-uncommitted visual foundation.

- Root/build: `README.md`, `DEVELOPMENT_STATUS.md`, `.gitattributes`, `.gitignore`,
  `.github/workflows/android.yml`, `gradle/wrapper/gradle-wrapper.properties`.
- `app/src/main/AndroidManifest.xml`.
- Under `app/src/main/java/dev/bluetap/app/bluetooth/`: `AssociatedDevice.kt`,
  `BluetoothConnector.kt`, `BondedDevice.kt`, `BondedDeviceProvider.kt`.
- Under `app/src/main/java/dev/bluetap/app/ui/`: `DeviceSetupScreen.kt`,
  `MainActivity.kt`, `DeviceRow.kt`, `WidgetEditorScreen.kt`, `WidgetPreview.kt`.
- Under `app/src/main/java/dev/bluetap/app/widget/`: `SafePreferences.kt`,
  `WidgetConfigStore.kt`, `WidgetConfigActivity.kt`, `BlueTapWidgetReceiver.kt`,
  `BlueTapWidget.kt`, `DeviceArtwork.kt`, `WidgetLayoutRules.kt`.
- Under `app/src/main/res/`: `values/strings.xml`, `layout/widget_preview.xml`,
  `layout/widget_loading.xml`, `xml/data_extraction_rules.xml`.
- Under `app/src/test/java/dev/bluetap/app/`: `bluetooth/BondedDeviceTest.kt`,
  `widget/WidgetConfigStoreTest.kt`, `widget/WidgetAppearanceTest.kt`,
  `widget/DeviceArtworkTest.kt`.

Verification references: [Gradle release checksums](https://gradle.org/release-checksums/),
[setup-gradle wrapper validation](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md),
[Glance widget guidance](https://developer.android.com/develop/ui/compose/glance/create-app-widget),
[Android backup/transfer rules](https://developer.android.com/identity/data/autobackup).

## Resume checklist

- [ ] Before pausing: run host clean build/tests/lint; record results above.
- [ ] Install via `adb install -r` to verify existing widgets keep assignments/styles.
- [ ] Phone smoke: deny/regrant permission; Bluetooth off/on; unpair/re-pair and Refresh.
- [ ] Create/cancel/edit/delete widgets; verify other widgets and shared artwork survive.
- [ ] Import/replace/remove transparent, large and unsupported images; check fallback.
- [ ] All presets; 2x1/3x1/4x1/5x1 rows, 2x2/wide/large; long names and large font scale.
- [ ] Light/dark launcher and app, themed icon, narrow editor, TalkBack toggle labels.
- [ ] Restart app/launcher and reboot; verify persistence and graceful stale availability.
- [ ] Commit the complete foundation and audit files together (some prior work is
      untracked); do not create a production release/tag.
- [ ] On return: read this document, run baseline checks, then start resume plan step 1.
