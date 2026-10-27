# Quick Notes — Native Android app

A Kotlin + Jetpack Compose notes app. It does not require an account, server, or internet permission.

## Features
- Create and edit notes
- Delete notes
- Search note titles and content
- Pin/unpin notes
- Light/dark appearance toggle
- Local persistence using Android SharedPreferences
- Installable debug APK built with Gradle or GitHub Actions

## Build in Termux
Requires Java 17+ and Android SDK platform/build-tools 36 installed. If your SDK is at `$HOME/android-sdk`:

```sh
cd ~/QuickNotesAndroid
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
gradle assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

To install from Termux (after enabling Android package install permission):
```sh
termux-open app/build/outputs/apk/debug/app-debug.apk
```

If you have not installed platform 35, use sdkmanager:
```sh
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-36" "build-tools;36.0.0"
```
Your SDK may have a different cmdline-tools path; locate `sdkmanager` if needed.

## Build APK with GitHub Actions
1. Upload this project to a GitHub repository.
2. Open **Actions** → **Build Android APK** → **Run workflow**.
3. Open the completed workflow run and download the `QuickNotes-debug-apk` artifact.
4. Extract it to get `app-debug.apk`.

The APK is a debug build, suitable for personal installation/testing. This is a starter project, not a Play Store-signed release.
