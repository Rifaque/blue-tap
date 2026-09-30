package dev.bluetap.app.bluetooth

/** Specific class metadata wins; only unambiguous product words are used as a fallback. */
enum class DeviceKind(val label: String) {
    GENERIC("Bluetooth device"), HEADPHONES("Headphones"), SPEAKER("Audio device"),
    WATCH("Wearable"), CONTROLLER("Controller"), INPUT("Input device")
}

internal fun deviceKind(deviceClass: Int?, name: String? = null): DeviceKind = when (deviceClass) {
    0x0404, 0x0418 -> DeviceKind.HEADPHONES // Wearable headset, headphones
    0x0414, 0x041C, 0x0420, 0x0428 -> DeviceKind.SPEAKER
    0x0704 -> DeviceKind.WATCH
    0x0504, 0x0508, 0x0810 -> DeviceKind.CONTROLLER // Joystick, gamepad, toy controller
    0x0540, 0x0580, 0x05C0 -> DeviceKind.INPUT
    else -> nameDeviceKind(name)
}

internal fun nameDeviceKind(name: String?): DeviceKind {
    val words = name.orEmpty().lowercase(java.util.Locale.ROOT)
    fun matches(pattern: String) = Regex("\\b(?:$pattern)\\b").containsMatchIn(words)
    return when {
        matches("buds|earbuds|earphones|ear buds|headphones|headset") -> DeviceKind.HEADPHONES
        matches("speaker|soundbar") -> DeviceKind.SPEAKER
        matches("watch|band") -> DeviceKind.WATCH
        matches("controller|gamepad|gamesir") -> DeviceKind.CONTROLLER
        else -> DeviceKind.GENERIC
    }
}

/** Older widget records may predate classification. Preserve storage and improve their fallback. */
internal fun artworkKind(device: BondedDevice?): DeviceKind =
    device?.kind?.takeIf { it != DeviceKind.GENERIC } ?: nameDeviceKind(device?.name)
