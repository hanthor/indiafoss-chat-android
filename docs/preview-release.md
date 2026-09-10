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

## Release manifest

Every preview release attaches `release-manifest.json` next to the APK. It is the single record that maps the Neutrino fork commit to the neutrino-iroh commit, to the bindings `.aar` and its SHA-256, to the Chat commit, to the APK's identity, checksum and signing certificate, and to the CI runs that passed on that commit. `scripts/release-manifest.py` produces it in the publish workflow and verifies it again before upload; the same script validates a downloaded pair offline so the F-Droid repository ([hanthor/indiafoss-android-repo](https://github.com/hanthor/indiafoss-android-repo)) and reviewers can consume it.

The provenance half comes from `services/neutrino/impl/neutrino-provenance.json`, which is checked in next to the bindings pin. `check-provenance` refuses a build where that record, `gradle/libs.versions.toml` (`neutrino =`), the `neutrinoSha256` in `services/neutrino/impl/build.gradle.kts` and the `.aar` Gradle actually downloaded disagree, so bumping the pin means updating all three together. The revs must be full 40-character commit SHAs whose 7-character prefixes appear in the version string (`<upstream version>-e2ee.<neutrino rev>-ble.<neutrino-iroh rev>`); the Companion's `patches/neutrino/version.json` and the `neutrino-bindings-*` release body are the sources for them.

### Schema (`schemaVersion` 1)

| Field | Meaning |
| :--- | :--- |
| `schemaVersion` | `1`. `verify` rejects any other value. |
| `dryRun` | `false` for a publication. `true` marks the PR-CI dry run built from a debug-signed APK; `verify` refuses it unless `--allow-dry-run` is passed. |
| `generatedAt`, `generator` | UTC timestamp and the script that wrote the file. |
| `chat.repository`, `chat.commit`, `chat.url` | Source repository and full commit SHA the APK was built from. |
| `build.workflow`, `build.runId`, `build.runNumber`, `build.runAttempt`, `build.url` | The publish workflow run. `runNumber` is what the version code and name are derived from. |
| `neutrino.repository`, `neutrino.branch`, `neutrino.rev`, `neutrino.base`, `neutrino.url` | Homeserver crates fork (`hanthor/neutrino`) and the full commit the bindings were built against. |
| `neutrinoIroh.repository`, `neutrinoIroh.ref`, `neutrinoIroh.rev`, `neutrinoIroh.base`, `neutrinoIroh.url` | Bindings/BLE transport fork (`hanthor/neutrino-iroh`), its tag and full commit. |
| `bindings.version`, `bindings.artifact`, `bindings.sha256`, `bindings.source`, `bindings.builtBy`, `bindings.license` | The `.aar` release the build consumed. `bindings.verifiedAgainstDownload` is `true` when the build hashed the downloaded file itself; `verify` requires it for a publication. |
| `apk.filename`, `apk.sha256`, `apk.sizeBytes` | The attached file and its checksum (same value as `indiafoss-chat-android.apk.sha256`). |
| `apk.packageName`, `apk.versionCode`, `apk.versionName`, `apk.minSdkVersion` | Read from the APK's own binary `AndroidManifest.xml`, not from build parameters. |
| `apk.flavour`, `apk.buildType`, `apk.abi` | `fdroid` / `release` / `universal` for the preview. |
| `signing.certificateSha256` | SHA-256 of the signing certificate, read from the APK Signing Block and confirmed by `apksigner verify`. Must equal the preview certificate below. |
| `signing.schemes` | Signature schemes present (`v2`, `v3`, `v3.1`). |
| `signing.verifiedBy` | `apksigner` for a publication; `signing-block-only` is only possible in a dry run. |
| `evidence.ci[]` | One entry per required workflow (`Test`, `Code Quality Checks`, `APK Build`): `workflow`, `runId`, `url`, `headSha`, `conclusion`. Each must be a successful `push` run on `main` of `chat.commit`. |
| `evidence.device` | Always `status: "not-claimed"` with the tracking issues. CI establishes source, checksums and signature only; two-phone media, fresh-install/upgrade retention and recovery evidence live on #45 and #49, not in this file. |

### Verifying a download

```bash
curl -LO https://github.com/hanthor/indiafoss-chat-android/releases/download/nightly/indiafoss-chat-android.apk
curl -LO https://github.com/hanthor/indiafoss-chat-android/releases/download/nightly/release-manifest.json
python3 scripts/release-manifest.py verify \
  --apk indiafoss-chat-android.apk --manifest release-manifest.json \
  --expect-signer 137cde918f4b669ef9eea1985531af0f43b5dd98ca9db17032894370bddf5175 \
  --previous previously-accepted-release-manifest.json
```

`verify` needs only the Python standard library and works offline. It checks the APK's SHA-256 and size against the manifest, reads the package, `versionCode` and `versionName` from the APK's binary manifest and compares them, reads the signing certificate from the APK Signing Block and compares it with the manifest (and with `--expect-signer`), checks the provenance fields are well formed, and for a publication requires the CI evidence to be complete, successful and for the same commit. With `--previous` it additionally requires a strictly greater `versionCode`, a different `versionName`, the same package and the same signing certificate, so an update that could not install in place is refused. `apksigner` (found through `--apksigner`, `$APKSIGNER`, `$ANDROID_HOME` or `$ANDROID_SDK_ROOT`) cryptographically verifies the signature; without it the script fails unless `--no-apksigner` is passed explicitly, in which case it prints a warning that only the declared certificate was checked.

The publish workflow runs `check-provenance --aar` after the Gradle build, then `build` and `verify --expect-signer` (with `--previous` set to the manifest currently on the `nightly` release, when there is one) before anything is uploaded. PR CI (`APK Build`) runs the script's unit tests, `check-provenance` against the catalog and build script, `check-provenance --aar` after compiling release sources, and a `--dry-run` build/verify on the debug-signed F-Droid universal APK, uploaded as the `release-manifest-dry-run` artifact.

## Signing recovery

The dedicated Chat preview certificate SHA-256 is `137cde918f4b669ef9eea1985531af0f43b5dd98ca9db17032894370bddf5175`. It is separate from Companion signing and any future store production key.

GitHub secrets: `NIGHTLY_KEYSTORE_BASE64`, `NIGHTLY_KEYSTORE_PASSWORD`. Repository variables: `NIGHTLY_KEY_ALIAS`, `NIGHTLY_CERT_SHA256`. Missing or mismatched configuration fails publication; never generate a fallback runner key. Signing material is exposed only to the signing step and is not placed in Gradle caches or artifacts.

James (hanthor) owns the encrypted PKCS12 and password recovery copy in the maintainer's private signing directory. Keep a separate secure backup of both; never attach them to an issue or commit them. Restore the existing key when repairing CI. A replacement key cannot update an installed app in place unless an appropriate signing-key migration has been established.

CI signature checks are not a physical upgrade test. Retain two increasing-code APKs for the two-phone, in-place upgrade and account/key recovery rehearsal in #49. Public preview availability does not close those acceptance issues.

## Known limitations to disclose

- **Nearby-discovery hiding is unverified on hardware.** The bindings pin (`neutrino-bindings-0.8.2-e2ee.2d85348-ble.15117e9`) carries the `set_discoverable` FFI and the app calls it directly, so the first-run "Stay hidden" choice and the Advanced-settings toggle no longer show "not available in this build". Whether BLE advertising actually stops, stays stopped across a restart, and what that does to existing conversations' reachability has not been exercised on two physical devices (#47). Until it has, preview notes must not claim that hiding works — say that the control is wired but unverified.
