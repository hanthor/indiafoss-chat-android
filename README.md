# IndiaFOSS Chat (Element X Android · Neutrino)

> **Unofficial community fork.** Not produced or endorsed by Element or FOSS
> United. Element is a trademark of Element Creations Ltd; this app is renamed
> and re-identified (`org.indiafoss.chat`) and ships no Element branding.

[**Download the Android preview APK**](https://github.com/hanthor/indiafoss-chat-android/releases/download/nightly/indiafoss-chat-android.apk) · [Release notes and checksums](https://github.com/hanthor/indiafoss-chat-android/releases/tag/nightly) · [Open the Companion](https://hanthor.github.io/indiafoss-companion/)

Android 7.0 or later. Download the APK on your phone, open it and allow installation from your browser when prompted. Chat is a separate app from the Companion. This is a preview; physical-device mesh, background and recovery acceptance is tracked in #45 and #49. [Publishing and signing](docs/preview-release.md).

Fork of [element-x-android-neutrino](https://github.com/element-hq/element-x-android-neutrino)
(itself a P2P fork of Element X Android `v26.05.2` with the embedded
[Neutrino](https://github.com/element-hq/neutrino) homeserver), imported at
upstream commit `94cd8274`, aligned with the
[IndiaFOSS Companion](https://github.com/hanthor/indiafoss-companion):

- **Conference handoff links.** `indiafoss://chat?dm=@user:server`,
  `indiafoss://chat?join=#alias:server` and `indiafoss://friend?v=1…` cards
  (the companion's shared contract) are rewritten to `matrix.to` permalinks by
  `IndiafossLinks` and flow through Element's normal user-profile / join
  screens — nothing is messaged or joined without a tap. A friend card that
  only carries a Neutrino `server_name` resolves to the P2P address
  `@n:<server_name>`.
- **Conference inside the app.** `ConferenceActivity` hosts the offline-first
  companion PWA (schedule, ranking, itinerary, venue map, contact cards) and
  is reachable from the launcher shortcut "Conference" or
  `indiafoss://conference/<route>`. When the native Companion
  (`org.indiafoss.companion.nativeapp`) is installed, routes it understands
  (`activity`, `location`, `speaker`, home) open there instead. Links met in
  the WebView are classified by `ConferenceLinks`: `matrix:` and `matrix.to`
  links and `indiafoss://chat…`/`friend…` cards hand back to the messenger
  (a `matrix:` URI resolves locally, so it works offline), pages outside the
  companion origin open in the browser, and a missing handler is explained
  with a toast instead of a crash.
- **Event identity.** The launcher, splash, notification and first-run
  artwork and the conference entry carry the IndiaFOSS 2026 palette as a
  sibling of the Companion; conversations keep Compound's neutral or
  Material You accent. Asset provenance, licence and affected screenshot
  goldens: [docs/indiafoss/branding-2026.md](docs/indiafoss/branding-2026.md).
- **E2EE.** Public homeservers use Element X's Matrix encryption. The pinned
  Neutrino bindings implement mesh E2EE; installed-device interoperability,
  media and recovery still require the acceptance evidence in #45 and #49.

## Building

Same as upstream (Android Studio, JDK 21). The Neutrino bindings come from
GitHub Packages (`io.element.neutrino:bindings`, published by
`element-hq/neutrino-iroh`), which needs a token with `read:packages` even for
public packages: set `GITHUB_TOKEN` locally, and add the repository secret
`ELEMENT_BOT_TOKEN_NEUTRINO` (any PAT with `read:packages`) for the workflows
in `.github/workflows`.

---

# Element X Android (Neutrino)

P2P Fork of [Element X Android](https://github.com/element-hq/element-x-android), imported as a snapshot of `v26.05.2`.

Element X Android is a [Matrix](https://matrix.org/) client built on the [Matrix Rust SDK](https://github.com/matrix-org/matrix-rust-sdk), targeting devices running Android 7+. The UI layer is written using [Jetpack Compose](https://developer.android.com/jetpack/compose), and the navigation is managed using [Appyx](https://github.com/bumble-tech/appyx).

## Table of contents

<!--- TOC -->

* [Rust SDK](#rust-sdk)
* [Minimum SDK version](#minimum-sdk-version)
* [Build instructions](#build-instructions)
* [Copyright and License](#copyright-and-license)

<!--- END -->

## Rust SDK

Element X leverages the [Matrix Rust SDK](https://github.com/matrix-org/matrix-rust-sdk) through an FFI layer that the final client can directly import and use.

## Minimum SDK version

Element X Android requires a minimum SDK version of 24 (Android 7.0, Nougat).

## Build instructions

Just clone the project and open it in Android Studio. Make sure to select the
`app` configuration when building (as we also have sample apps in the project).

To build against a local copy of the Rust SDK, see the [Developer
onboarding](docs/_developer_onboarding.md#building-the-sdk-locally) instructions.

## Copyright and License

Copyright (c) 2025 - 2026 Element Creations Ltd.
Copyright (c) 2022 - 2025 New Vector Ltd.

This software is dual licensed by Element Creations Ltd (Element). It can be used either:

(1) for free under the terms of the GNU Affero General Public License (as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version); OR

(2) under the terms of a paid-for Element Commercial License agreement between you and Element (the terms of which may vary depending on what you and Element have agreed to).

Unless required by applicable law or agreed to in writing, software distributed under the Licenses is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.
