# Maestro UI tests

This directory contains local Android UI flows for IndiaFOSS Chat. The suite
uses [Maestro](https://docs.maestro.dev/) to drive a connected Android device
or emulator from outside the app.

<!--- TOC -->

* [Prerequisites](#prerequisites)
* [Run the test suite](#run-the-test-suite)
  * [Required variables](#required-variables)
  * [Output](#output)
* [Write or update a flow](#write-or-update-a-flow)
* [Continuous integration](#continuous-integration)

<!--- END -->

## Prerequisites

- Install the [Maestro CLI](https://docs.maestro.dev/maestro-cli/).
- Install the Android SDK and ADB. Start an Android emulator or connect a
  physical device with USB debugging enabled.
- Build and install the app's debug variant. The current debug package is
  `org.indiafoss.chat.debug`. Maestro expects the app to be installed before
  the flow starts; it does not build or install this project.
- Use a disposable Matrix test account. The suite logs in, verifies the
  session, sends messages, creates and removes rooms, invites users, and logs
  out.

The [repository README](../README.md) covers the Android Studio `app` build
configuration. If you change application code, rebuild and reinstall the debug
variant before running these flows.

## Run the test suite

Run the command from the repository root. The suite loads
`.maestro/allTests.yaml` and its referenced flows and scripts.

### Required variables

Set these variables in your shell or in Maestro Studio before running the
suite. Keep passwords, recovery keys, and Matrix account data out of source
control. The example assumes the variables are already exported in your shell.

| Variable | Used for |
| --- | --- |
| `MAESTRO_APP_ID` | Debug package to launch: `org.indiafoss.chat.debug` |
| `MAESTRO_USERNAME` | Matrix test account login |
| `MAESTRO_PASSWORD` | Matrix test account password |
| `MAESTRO_RECOVERY_KEY` | Session verification |
| `MAESTRO_ROOM_NAME` | Existing joined room used by room-list and message flows |
| `MAESTRO_INVITEE1_MXID` | User invited by room and direct-message flows |
| `MAESTRO_INVITEE2_MXID` | Additional user invited by room flows |

Run all flows:

```shell
maestro test \
    -e MAESTRO_APP_ID=org.indiafoss.chat.debug \
    -e MAESTRO_USERNAME="$MAESTRO_USERNAME" \
    -e MAESTRO_PASSWORD="$MAESTRO_PASSWORD" \
    -e MAESTRO_RECOVERY_KEY="$MAESTRO_RECOVERY_KEY" \
    -e MAESTRO_ROOM_NAME="$MAESTRO_ROOM_NAME" \
    -e MAESTRO_INVITEE1_MXID="$MAESTRO_INVITEE1_MXID" \
    -e MAESTRO_INVITEE2_MXID="$MAESTRO_INVITEE2_MXID" \
    .maestro/allTests.yaml
```

### Output

Maestro prints test results to the console. Flow screenshots are written under
`build/maestro`.

## Write or update a flow

Flows are YAML files under `.maestro/tests`. Keep reusable flows deterministic
and leave the app in a known state. The complete suite intentionally clears
app state at startup and changes the disposable Matrix account and rooms.

Use [Maestro Studio](https://docs.maestro.dev/maestro-studio/) to inspect the
connected app and author flows interactively. Reinstall the debug app after
changing application code, then rerun the affected flow from the repository
root.

## Continuous integration

No GitHub Actions workflow currently runs these Maestro flows, and this
repository has no configured Maestro Cloud job. Run the suite locally with the
prerequisites above. The old Element-owned account, secret names, and workflow
path are not valid instructions for this fork.
