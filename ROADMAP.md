# BlueTap Roadmap

BlueTap will remain a small, local-first Bluetooth device control utility. The
home-screen widget will remain the primary product surface. Future widgets, Quick
Settings, NFC and device-detail screens will share one Bluetooth state/action engine.
BlueTap will not become a full replacement for Android Bluetooth Settings.

This document records intended product direction, not a release promise. Phases have
no calendar dates; Android 17 platform behavior and real-device validation will
determine feasibility and timing. [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md)
contains the technical handoff, validation status and detailed resume instructions.

## Current baseline

The pre-Android-17 foundation is complete: Android 12+ bonded-device discovery,
friendly names/classification, per-widget assignment, responsive exact-size Glance
widgets, shared custom device artwork, generic fallbacks and editor previews.
The eight presets are Blue Tap, Dot Mono, Hyper Glass, Material You, OLED, Minimal,
Compact and Outline. `BluetoothAdapter.bondedDevices` is the source of truth.

Real connect/disconnect, live connection state, event-driven Bluetooth refresh and
generic battery reporting are **not implemented**. `Android17BluetoothConnector`
remains unwired and unimplemented. Widget taps currently open the editor; availability
may remain stale until a refresh. Current build/test limitations remain documented
in the development status rather than being resolved by this roadmap.

Automatic vendor/Fast Pair product artwork is unavailable through the current
legitimate public implementation. Actual product imagery is supplied manually by
the user; BlueTap does not claim automatic product-artwork detection.

## Phase 0 — Resume / API 37 validation

This will be the first work after the development pause, before feature implementation.

1. Install/update Android 17 / API 37 SDK tooling and test on a real Android 17 device.
2. Verify the actual public `BluetoothDevice.connect()` / `disconnect()` contract.
3. Verify permissions, association requirements, threading, errors, repeated calls,
   already-connected behavior and unavailable-device behavior.
4. Revisit the legacy association-oriented `BluetoothConnector` contract.
5. Decide how saved bonded MAC addresses will map cleanly into the API 37 implementation.
6. Keep bonded devices as the normal picker. Do not revive CDM as the normal picker;
   add a specific association step only if the verified API 37 contract requires it.

Exit criterion: a documented, tested platform contract and clear device/connector
mapping, with unresolved platform constraints identified before implementation starts.

## Phase 1 — Core connection engine

This will be the highest-priority implementation phase and the foundation for all
later action surfaces.

### Real connection control

The engine will implement connect, disconnect and toggle, followed by the full-widget
tap action. The default behavior will be Disconnected -> Connect and Connected ->
Disconnect. Bonding will never be treated as connection. Unknown/transitional states
will require explicit handling rather than an assumed toggle direction.

### Real connection state

The state model will support unavailable, disconnected, connecting, connected,
disconnecting, failure/timeout and unknown. The UI will replace "Android 17 required"
only when genuine capability/state is available, preserving truthful fallback behavior
on unsupported devices and Android versions.

### Timeout and failure handling

The implementation will define a bounded timeout and state machine after platform
validation. Connecting must never remain indefinitely transitional. A failed attempt
will leave Connecting, expose temporary failure feedback, restore a truthful final
state and allow retry. BlueTap will never manufacture success.

### Event-driven refresh

Platform events/state callbacks will update widgets when devices connect/disconnect,
including when the app is not open. This will also address the current stale-availability
limitation for permission, pairing and Bluetooth availability changes where supported.
Continuous polling will not be used.

### Haptics

Where Android permits it cleanly, user-triggered actions will receive subtle tactile
feedback for initiation, successful connection/disconnection and failure. Feedback
will be restrained, with no excessive vibration.

## Phase 2 — Quick Settings

A BlueTap Quick Settings tile will use the same core action/state engine as widgets.
The first version will control one user-selected device, reflect real state where
the APIs permit, perform the configured action on tap and restore its configuration
after app/process restart. It will not contain separate Bluetooth logic.

Multiple tiles will be considered only if Android's tile model makes them practical;
multi-tile support is not a commitment of the first version.

## Phase 3 — Device organization

After connection control is reliable, the app will gain focused device management.

### Favorites

The default list will emphasize Favorites. A collapsed or secondary **Show all devices**
list will keep new/other bonded devices accessible without requiring users to scroll
through every paired device to reach their regular BlueTap devices.

### Per-device nickname

A local BlueTap nickname, such as "Daily Buds" for "OnePlus Buds 4", will normally
appear in widgets when configured. It will not rename the actual Android Bluetooth
device. The original Bluetooth name will remain visible in device details.

### Device detail screen

A lightweight page will manage artwork, nickname, original Bluetooth name, real
connection state, battery if available, favorite state and widgets referencing the
device. It will include artwork replacement/removal and later NFC actions. It will
remain a BlueTap management screen rather than a Bluetooth Settings clone.

## Phase 4 — Connection handoff

Optional handoff will allow an action on OnePlus Buds 4 to disconnect a connected
CMF Buds 2 Plus, connect the selected target and update both devices/widgets with
appropriate haptic feedback. A setting such as **Switch audio device automatically**
will make this behavior opt-in/configurable; BlueTap will not assume another device
should always be disconnected.

