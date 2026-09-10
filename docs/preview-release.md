# Android preview downloads

Attendees can [download the universal APK](https://github.com/hanthor/indiafoss-chat-android/releases/download/nightly/indiafoss-chat-android.apk) without a GitHub login. [Release notes, checksums and signing metadata](https://github.com/hanthor/indiafoss-chat-android/releases/tag/nightly) accompany it. The Companion links to the same download.

The preview is the F-Droid build flavour, package `org.indiafoss.chat`, for Android 7.0 or later. It is not a store listing. Debug CI artifacts remain developer builds with the separate `.debug` package. Do not promise that debug account data automatically transfers to the preview.

## Publish

1. Merge a reviewed PR after all checks pass. APK Build now also runs on main.
2. Wait for Test, Code Quality Checks and APK Build to pass on that exact main commit.
3. Dispatch **Publish Android preview** on main. The workflow refuses other branches or commits without those passing main checks.
4. It builds the release APK without Gradle debug signing, signs with the protected preview key, verifies the certificate and alignment, and checks the package and actual universal version code.
5. It creates an immutable versioned preview first, then updates `nightly` and its stable APK URL. A failure before publication leaves existing downloads intact. If updating the rolling alias fails, the immutable release remains available for recovery.

The version code is `(20270000 + workflow run number) * 10` for the universal APK. Version names include the run number and source short SHA. Re-running a run preserves its code; dispatch a new run for an upgrade. Keep this workflow's run-number history, or explicitly move the base above the last published version before replacing it.

## Signing recovery

The dedicated Chat preview certificate SHA-256 is `137cde918f4b669ef9eea1985531af0f43b5dd98ca9db17032894370bddf5175`. It is separate from Companion signing and any future store production key.

GitHub secrets: `NIGHTLY_KEYSTORE_BASE64`, `NIGHTLY_KEYSTORE_PASSWORD`. Repository variables: `NIGHTLY_KEY_ALIAS`, `NIGHTLY_CERT_SHA256`. Missing or mismatched configuration fails publication; never generate a fallback runner key. Signing material is exposed only to the signing step and is not placed in Gradle caches or artifacts.

James (hanthor) owns the encrypted PKCS12 and password recovery copy in the maintainer's private signing directory. Keep a separate secure backup of both; never attach them to an issue or commit them. Restore the existing key when repairing CI. A replacement key cannot update an installed app in place unless an appropriate signing-key migration has been established.

CI signature checks are not a physical upgrade test. Retain two increasing-code APKs for the two-phone, in-place upgrade and account/key recovery rehearsal in #49. Public preview availability does not close those acceptance issues.

## Known limitations to disclose

- **Nearby-discovery hiding is unverified on hardware.** The bindings pin (`neutrino-bindings-0.8.2-e2ee.2d85348-ble.15117e9`) carries the `set_discoverable` FFI and the app calls it directly, so the first-run "Stay hidden" choice and the Advanced-settings toggle no longer show "not available in this build". Whether BLE advertising actually stops, stays stopped across a restart, and what that does to existing conversations' reachability has not been exercised on two physical devices (#47). Until it has, preview notes must not claim that hiding works — say that the control is wired but unverified.
