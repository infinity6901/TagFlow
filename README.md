# TagFlow

A Material 3 / Material You NFC toolkit for Android — read, write, automate and protect NFC tags (built around NTAG213, works with 215/216 too).

## Build
1. Open this folder in **Android Studio** (Ladybug or newer, JDK 17) and let Gradle sync, or run:
   ```
   ./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
   ./gradlew assembleRelease    # minified, signed with the debug key so it installs directly
   ```
2. Install on a phone with NFC (Android 10+).

## Features
- **Scan**: UID, chip type (NTAG213/215/216), memory meter, password status, decoded records (text, links, phone, email, SMS, geo, Wi‑Fi, vCard, app launch)
- **Library**: save tags, name/colour/icon them, enable/disable, scan counters, history
- **Action chains** (run in order on tap): open link, launch app, flashlight, alarm, timer, dial, SMS draft, notification, media control, ringer, Do Not Disturb, volume, clipboard, settings panels, maps, vibrate, webhook (GET/POST for Home Assistant / n8n), wait
- **Runs with the app closed**: write the *TagFlow link* onto a tag (Tag detail → Write TagFlow link) and a tap launches an invisible activity that runs the chain
- **Write**: text, link, call, email, SMS, location, Wi‑Fi, contact, app launch, TagFlow link — live byte-capacity meter, optional lock
- **Tools**: memory viewer, clone, erase, permanent read-only lock, 4-character NTAG password set/remove
- **Settings**: Material You dynamic colour, accent palettes, AMOLED black, haptics, JSON backup/restore

## Notes
- Password protection uses NTAG PWD_AUTH from page 4 onward. Write-only protection keeps the tag readable by everyone; "also require to read" hides content. **Remember the password** — there is no recovery.
- "Make read-only" is permanent.
- Android doesn't let apps toggle Wi‑Fi/Bluetooth directly, so those actions open the system panel instead.

## GitHub automation
`.github/workflows/android.yml` builds on every push, pull request and manual run, and uploads the debug and release APKs as artifacts (Actions tab → run → Artifacts). Push a tag such as `v1.0.0` (`git tag v1.0.0 && git push --tags`) and it also publishes a GitHub Release with the APK attached. `dependabot.yml` keeps the actions and Gradle dependencies up to date monthly.
The release APK is signed with the debug key; add your own keystore via repository secrets if you want to publish to the Play Store.
