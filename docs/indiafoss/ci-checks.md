# CI checks required on pull requests

Every pull request runs three workflows: **Test** (`tests.yml`), **Code Quality Checks**
(`quality.yml`) and **APK Build** (`build.yml`). The job names below are the check names GitHub
shows on a PR; they are the names to list in the branch ruleset for `main`.

| Workflow | Job (check name) | What it runs | Expected duration (warm cache) |
| --- | --- | --- | --- |
| Test | `Unit tests` | `testDebugUnitTest`, then the merged Kover report and `koverVerifyAll` | 15–20 min |
| Test | `Screenshot tests` | `:tests:uitests:verifyPaparazziDebug` against the LFS golden images | 10–15 min |
| Code Quality Checks | `Android lint check` | `compileGplayDebugKotlin`, `compileFdroidDebugKotlin`, `lint*` | 8–12 min |
| Code Quality Checks | `Compose tests` | `assembleGplayDebug` with compiler reports, stability check | 8–12 min |
| Code Quality Checks | `Konsist tests` | `:tests:konsist:testDebugUnitTest` | 3–5 min |
| Code Quality Checks | `Detekt checks` | `detekt` | 3–5 min |
| Code Quality Checks | `Ktlint checks` | `ktlintCheck` | 3–5 min |
| Code Quality Checks | `Search for forbidden patterns` | `tools/check/check_code_quality.sh` | < 1 min |
| Code Quality Checks | `Search for invalid screenshot files` | `tools/test/checkInvalidScreenshots.py` | < 1 min |
| Code Quality Checks | `Search for invalid dependencies` | `tools/dependencies/checkDependencies.py` | 1–2 min |
| Code Quality Checks | `Doc checks` | `tools/docs/generate_toc.py --verify` | < 1 min |
| Code Quality Checks | `Check shell scripts` | shellcheck | < 1 min |
| Code Quality Checks | `Run zizmor` | zizmor over `.github/workflows` | < 1 min |
| APK Build | `Build APKs (debug)` | `:app:assembleGplayDebug`, `:app:assembleFDroidDebug` | 8–12 min |

`Build APKs (release)` (`bundleGplayRelease`) runs only on pushes to `main`, in the merge queue
and on manual dispatch, not on pull requests.

## How the cache works

`gradle/actions/setup-gradle` writes the Gradle dependency and build cache only on `main`
(`cache-read-only: ${{ github.ref != 'refs/heads/main' }}`); pull requests read it. The first run
after a change to `main` therefore rebuilds what changed, and pull requests opened afterwards get
cache hits for every module their diff does not touch. The upstream Element X workflows used
`refs/heads/develop` here, which never matched in this repository and left every run cold.

The configuration cache, build cache and parallel execution from `gradle.properties` are left on in
CI (no `--no-configuration-cache` / `--no-daemon`). If a job fails with a configuration-cache
problem report, add `--no-configuration-cache` to that job's Gradle invocation only, with a comment
naming the task that is incompatible.

A new push to a PR cancels that PR's running workflows; runs on `main` are never cancelled.
