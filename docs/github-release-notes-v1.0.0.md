# WizBlock 1.0.0

WizBlock is a free, GPL-3.0-only Android focus blocker for apps, websites,
keywords, schedules, usage limits, and Strict Mode sessions.

## Highlights

- No ads.
- Free to use.
- No login.
- No analytics SDK.
- No cloud sync.
- No `android.permission.INTERNET`.
- Local rules, schedules, usage counters, and block history.
- Editable starter Blocklists for Games, Shopping, Social, and Video.
- Optional Android Device Admin uninstall protection for Strict Mode.

## Android Permissions

WizBlock uses Android Accessibility to detect the active app, visible browser URL
text, and window changes so local block rules can be enforced. It uses overlay
permission to show the blocking screen. Optional Device Admin is used only for
Strict Mode uninstall protection.

Accessibility-derived information is processed on device only and is not
transmitted.

## Release Artifact

The GitHub Release includes a signed Android APK for direct installation. A
SHA-256 checksum is provided as a separate release asset.

Google Play AAB:

```text
app-release.aab
SHA-256: 2823EC01853451C26F51FF7CCBE8AEB325B21BA1BC0243FD12EB534C5E912B6A
```

The AAB is signed for Play upload and is not committed to the repository.

## Installation Note

The GitHub APK and Google Play build may use different signing certificates
when Google Play App Signing is enabled. If so, Android will not treat them as
the same upgrade path. Use one distribution channel consistently for automatic
updates.
