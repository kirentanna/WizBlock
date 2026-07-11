# Google Play Listing Draft

## App Name

WizBlock

## Short Description

Free, open-source app and site blocker with no ads or internet access.

## Full Description

WizBlock is a privacy-first, free, GPL-3.0-only open-source focus blocker for
Android. It helps you block distracting apps, websites, and keywords using rules
that stay on your device.

WizBlock has no ads, no login, no analytics SDK, no cloud sync, and no internet
permission. Your blocklists, schedules, usage counters, and recent blocked
attempts are stored locally on your phone.

Features:

- Block selected apps.
- Block websites and keywords where browser URL text is available.
- Create named Blocklists that combine apps, websites, and keywords.
- Start from editable Games, Shopping, Social, and Video Blocklists.
- Use schedules and daily usage limits.
- Start quick focus sessions from the Home screen.
- Use Strict Mode when you want a timer that cannot be stopped early.
- Optionally enable Android Device Admin uninstall protection for Strict Mode.
- Review local daily stats and recent blocked attempts.

WizBlock asks for Android Accessibility and overlay permissions because local
blocking apps need them to detect active apps/browser URL text and show the
blocking screen. Accessibility-derived information is processed on device only
and is not transmitted. Device Admin is optional and used only to prevent normal
uninstall while Strict Mode uninstall protection is enabled.

WizBlock is GPL-3.0-only open source and completely free to use. The project is
built as a privacy tool, not an ad network, analytics product, or subscription
funnel.

## Feature Graphic Text Direction

Private, local app and website blocking.

Prepared asset:

- `docs/store-assets/feature-graphic.png` - 1024 x 500 Google Play feature graphic.

## Prepared Screenshots

- 1. `docs/screenshots/protection-running.png` - Home screen with protection running.
- 2. `docs/screenshots/blocklists.png` - Named Blocklists for Games, Shopping, Social, and Video.
- 3. `docs/screenshots/blocking-overlay.png` - Full-screen local blocking overlay for a blocked YouTube website visit.
- 4. `docs/screenshots/blocklist-detail-schedule.png` - Target detail sheet with schedule presets and day controls.
- 5. `docs/screenshots/strict-mode-lock-options.png` - Strict Mode lock and optional uninstall-protection controls.
- Optional GitHub/supporting screenshot: `docs/screenshots/timed-session.png` - Timed focus session controls.

## Screenshot Captions

- Keep protection running locally without accounts or cloud sync.
- Start from editable Blocklists for Games, Shopping, Social, and Video.
- See exactly why a blocked app or site was stopped.
- Fine-tune when each app, site, or keyword is blocked.
- Choose Strict Mode protections, including optional uninstall protection.

## Data Safety Draft

- Data collected: No.
- Data shared: No.
- Security practices: data stays on device; no account required; no network permission.
- Data deletion: users can remove rules/history by clearing app data or through app controls where available.

These answers must be verified against the final uploaded artifact before
submission.

## Sensitive Permission Declaration Copy

### Accessibility

WizBlock uses Accessibility to read the active app, visible browser URL text,
and window changes on this device so it can enforce the user's local blocklists.
This information is processed locally on the device and is not sent anywhere.

### Foreground Service

WizBlock uses a foreground service to keep protection reliable while the user has
enabled blocking. The ongoing notification makes the active protection state
visible to the user.

### Overlay

WizBlock uses overlay permission to show a blocking screen over apps and
browsers that match the user's local rules.

### Device Admin

WizBlock uses Android Device Admin only when the user explicitly enables
optional Strict Mode uninstall protection. It prevents normal uninstall while
Strict Mode uninstall protection is enabled. WizBlock does not request wipe,
password, camera, or lock-screen policies.
