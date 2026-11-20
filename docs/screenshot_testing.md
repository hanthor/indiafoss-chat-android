# Screenshot testing

<!--- TOC -->

* [Overview](#overview)
* [Setup](#setup)
* [Recording](#recording)
* [Verifying](#verifying)
* [Contributing](#contributing)

<!--- END -->

## Overview

- Screenshot tests record rendered screens and compare later runs against checked-in golden images.
- This fork uses [Paparazzi](https://github.com/cashapp/paparazzi) to render, record and verify Composables. Public and internal Composable previews are discovered through [ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner).
- Screenshot verification runs on every pull request in the `tests.yml` workflow.

## Setup

- Install [Git LFS](https://git-lfs.com/) and ensure `git lfs` is available.
- From the `indiafoss-chat-android` repository root, install the repository-local Git LFS hooks:

```shell
git lfs install --local
```

Git LFS stores the tracked PNG objects outside ordinary Git blobs. Once hooks are installed, use normal `git add`, `git commit` and `git push` commands; the remote LFS service transfers the objects.

## Recording

This fork has no GitHub Actions workflow that records screenshots. Record them locally, review the generated PNGs, and commit intended snapshot changes through Git LFS before opening a pull request.

From the repository root:

```shell
./gradlew recordPaparazziDebug
```

The repository's `removeOldSnapshots` task clears each module's `src/test/snapshots` directory before recording. Paparazzi writes the goldens under `tests/uitests/src/test/snapshots`; commit changed images with Git LFS.

## Verifying

Run the same verification task used by CI:

```shell
./gradlew :tests:uitests:verifyPaparazziDebug
```

When verification fails, Paparazzi writes expected, actual and diff images under `tests/uitests/build/paparazzi/failures`.

## Contributing

- Adding a `@Preview`-annotated Composable creates screenshot coverage through the preview scanner.
- Record and review the new snapshot, commit intended PNG changes through Git LFS, then run the verification task.
- `./tools/git/validate_lfs.sh` checks that files marked for LFS are stored through Git LFS; CI runs this check as part of its quality gates.
