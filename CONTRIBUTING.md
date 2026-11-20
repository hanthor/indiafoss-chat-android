# Contributing to IndiaFOSS Chat Android

IndiaFOSS Chat Android is an unofficial community fork of Element X Android.
It adds IndiaFOSS conference handoff, Companion integration, and Neutrino mesh
features. Read this page for fork-specific work; use the repository's detailed
references for architecture and release procedures.

## Read first

- [`AGENTS.md`](AGENTS.md) — repository rules, code style, PR title and label
  requirements, and common Gradle tasks.
- [`README.md`](README.md) — build prerequisites, fork scope, and the pinned
  Neutrino binding overview.
- [`docs/_developer_onboarding.md`](docs/_developer_onboarding.md) — Matrix,
  Rust SDK, module architecture, and upstream development concepts.
- [`docs/indiafoss/ci-checks.md`](docs/indiafoss/ci-checks.md) — required CI
  checks and their exact job names.

Do not replace these references with a second architecture guide. Update this
page only when the fork-specific workflow changes.

## Local setup

Install Android Studio with an Android SDK, JDK 21, Git, and Git LFS. The SDK
and JDK are needed for Gradle builds; Git LFS is needed when working with the
Paparazzi screenshot goldens. Android Studio's SDK Manager installs Android SDK
components. Keep the toolchain and Gradle caches outside temporary directories.

Open the repository in Android Studio and select the `app` configuration for a
normal app build. The project can also use the Gradle wrapper from its root.
For sandboxed or cache-complete local validation, the repository's documented
offline commands are:

```bash
./gradlew --offline assembleDebug
./gradlew --offline testDebugUnitTest
./gradlew --offline lintDebug
```

These commands require JDK 21 and a populated local Gradle/Android cache. An
offline failure caused by a missing cache is an environment limitation, not
proof that source changes are broken. The CI matrix in
[`docs/indiafoss/ci-checks.md`](docs/indiafoss/ci-checks.md) remains the source
of truth for the full validation set.

## Before opening a pull request

Run checks relevant to changed files. At minimum, documentation-only changes
should pass the repository's documentation verifier and changes that touch
source or tests should run the local quality script:

```bash
python3 ./tools/docs/generate_toc.py --verify ./*.md docs/**/*.md
./tools/check/check_code_quality.sh
```

For Kotlin, Android, screenshot, or release changes, follow the corresponding
commands in [`docs/indiafoss/ci-checks.md`](docs/indiafoss/ci-checks.md) and the
specialized guides below. Do not report a skipped local check as a pass.

- [`docs/_developer_onboarding.md`](docs/_developer_onboarding.md) — Android
  modules, Rust SDK work, and architecture orientation.
- [`docs/screenshot_testing.md`](docs/screenshot_testing.md) — Git LFS setup,
  recording, and verification of Paparazzi goldens.
- [`docs/integration_tests.md`](docs/integration_tests.md) — emulator and
  homeserver integration tests.
- [`.maestro/README.md`](.maestro/README.md) — current Maestro prerequisites,
  environment, flows, and output.
- [`docs/deeplink.md`](docs/deeplink.md) — deep-link routes and adb smoke
  checks.
- [`docs/notifications.md`](docs/notifications.md) — notification behavior and
  troubleshooting.
- [`docs/preview-release.md`](docs/preview-release.md) — preview APK
  publication, manifest verification, signing boundaries, and known limits.

## Neutrino bindings and provenance

The app consumes a pinned Neutrino `.aar` published by the IndiaFOSS Companion
repository. Gradle downloads the release artifact anonymously and checks its
SHA-256 before using it; do not substitute an upstream GitHub Packages
coordinate, a locally downloaded artifact with no provenance, or a different
release with the same filename.

A binding bump must keep these records consistent:

- `gradle/libs.versions.toml` — the `neutrino` version.
- `services/neutrino/impl/build.gradle.kts` — the expected `neutrinoSha256` and
  download task.
- `services/neutrino/impl/neutrino-provenance.json` — source repositories,
  full commit SHAs, release artifact, and checksum.

The build and release checks reject mismatches. Inspect the existing comments
and [`docs/preview-release.md`](docs/preview-release.md) before proposing a
bump. Never commit a replacement checksum without verifying the exact release
artifact and its source revisions.

## Change and pull-request workflow

1. Keep each branch focused on one change. Include tests or documentation that
   prove the changed behavior; do not broaden a fix into unrelated cleanup.
2. Use a sentence-style PR title that describes the changelog entry. Do not use
   Conventional Commit prefixes or put `Fixes #...` in the title.
3. Apply exactly one `PR-` label for changelog categorization.
4. Include screenshots or a screen recording for UI changes. Screenshot
   goldens belong in Git LFS; follow [`docs/screenshot_testing.md`](docs/screenshot_testing.md).
5. Explain validation in the PR body. Name checks that ran, checks that were
   skipped, and any required device or cache that was unavailable.
6. Keep secrets, signing keys, generated local configuration, and release
   credentials out of commits. If a report contains credentials or other
   sensitive data, contact repository maintainers before posting it publicly.

Ordinary contributors can change source, tests, and documentation. Preview
publication is a maintainer workflow because it uses protected signing secrets,
requires successful checks on the exact `main` commit, and updates the rolling
`nightly` release. Follow [`docs/preview-release.md`](docs/preview-release.md)
for release verification; do not attempt to recreate or replace the preview
signing key.

## Documentation changes

Keep links relative to this repository and update the nearest specialized guide
when behavior changes. If a Markdown file has TOC markers, regenerate or verify
its table of contents with:

```bash
python3 ./tools/docs/generate_toc.py --verify ./*.md docs/**/*.md
```

Use the exact CI check names in [`docs/indiafoss/ci-checks.md`](docs/indiafoss/ci-checks.md)
when describing validation. A documentation change does not establish that a
physical device, mesh transport, notification delivery, or upgrade path works;
those claims require the evidence described by the relevant guide and tracking
issues.
