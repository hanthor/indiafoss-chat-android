# IndiaFOSS 2026 identity in Chat

Implements hanthor/indiafoss-chat-android#50 under the plan in
hanthor/indiafoss-companion#33. Companion and Chat are siblings: same event
palette and flat-glyph icon family, different silhouettes and grounds. Event
identity is confined to the surfaces below; everyday conversations, dialogs,
delivery and encryption states keep Compound semantic tokens and the neutral
or Material You accent from #41. Brand colours are never used as verification
or delivery signals.

## Where the identity appears

| Surface | Asset or change | Notes |
| --- | --- | --- |
| Launcher icon | `appicon/element/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`; per-variant `ic_launcher_background.xml` | Mint stepped-pixel speech bubble with three white lines on an ink ground (release), pale green (debug), mint (nightly). Themed-icon layer is the same bubble as a single-colour silhouette. Legacy `mipmap-*/ic_launcher*.webp` and `ic_launcher-playstore.png` are rendered from the same geometry (central 72dp on the release ground). |
| Splash | `app/src/main/res/values{,-night}/themes.xml` | `windowSplashScreenAnimatedIcon` shows the launcher icon on the theme canvas colour. |
| Notification tray | `libraries/designsystem/src/main/res/drawable/ic_notification.xml` | The bubble silhouette on a 24dp grid; Android tints it. |
| First run | `app/src/main/res/drawable{,-night}/onboarding_logo.xml` | Picked up by name by `OnBoardingLogoResIdProvider`. Glyph above the official IndiaFOSS 2026 wordmark; night variant is white with a pale-green frame. |
| Conference entry | `app/src/main/kotlin/io/element/android/x/conference/ConferenceHeader.kt`, `ConferenceEntry.kt` | Ink header, mint rule, "Open Companion" (native, via `ConferenceLinkDispatcher`) or "Open in browser" (PWA) decided by `ConferenceEntry.companionAction`. |
| Home overflow | `features/home/impl/.../indiafoss_strings.xml` | Menu entry reads "Open IndiaFOSS Companion". |
| Product name in copy | `appconfig/.../ApplicationConfig.kt` | `PRODUCTION_APPLICATION_NAME` is "IndiaFOSS Chat", so sign-in, invite and share text name this app rather than Element. The launcher label was already "IndiaFOSS Chat". |
| Settings › About | `features/preferences/impl/.../about/` | "IndiaFOSS artwork attribution" opens the pinned branding revision; the unofficial-project disclosure is shown on the screen. |

## Rendered checks

Vector renders of the shipped geometry (not device screenshots):
[launcher release / debug / monochrome](branding-2026/launcher-release-debug-monochrome.png),
[store icon / round legacy / notification](branding-2026/store-round-notification.png),
[first-run lockup day / night](branding-2026/first-run-lockup-day-night.png).

## Colours and type

Observed source values from the official 2026 stylesheet, used as fixed
event colours in both themes on the surfaces above only:

| Role | Value | Source |
| --- | --- | --- |
| Mint | `#0FB556` (`hsl(144 92% 37%)`) | Companion Material seed; official light primary |
| Ink | `#114B29` (`hsl(145 63% 18%)`) | Official dark green |
| Pale green | `#BAFCD6` (`hsl(145 92% 86%)`) | Official pale green |
| Wordmark | `#141414` / `#FFFFFF` | Official black/white wordmark assets |

Typography is Compound's (system sans). No font is bundled: the plan asks for
Inter on web and platform/Compound typography on Android, and Chat does not
carry FFF Forward or Press Start 2P. The pixel character comes from the
stepped glyph and the official wordmark paths, not from a display font.

## Asset provenance

All third-party artwork comes from https://github.com/fossunited/Branding at
revision `d55f3581f500041c9601be6713c9fb328e263c91` (default branch `main`,
committed 2026-06-09), licensed CC BY-SA 4.0 (`LICENSE` at that revision,
SHA-256 `23ee78c8bae49cf0…`). Retrieved 10 September 2026.

| Source file (at `d55f3581`) | SHA-256 (first 16) | Used in | Modifications |
| --- | --- | --- | --- |
| `asset/indiaFOSS/IndiaFOSS-2026-Black.svg` | `21cffdf9841c3291` | `onboarding_logo.xml` (day) | Paths copied verbatim into a vector drawable; none. |
| `asset/indiaFOSS/IndiaFOSS-2026-Black.svg` | `21cffdf9841c3291` | `onboarding_logo.xml` (night) | Same paths, fill `#FFFFFF`. (`IndiaFOSS-2026-White.svg`, `f1e87397f1dfac3e`, differs from Black only by fill and a 0.0001 coordinate rounding.) |
| `asset/indiaFOSS/Generic-IndiaFOSS.svg` | `5f0c57617646d731` | reference only | Not shipped. |
| `asset/FOSS United Logo/FOSS United Logo Black.svg` | `43d2e94aaa23b7cd` | reference only | Not shipped: Chat does not use the FOSS United mark. |

The speech-bubble glyph (launcher, monochrome, notification, header) is an
original mark drawn for this app in the stepped-pixel style of the 2026
wordmark and the three-bar motif of the Companion favicon; it copies no path
from the branding repository. Files that carry wordmark paths say so in their
header and remain CC BY-SA 4.0. Using these assets asserts no ownership of the
IndiaFOSS or FOSS United marks and no organiser endorsement; the unofficial
community-project disclosure stays in the README and in Settings › About.

Companion's counterpart record: `apps/web/static/branding/2026/README.md` in
hanthor/indiafoss-companion.

## Screenshot goldens affected

Changing the icon family alters existing Paparazzi previews. These goldens
must be re-recorded (CI artefact `tests-and-screenshot-tests-results`, reviewed
and copied as in `snapshot-recovery-2026-09-08.md`):

- `appicon.element_Icon_en.png`, `appicon.element_RoundIcon_en.png`, `appicon.element_MonochromeIcon_en.png`
- `libraries.designsystem.icons_IconsOther_{Day,Night}_0_en.png` (notification icon)
- `features.preferences.impl.about_AboutView_{Day,Night}_0_en.png` (attribution row and disclosure)

`ConferenceHeaderPreview` lives in the `app` module, which the screenshot
suite does not scan, so it adds no golden. Onboarding previews are unchanged:
the first-run lockup is resolved at runtime by resource name.

## Not claimed

No two-phone, TalkBack, large-text or venue-lighting pass was performed for
this change; icons were checked as rendered vectors only. Release artwork and
store listings are not part of this change. Nothing here advertises
cross-seam E2EE or calls.
