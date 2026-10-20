---
name: android-tv-custom-launcher
description: Build, install, customize, or troubleshoot a native Android TV or Fire TV launcher over ADB, with reversible device changes and on-device screenshot verification.
metadata:
  short-description: Build and install Android TV launchers
---

# Android TV custom launcher workflow

Use this skill when the user wants a custom launcher built or installed on an Android TV or Fire TV device. Adapt to the device's Android version, vendor restrictions, and the user's stated visual and navigation preferences.

## Protect the device and user choices

- Confirm the target device and connection before acting. Discover the IP from available network/device evidence, or ask the user when needed. For wireless ADB, use port 5555 only if the device supports it and debugging is enabled. Tell the user when the trust/authorization prompt appears so they can approve it.
- Before changing packages or settings, save a dated backup containing package lists, disabled-package lists, `getprop`, relevant settings, and a baseline TV screenshot. Keep ADB keys, device backups, Wi-Fi identifiers, IPs, package dumps, and device-specific scripts private; never add them to a public source release.
- Prefer reversible package disablement to removal. Do not uninstall system packages. Classify packages and ask which features/apps must remain before changing streaming, voice assistant, casting, screen mirroring, or firmware update services. Use one `pm` command per shell call on devices where chained commands may stop silently.
- Ask before making preference-dependent UI choices, including pinned apps/order, visual style, or which preinstalled features to disable. Carry forward explicit choices already given in this conversation.
- Write a restore script that re-enables every package this task disables. Keep the backup and restore artifacts local to the user unless they explicitly request sharing.

## Choose the launcher integration

First inspect the device's HOME behavior and vendor policy. Do not assume Android's default HOME resolver can replace a protected Fire TV launcher. Explain the difference between a normal HOME activity, a selectable HOME app, and an Accessibility-based Home-button redirect. If a helper app is needed, disclose its role, permissions, and independent installation/configuration. Preserve a route back to the stock launcher and test that switching both directions works.

## Build and install

- Choose the smallest native UI and dependency set that fits the target OS and hardware. Build a debug APK unless the user requests release signing. Record the build command, package ID, min SDK, and output path.
- Keep installation repeatable with a build-and-install script. Avoid background polling, heavy blur, video, or memory-heavy effects on low-memory TVs unless the user requests them.
- Request only permissions the selected features require and explain why. Keep network display optional if the OS restricts access to the Wi-Fi name.
- Never change the default launcher, disable the stock launcher, grant privileged settings permissions, or disable device packages without the user authorizing that operation. For `WRITE_SECURE_SETTINGS`, explain the exact behavior it enables before granting it.

## Verify on the real TV

After each meaningful device change, inject remote key presses over ADB, capture a screenshot pulled from the TV, inspect it, and fix issues before continuing. Verify navigation edges, app launch behavior, launcher switching, and recovery to stock home. Distinguish an actual TV screenshot from a mockup or preview. If device access is unavailable, stop live-device steps and report what remains unverified.

If macOS stock `adb` reports `No route to host` despite the port being reachable, treat Local Network privacy as a likely cause instead of repeatedly debugging the binary; use Python `adb-shell` in a venv if available. If large ADB push operations hang, transfer base64 chunks through the shell and verify the resulting MD5 before installing.

## Finish with usable artifacts

Provide:

- the launcher source and build/install script;
- a local restore script and instructions to re-enable a single app;
- a README describing the design, build, install, required permissions, vendor limitations, customization, and recovery;
- selected TV screenshots only after removing Wi-Fi SSIDs or other personal details from public copies;
- the exact file list before publishing to a remote repository.

Publishing files to a public repository is a separate action. Prepare and review the file list first, exclude backups/credentials/device-private artifacts, and get clear confirmation for the specific set before updating a public branch.