Handoff will be orchestrated by the common engine, not widget-specific logic. If
disconnect succeeds but the target connection fails, BlueTap will expose the actual
resulting states rather than pretending the handoff succeeded.

## Phase 5 — Multi-device widgets

The first target will be a 3x2-style widget, selected by real available dp dimensions
rather than guaranteed launcher cells. The initial concept will contain two devices,
with a possible third only if the composition remains clean.

Each device region will be independently tappable and have its own live state.
Device-scoped artwork and nicknames will apply automatically; users will be able to
reorder devices. Handoff rules will integrate through the same engine. Known battery
may appear beside state, but absent readings will remain omitted.

The composition will remain a device widget, not a Bluetooth dashboard. Other sizes
are deliberately uncommitted until this first multi-device layout is validated.

## Phase 6 — Battery (conditional)

Battery support will be implemented only if a legitimate, sufficiently reliable
public Android API/data source becomes available. It could supply aggregate percentage,
low-battery indication, widget text, device-detail readings and multi-device readings.
The existing Show battery setting and battery data model will be reused.

Left/right/case percentages will be allowed only when reliable public data explicitly
provides those readings. BlueTap will never invent values, use hidden APIs or
system-only broadcasts/accessors, scrape OEM/Fast Pair internals, or infer battery
from unrelated signals. If no legitimate generic source exists, battery will remain
unsupported. This phase is conditional and will not block later unrelated phases.

## Phase 7 — NFC actions

Native, opt-in NFC support will live inside BlueTap, using a flow such as Device detail
-> NFC actions -> Write NFC tag. Possible actions will be Connect, Disconnect, Toggle
and Handoff to this device.

Tags will use a stable BlueTap action/device identity format, never launcher widget
IDs, which are local and ephemeral. Scanning a tag will resolve device configuration
and route through the same engine as widgets and Quick Settings.

The design will handle missing devices, Bluetooth off, missing permission, device
remapping after import and failed connection attempts. NFC will remain local and
will not require a separate companion app.

## Phase 8 — Configuration import / export

Current platform backup/device-transfer exclusions will remain unless deliberately
changed later. Instead, explicit user-controlled files will support a future flow:
configure on phone -> export package -> import on tablet -> match/remap paired devices
-> restore device metadata/artwork/settings -> place widgets from restored templates.

The package will eventually support nicknames, favorites, custom artwork and mappings,
appearance settings, multi-device widget configurations/templates and relevant app
settings. Artwork will travel inside the package. The format will be versioned for
future migration; no format is implemented or fixed by this document.

AppWidget IDs and launcher placement will not be treated as portable configuration.
Matching will not blindly depend on MAC addresses. It will use deterministic matching
where safe and ask users to resolve ambiguity, never silently pairing unrelated devices
because names look similar. Export/import will not require a BlueTap account/server.

## Deferred — Artwork states

Connected/disconnected/charging artwork and both-earbuds/left-only/right-only variants
will remain deferred until trustworthy data sources exist. Current users manually
supply product images; automatic vendor/Fast Pair artwork is not available through
the current legitimate public implementation.

State/data correctness will come first; visual variants will follow later. Single-earbud
state will never be inferred from mono/stereo audio behavior.

## Explicitly not planned

### Persistent quick-action notification

Not planned. Widgets, Quick Settings and NFC will cover the intended quick-access use case.

### Hidden/private Bluetooth APIs

Never planned under the project's public-API-only philosophy: reflection, root,
Shizuku, Accessibility automation, `BLUETOOTH_PRIVILEGED`, private Fast Pair APIs or
OEM-specific Bluetooth control protocols. Any future change to that philosophy would
require an explicit product decision; this roadmap does not authorize an exception.

### Backend / cloud account

Not planned. BlueTap will remain local-first; configuration portability will use
user-controlled files rather than a BlueTap account/server.

## Architecture principle

Future functionality will converge on one internal device state/action engine:

```text
Bluetooth device state/action layer
    +-- Home-screen widgets
    +-- Multi-device widgets
    +-- Quick Settings tile
    +-- Device detail screen
    +-- NFC actions
```

The engine will own state, connect, disconnect, toggle, timeout, failure and handoff.
Presentation layers will consume it. No UI surface will have its own Bluetooth
implementation. This principle will guide the Android 17 phase; it does not imply
the common engine is implemented today.

## Suggested priority order

Priorities describe intended sequencing, not delivery dates. Phase numbers above
describe work areas; the P-levels below describe relative product priority.

| Priority | Intended work |
| --- | --- |
| P0 | API 37 verification |
| P1 | Real connect/disconnect |
| P1 | Real connection state |
| P1 | Event-driven refresh |
| P1 | Timeout/failure state |
| P1 | Restrained haptics |
| P2 | Quick Settings tile |
| P2 | Favorites + Show all |
| P2 | Nicknames |
| P2 | Device detail screen |
| P3 | Opt-in connection handoff |
| P3 | Multi-device 3x2-style widget |
| P3 | Battery, only with a legitimate public source |
| P4 | NFC actions |
| P4 | Configuration export/import |
| Deferred | Artwork state variants |
| Not planned | Persistent quick-action notification |

Implementation will start with verified platform behavior, then dependable connection
and state handling, before expanding to additional action surfaces.
