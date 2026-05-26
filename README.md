# HideDevOptions

`HideDevOptions` is a small LSPosed/Xposed module for Android that hooks system settings reads and makes target apps see Developer Options and ADB as disabled.

## What it does

- Hooks `Settings.Global.getInt(...)`
- Hooks `Settings.Secure.getInt(...)`
- Hooks `Settings.Global.getString(...)`
- Hooks `Settings.Secure.getString(...)`
- Returns `0` or `"0"` for:
  - `development_settings_enabled`
  - `adb_enabled`

## Project structure

- `app/src/main/java/com/example/hideadb/MainHook.java`: Xposed hook entry point
- `app/src/main/resources/META-INF/xposed/`: Xposed module metadata
- `app/src/main/res/values/arrays.xml`: default LSPosed scope config

## Requirements

- Android SDK
- JDK 11
- Gradle wrapper included in this repo
- LSPosed or another compatible modern Xposed framework

## Build

```bash
./gradlew assembleRelease
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleRelease
```

The unsigned release APK will be generated under:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

## Usage

1. Build the release APK.
2. Install it in an environment with LSPosed.
3. Enable the module in LSPosed Manager.
4. Select the target apps in module scope.
5. Reboot or restart the target apps.

## Notes

- The module uses an empty default scope, so target apps must be selected manually.
- `local.properties` and build outputs are intentionally excluded from version control.

## License

No license file has been added yet. Add one before wider public distribution if needed.
