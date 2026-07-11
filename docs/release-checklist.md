# WizBlock Release Checklist

The owner approved the first public GPL-3.0 release workflow on 2026-07-04,
including a fresh one-commit public GitHub history.

## Pre-Release Source Check

- Confirm the worktree is clean on the intended release branch.
- Confirm `applicationId = "com.wizblock"` is still the intended first-upload package name.
- Confirm `versionCode = 1` and `versionName = "1.0.0"` are still correct.
- Confirm `LICENSE` is GPL-3.0-only.
- Confirm `README.md`, `docs/PRIVACY.md`, `docs/google-play-listing.md`, and `docs/google-play-review-notes.md` match the final app behavior.

## Verification Commands

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:bundleRelease --no-daemon
```

## Artifact Checks

- Inspect the merged release manifest and confirm there is no `android.permission.INTERNET`.
- Confirm expected sensitive capabilities only:
  - AccessibilityService with `BIND_ACCESSIBILITY_SERVICE`.
  - `SYSTEM_ALERT_WINDOW`.
  - Device Admin receiver with `BIND_DEVICE_ADMIN`.
  - Foreground service.
  - Launcher app query.
- Confirm Android backup remains disabled.
- Generate and record checksums for public artifacts.

Example checksum command:

```powershell
Get-FileHash app\build\outputs\bundle\release\app-release.aab -Algorithm SHA256
```

## Release Signing

Create an untracked `keystore.properties` file only on the release machine:

```properties
storeFile=C:/absolute/path/to/wizblock-release.jks
storePassword=replace-with-local-secret
keyAlias=wizblock
keyPassword=replace-with-local-secret
```

Never commit keystores, passwords, or `keystore.properties`.

## Google Play Preparation

- Create Play Console app entry.
- Complete App content forms:
  - Data Safety: no data collected/shared, verified against final code and manifest.
  - Privacy policy: use the hosted version of `docs/PRIVACY.md`.
  - Sensitive permissions declarations for Accessibility and overlay-related behavior.
  - Store listing copy from `docs/google-play-listing.md`.
- Upload Android App Bundle from `:app:bundleRelease`.
- Provide reviewer notes from `docs/google-play-review-notes.md`.
- Upload screenshots and a demo video showing Accessibility, overlay blocking, and Strict Mode Device Admin disclosure.
- Use internal testing first. Do not promote to production without owner approval.

## GitHub Public Release Preparation

- Prepare a clean public branch with one final release commit because development history should not be exposed.
- Include source code, GPL-3.0-only license, privacy policy, and build instructions.
- Attach release artifacts only after signing and checksum verification.
- Release notes should state:
  - No ads, no login, no analytics, no internet permission.
  - Local-only storage.
  - Sensitive permissions and why they are required.
  - SHA-256 checksum for attached artifacts.
