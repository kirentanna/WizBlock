# WizBlock Privacy Policy

Last updated: 2026-07-04

WizBlock is a local-only Android focus blocker. It does not have accounts, ads,
analytics, telemetry, cloud sync, remote rule downloads, or
`android.permission.INTERNET`.

## Data WizBlock Stores

WizBlock stores these items locally on the device:

- Blocked apps, websites, keywords, category choices, schedules, profiles, and usage limits.
- Strict Mode and protection settings.
- Local usage counters and recent blocked-attempt history.

This data is stored with Android local storage such as Room and DataStore. It is
not sent to WizBlock servers because WizBlock does not operate servers and does
not request internet access.

## Sensitive Permissions

WizBlock asks for powerful Android permissions because Android blockers cannot
work reliably without them:

- Accessibility: reads the active app, visible browser URL text, and window changes so local block rules can be enforced.
- Appear on top: shows the blocking screen over distracting apps and browsers.
- Device Admin: optionally prevents normal uninstall while Strict Mode uninstall protection is enabled.
- Foreground service: keeps protection reliable while enabled.

Device Admin is optional. WizBlock does not request wipe, password, camera, or
lock-screen Device Admin policies.

## Data Sharing

WizBlock does not collect, sell, share, or transmit personal data. It has no
third-party ad SDKs, analytics SDKs, or crash-reporting SDKs.

## Backups

Android backup is disabled for the app so local blocking data is not copied by
Android app backup.

## Contact

For privacy or security questions, use the GitHub issue/contact channel provided
with the public release.
