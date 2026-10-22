# Jafurah Emergency Alert — Android prototype

**Status: source project; the APK must be built and tested before use.** This is a secondary convenience alert, not an approved emergency-warning system.

## Configured target
- WhatsApp group filter default: `SUBCON Management Emergency Group`
- Activation variants include `Shelter-in-place is activated` and `Shelter-in-place activated`
- Deactivation variants include `Shelter-in-place is deactivated` and `Shelter-in-place deactivated`
- Intended response: loud alarm, vibration, spoken announcement, red/green alert screen, and local alert history

## Build an APK without installing Android Studio locally
This project includes a GitHub Actions workflow that builds a **debug APK** in GitHub's build environment.

1. Create a private repository on GitHub (recommended because the project concerns a workplace emergency).
2. Upload the contents of this folder to the repository root. Make sure `app/`, `build.gradle`, `settings.gradle`, `gradle.properties`, and `.github/workflows/build-apk.yml` are at the repository root.
3. In GitHub, open **Actions** and enable workflows if prompted.
4. Run **Build Android APK** using **Run workflow**, or push a commit to the `main` branch.
5. When the run completes, open the run and download the artifact named `Jafurah-Emergency-Alert-debug-APK`. It contains `app-debug.apk`.
6. Transfer the APK to your Samsung phone, install it, then complete the permissions and testing below.

## First-run setup on Samsung
1. Open the app and allow notifications if prompted.
2. Tap **ENABLE WHATSAPP NOTIFICATION ACCESS** and enable this app in Android's notification-access settings.
3. Confirm the group filter is `SUBCON Management Emergency Group`, then save.
4. In Settings > Apps > Jafurah Emergency Alert, allow notifications. Check sound, lock-screen visibility, and any full-screen alert permission available on your Android version.
5. In Settings > Battery > Background usage limits, add the app to **Never sleeping apps** if available. Use unrestricted battery usage if your phone offers it.
6. Test only with a harmless test message in a separate test group; do not send fake emergency orders to the live site group.
7. Validate with the screen locked, WhatsApp backgrounded, after reboot, and with relevant sound/Do Not Disturb settings. Do not rely on it until the HSE/emergency team approves controlled testing.

## Important technical limitations
- The app does not access WhatsApp's private database. It only inspects text Android exposes in WhatsApp notifications.
- The group filter works only if the group name appears in the visible notification title/text. During first testing, if it misses the target group, inspect a notification safely and adjust the filter; do not disable filtering for operational use without understanding the false-alert risk.
- Android/Samsung may suppress notification text, restrict background activity, or block a full-screen launch. Notification access does not guarantee every message will be detected or every alarm will sound.
- Android notification-listener access is sensitive and can expose notification content. Keep the app private and process messages only on-device.
- The workflow creates a **debug APK**, not a Play Store release. It has not been built or field-tested in this environment.
- Always follow site alarms, official emergency channels, and the approved shelter-in-place instructions. This app must not replace them.
