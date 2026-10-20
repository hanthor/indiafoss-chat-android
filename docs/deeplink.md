# IndiaFOSS Chat deep links

<!--- TOC -->

* [Introduction](#introduction)
  * [Messenger links](#messenger-links)
  * [Conference and handoff links](#conference-and-handoff-links)
  * [Matrix links](#matrix-links)
* [Developer smoke test](#developer-smoke-test)

<!--- END -->

## Introduction

IndiaFOSS Chat accepts deep links for messenger navigation, Matrix permalinks,
and IndiaFOSS conference handoff. This page documents the links declared by
the fork's Android manifest and the developer smoke test for them.

This fork does **not** declare HTTP/HTTPS Android App Links. The upstream
Element asset-links URL and domain-verification commands are not part of this
app's deep-link contract.

### Messenger links

`elementx://open/` links navigate to a Matrix session. Path components are
URL-encoded when they contain reserved characters.

| Link | Destination |
| --- | --- |
| `elementx://open/@alice:example.org` | Session for `@alice:example.org` |
| `elementx://open/@alice:example.org/!room:example.org` | Room |
| `elementx://open/@alice:example.org/!room:example.org/$thread` | Thread |
| `elementx://open/@alice:example.org/!room:example.org/$thread/$event` | Event in thread |
| `elementx://open/@alice:example.org/!room:example.org//%24event` | Event without thread |

The same examples are in [`tools/adb/deeplink.sh`](../tools/adb/deeplink.sh).

### Conference and handoff links

The IndiaFOSS Companion uses these links to hand a Matrix target to Chat:

```text
indiafoss://chat?dm=%40alice%3Aexample.org
indiafoss://chat?join=%23indiafoss%3Aexample.org
indiafoss://friend?v=1&matrix_id=%40alice%3Aexample.org
```

`indiafoss://conference/<route>` opens the hosted Companion inside Chat. For
example:

```text
indiafoss://conference/activity/session-1
```

`activity`, `location`, and `speaker` routes can be handed to the native
Companion when it is installed. `event` and `booth` routes stay in the hosted
Companion.

### Matrix links

The messenger accepts Matrix custom-scheme links and Matrix permalinks:

```text
matrix:u/alice:example.org?action=chat
matrix:r/indiafoss:example.org?action=join
https://matrix.to/#/@alice:example.org
element://user/@alice:example.org
element://room/#indiafoss:example.org
```

`matrix:` and `element:` are Android intent schemes declared by the app.
`https://matrix.to` is classified when handled from the conference surface;
it is not an Android App Link that claims the URL from a browser.

## Developer smoke test

Prerequisites:

- Android SDK Platform Tools, which provide `adb`.
- A connected Android device or emulator with USB or wireless debugging
  enabled.
- An installed debug build. Its package identifier is
  `org.indiafoss.chat.debug`.

From a terminal with the device connected, open a session:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d 'elementx://open/@alice:example.org'
```

Open a conference route:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d 'indiafoss://conference/activity/session-1'
```

Android's `adb` documentation covers device connectivity and `am start`
intent commands. These custom-scheme tests do not use App Links verification
commands such as `pm set-app-links` or `pm verify-app-links`.
