# PauseZen

A minimal-permission, 100% offline Android clone of ScreenZen's mindful pause overlay for native apps and short-form video feeds.

## Features

- **App Groups**: Organize distracting apps and short-form feeds into custom groups (e.g., *Social Media*, *Reels & Shorts*, *Games*) with per-group pause timers, unlock grace periods, custom prompts, and master toggles.
- **In-App Short-Form Feed Detection**: Pause specific feeds without blocking the rest of the app—supports **Instagram Reels**, **YouTube Shorts** (including **YouTube ReVanced**), **Snapchat Spotlight**, **Snapchat Stories**, **Instagram Explore**, and **Facebook Reels**.
- **Daily Limits**: Set per-group **daily open limits** and **daily screen time limits** that reset automatically at midnight.
- **30-Day History & 7-Day Charts**: Tracks **Tries** (every attempt to open a guarded app/feed), **Unlocked opens** (after waiting out the pause timer), resisted count, and screen time across 30 days, with expandable 7-day per-app bar charts.
- **Customizable Breathing Guides**: Choose from 3 standalone visual animations (*Bloom*, *Lotus*, *Horizon*) and 4 breathing rhythms (*Calm*, *Box Breathing 4-4-4-4*, *4-7-8 Relax*, *Resonant 5-5*).

## Privacy & Permissions

- **Zero `INTERNET` permission** — enforced by Android's OS sandbox.
- No `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, or `QUERY_ALL_PACKAGES`.
- Uses a single `AccessibilityService` with gestures and screenshots disabled (`canPerformGestures="false"`, `canTakeScreenshot="false"`), inspecting window view IDs only when an enabled group targets an in-app feed and that app is in the foreground.

## Build & Install

Requires JDK 17 and Android SDK (API 35).

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install onto a USB-connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Android 13+ when sideloading manually (outside `adb`), enable **Settings → Apps → PauseZen → ⋮ → Allow restricted settings** before turning on the PauseZen Accessibility Service.
