# Google Play Review Notes

These notes are for the first Google Play submission. The owner approved the
release workflow on 2026-07-04.

## App Summary

WizBlock is a privacy-first Android focus blocker. It blocks selected apps,
websites, and keywords using local rules only. It has no ads, no account system,
no telemetry, no cloud sync, and no `android.permission.INTERNET`.

## Sensitive Permissions

### Accessibility Service

Purpose: detect the active app, visible browser URL text, and window changes so
the user's local blocklists can be enforced.

User disclosure: onboarding and permission status screens explain this before
opening Android Accessibility settings. The action label requires affirmative
acknowledgement: "I understand, open Accessibility settings."

Data handling: Accessibility-derived information is processed on device only and
is not transmitted.

Policy risk: Google Play treats Accessibility as a restricted capability. The
submission must include the Play Console declaration, in-app disclosure, and a
demo video showing the feature.

### Overlay Permission

Purpose: show a full-screen block screen over a distracting app or browser when
the local rules match.

User disclosure: onboarding explains that the overlay is used to display the
blocking screen.

### Device Admin

Purpose: optional uninstall protection for Strict Mode. WizBlock uses Device
Admin only to prevent normal uninstall while Strict Mode uninstall protection is
enabled.

Requested policies: none. The app does not request wipe, password, camera, or
lock-screen policies.

User disclosure: the Lock screen shows the Device Admin purpose before enabling,
and the app displays a confirmation dialog before launching Android's Device
Admin enable screen.

Policy risk: this is the highest-risk review area. The reviewer notes and demo
video must make clear that the feature is user-initiated, optional, limited to
Strict Mode, and disableable after the Strict Mode timer ends.

### Foreground Service

Purpose: keep protection reliable while enabled.

### Launcher App Query

Purpose: list installed launchable apps so the user can choose apps to block.
WizBlock does not request `QUERY_ALL_PACKAGES`.

## Data Safety Draft

- Data collected: No.
- Data shared: No.
- Security practices: data stays on device; no account required; no network permission.
- Data deletion: users can remove rules/history by clearing app data or through app controls where available.

Confirm these answers against the final release artifact before submission.

## Reviewer Demo Path

1. Install the release candidate.
2. Open WizBlock.
3. Show onboarding disclosure for Accessibility and overlay.
4. Enable Accessibility for WizBlock.
5. Enable "Appear on top" for WizBlock.
6. Add a test app or website to a blocklist.
7. Turn protection on.
8. Open the blocked target and show the blocking overlay.
9. Enable a timed Strict Mode session.
10. Open Rules, review Device Admin disclosure, and enable uninstall protection.
11. Show that Device Admin can be disabled after the Strict Mode timer ends.

## Required Console Assets

- Accessibility permission declaration.
- Prominent disclosure screenshot.
- Demo video showing Accessibility and overlay use.
- Privacy policy URL.
- Store listing copy from `docs/google-play-listing.md`.
- Data Safety form matching the final artifact.
- Screenshots showing app/site/keyword blocking, Strict Mode, and local privacy posture.
