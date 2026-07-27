# Cloudflare Manager for Android

[![Build APK](https://github.com/hussain933/cloudflare-manager-android/actions/workflows/build-apk.yml/badge.svg)](https://github.com/hussain933/cloudflare-manager-android/actions/workflows/build-apk.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API_24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-see_project_terms-111827)](#legal-and-privacy)

Cloudflare Manager is a native Android application for creating a temporary Cloudflare Tunnel from a local service on an Android device. It bundles the `cloudflared` executable, serves an offline control interface locally, and keeps the tunnel lifecycle in a foreground service.

## Product Overview

The application is designed around a simple, reliable flow:

1. Accept the terms on first launch.
2. Choose the local service port.
3. Start the local control server on loopback.
4. Launch `cloudflared` against the selected local service.
5. Display the public `trycloudflare.com` URL in the local WebView interface.

The local web interface runs from bundled assets and does not require a separate web server, account, or first-run download.

## Features

- Native Kotlin Android application.
- Bundled `cloudflared` binaries for `arm64-v8a` and `armeabi-v7a`.
- Local-only NanoHTTPD server bound to `127.0.0.1`.
- Offline HTML, CSS, and JavaScript control interface.
- Foreground service for tunnel lifecycle management.
- Live tunnel status and public URL detection from process logs.
- Copy-link action and native Android notifications.
- Optional wake lock while the tunnel is running.
- Background checks for newer `cloudflared` releases.
- Android 13 notification permission handling.
- Terms of Service and Privacy Policy screens included in the app.

## Architecture

| Component | Responsibility |
| --- | --- |
| `WelcomeActivity` | First-run terms agreement |
| `PortSetupActivity` | Local port selection and validation |
| `LoaderActivity` | Binary installation and local server startup |
| `WebViewActivity` | Hosts the local control interface |
| `LocalWebServer` | Serves `assets/www` on loopback only |
| `TunnelService` | Owns the foreground tunnel process |
| `CloudflaredRunner` | Starts and stops the bundled executable |
| `LogParser` | Reads output and extracts the public URL |
| `CloudflaredUpdater` | Checks and installs compatible updates |
| `JsBridge` | Connects the local web interface to native code |
| `PreferencesManager` | Stores local app preferences |

## Project Structure

```text
cloudflare-manager-android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── assets/
│       │   ├── cloudflared/
│       │   │   ├── arm64-v8a/cloudflared
│       │   │   └── armeabi-v7a/cloudflared
│       │   └── www/
│       ├── kotlin/com/example/cloudflaremanager/
│       ├── res/
│       └── AndroidManifest.xml
├── build.gradle
├── gradle.properties
├── gradlew
└── settings.gradle
```

## Requirements

- Android Studio Ladybug or newer.
- JDK 17 for Android builds.
- Android SDK with API 34 installed.
- Android device or emulator running Android API 24 or newer.

## Build Locally

```bash
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build an unsigned release APK with:

```bash
./gradlew assembleRelease
```

The release APK is generated at:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

## Continuous Integration

Every push to `main`, pull request targeting `main`, and manual workflow dispatch runs:

- Debug APK compilation.
- Debug APK artifact upload.
- Unsigned release APK compilation.
- Release APK artifact upload.

Open the [Build APK workflow](https://github.com/hussain933/cloudflare-manager-android/actions/workflows/build-apk.yml) to view current runs and download generated artifacts.

## Release Signing

The CI workflow intentionally produces an unsigned release APK. A production release should be signed with a private keystore stored in GitHub Actions secrets or another secure release system.

Never commit any of the following to the repository:

- Keystores.
- Signing passwords.
- API tokens.
- Private certificates.
- Local machine configuration.

## Security and Privacy

- The local control server binds to `127.0.0.1`; it is not intended to be reachable from the local network.
- The app stores settings and logs in Android app-private storage.
- Tunnel traffic is handled by `cloudflared` and Cloudflare's infrastructure.
- Update checks use the public Cloudflare Releases API.
- Users are responsible for the services they expose and for compliance with applicable laws and Cloudflare terms.

See the in-app [Privacy Policy](app/src/main/assets/www/privacy.html) and [Terms of Service](app/src/main/assets/www/terms.html).

## Verification Checklist

- First launch shows the terms screen; later launches can skip it.
- Port input accepts valid values from `1024` through `65535`.
- The local server responds on the loopback `/ping` endpoint.
- The WebView loads without browser chrome.
- Tunnel state transitions from Offline to Starting to Online.
- The public URL can be copied from the interface.
- Stopping the tunnel clears the URL and removes its notification.
- Notification actions support Exit and Awake to Lock.
- Settings persist after relaunch.
- Android 13 or newer requests notification permission on a fresh install.

## Legal and Privacy

Cloudflare Manager is an independent project and is not affiliated with or endorsed by Cloudflare, Inc. `cloudflared` is maintained by Cloudflare and remains subject to its own license and terms.

---

Built with Kotlin, Gradle, AndroidX, NanoHTTPD, OkHttp, and Kotlin Coroutines.