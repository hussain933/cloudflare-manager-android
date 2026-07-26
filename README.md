# Cloudflare Manager — Android App

A native Android app that runs `cloudflared` on your phone to expose any local service through a Cloudflare Tunnel — with a clean offline HTML/CSS/JS UI inside a WebView.

---

## Project Structure

```
cloudflare-manager-android/
├── build.gradle             ← Root Gradle config
├── settings.gradle
├── gradle.properties
└── app/
    ├── build.gradle          ← App dependencies (edit package name here)
    └── src/main/
        ├── AndroidManifest.xml
        ├── kotlin/com/example/cloudflaremanager/
        │   ├── CloudflareManagerApp.kt       App class + notification channel
        │   ├── WelcomeActivity.kt            First-run terms screen
        │   ├── PortSetupActivity.kt          Local server port selection
        │   ├── LoaderActivity.kt             Binary extraction + server boot
        │   ├── WebViewActivity.kt            Main WebView host
        │   ├── SimpleWebActivity.kt          Terms/Privacy viewer
        │   ├── JsBridge.kt                   JS ↔ Kotlin bridge (@JavascriptInterface)
        │   ├── TunnelService.kt              Foreground service (cloudflared lifecycle)
        │   ├── CloudflaredRunner.kt          ProcessBuilder wrapper for cloudflared
        │   ├── CloudflaredInstaller.kt       First-run binary extraction from assets
        │   ├── CloudflaredUpdater.kt         Auto-update via GitHub Releases API
        │   ├── LogParser.kt                  Real-time log tailing + URL regex
        │   ├── LocalWebServer.kt             NanoHTTPD serving assets/www/
        │   ├── NotificationHelper.kt         Builds the foreground notification
        │   ├── NotificationActionReceiver.kt Exit / Awake to Lock button handlers
        │   ├── PreferencesManager.kt         SharedPreferences wrapper (all settings)
        │   ├── WakeLockManager.kt            PARTIAL_WAKE_LOCK acquire/release
        │   ├── TunnelStateHolder.kt          In-memory tunnel state (thread-safe)
        │   ├── VersionStore.kt               cloudflared version.json read/write
        │   └── AppInfoProvider.kt            Version info for About tab
        ├── assets/
        │   ├── www/                          ← Offline website (served by NanoHTTPD)
        │   │   ├── index.html               Main SPA (Tunnel / Settings / About)
        │   │   ├── terms.html
        │   │   ├── privacy.html
        │   │   ├── css/style.css
        │   │   └── js/
        │   │       ├── bridge.js            Native bridge wrapper
        │   │       └── app.js               App logic + event handlers
        │   └── cloudflared/                  ← YOU MUST ADD THESE BINARIES
        │       ├── arm64-v8a/cloudflared
        │       └── armeabi-v7a/cloudflared
        └── res/
            ├── layout/  (5 layout XML files)
            ├── drawable/ (input_bg.xml)
            └── values/  (colors, strings, themes)
```

---

## Setup Steps (Android Studio)

### 1. Open the project

Open `cloudflare-manager-android/` as an existing Android project in Android Studio.

### 2. Download the cloudflared binaries (REQUIRED)

The app requires the actual `cloudflared` binary bundled inside the APK.

1. Go to: https://github.com/cloudflare/cloudflared/releases/latest
2. Download:
   - `cloudflared-android-arm64` → save as `app/src/main/assets/cloudflared/arm64-v8a/cloudflared`
   - `cloudflared-android-arm`   → save as `app/src/main/assets/cloudflared/armeabi-v7a/cloudflared`
3. These files have **no extension** — do not rename them.
4. Update `CLOUDFLARED_BUNDLED_VERSION` in `app/build.gradle` to match the release tag (e.g. `"2024.12.0"`).

### 3. Change the package name (optional)

In `app/build.gradle`, change:
```gradle
applicationId "com.example.cloudflaremanager"
namespace   'com.example.cloudflaremanager'
```
And rename the Kotlin package folders accordingly in Android Studio  
(Right-click package → Refactor → Rename).

### 4. Add a notification icon

Replace the placeholder `android.R.drawable.ic_dialog_info` in `NotificationHelper.kt` with your own:
1. Add a monochrome vector drawable `res/drawable/ic_cloud.xml`
2. Change `setSmallIcon(android.R.drawable.ic_dialog_info)` to `setSmallIcon(R.drawable.ic_cloud)`

### 5. Build and run

```
Build → Make Project
Run → Run 'app'
```

Or build a release APK:  
`Build → Generate Signed Bundle / APK → APK → Release`

---

## How It Works (Quick Reference)

| Layer | What it does |
|---|---|
| **WelcomeActivity** | One-time terms agreement; skipped on subsequent launches |
| **PortSetupActivity** | User sets the local server port (default 60000) |
| **LoaderActivity** | Extracts cloudflared binary, boots NanoHTTPD, waits for /ping |
| **WebViewActivity** | Loads `http://127.0.0.1:<port>/index.html` in a chrome-less WebView |
| **LocalWebServer** | Serves `assets/www/` on loopback; never exposed to network |
| **JsBridge** | Exposes `window.AndroidBridge.*` methods to the website |
| **TunnelService** | Foreground service; runs cloudflared, owns notification + wake lock |
| **CloudflaredRunner** | Starts/stops the cloudflared process via ProcessBuilder |
| **LogParser** | Tails the log file every 300ms; extracts `*.trycloudflare.com` URL via regex |
| **CloudflaredUpdater** | Checks GitHub Releases API once per 24h; atomically swaps binary if newer |

---

## Key Design Rules (from the blueprint)

- ❌ The word **"Dashboard"** must never appear anywhere in code, UI, or filenames. Use **"Home"** instead.
- ✅ The cloudflared binary ships **inside the APK** — no first-run download required.
- ✅ Updates happen **silently in the background** — no app update needed for new cloudflared versions.
- ✅ Local server binds strictly to **127.0.0.1** (loopback only) — never reachable from outside the device.
- ✅ Bottom nav always shows exactly **Tunnel / Settings / About**.
- ✅ Notification always shows **Tunnel Running / Port / Exit / Awake to Lock**.

---

## Testing Checklist

- [ ] First launch shows Welcome screen; subsequent launches skip it
- [ ] Port validation rejects letters and out-of-range values
- [ ] Loading spinner animates while server boots
- [ ] WebView loads with no browser chrome (no address bar, no zoom, no selection popup)
- [ ] Start → Status transitions: Offline → Starting → Online
- [ ] Public URL appears and copy button works (native toast: ✓ Link Copied)
- [ ] Stop → Status resets to Offline; URL clears; notification removed
- [ ] Notification: Exit stops everything; Awake to Lock toggles wake lock
- [ ] Settings: theme switch applies instantly and persists across relaunch
- [ ] About: version fields correct; Check for Update triggers a real network call
- [ ] Test on Android 13+: notification permission prompt appears on fresh install
- [ ] Test on an OEM skin (Xiaomi/MIUI): foreground service survives backgrounding
