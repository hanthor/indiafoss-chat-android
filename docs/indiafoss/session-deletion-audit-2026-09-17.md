# Session deletion audit — X-03 step 3 (17 September 2026)

Companion spec: `docs/tasks/X-03-chat-account-coordinator.md`, step 3: find every
path that deletes credentials, a session store or a crypto database, write down
what triggers it, and make sure no transient condition can reach one. This note
is that audit for the current code. It is a reading of the source, not a device
result; the device checks the spec lists remain owed under Chat #46.

## What "delete" means here

Two stores exist per account:

| Store | Where | Deleted by |
| --- | --- | --- |
| Credential row (`SessionData`: tokens, passphrase, paths) | SQLDelight `DatabaseSessionStore` | `SessionStore.removeSession` |
| Session directory: state store, **crypto store**, media cache | `filesDir/sessions/<uuid>` and `cacheDir/sessions/<uuid>` (`SessionPaths`) | `SessionPaths.deleteRecursively()` |

The mesh node's identity lives in `filesDir/data` (`DefaultNeutrinoService`,
`storageDir`). Nothing in the app deletes it, and it is outside every session
path, so no session operation can destroy the node key. The coupling is the
other way round: the server name is derived from that secret, so wiping
`filesDir/data` would orphan every stored `@n:` session row.

## Every path, and its trigger

| # | Path | Trigger | Row | Directory | Verdict |
| --- | --- | --- | --- | --- | --- |
| 1 | `RustMatrixClient.logout(userInitiated = true)` | Sign-out screens, `DefaultLogoutUseCase` (sign out all) | removed | deleted | User action. A session that cannot be restored is skipped, never removed. |
| 2 | `RustMatrixClient.deactivateAccount` | User deactivates the account | removed | deleted | User action, after the server confirmed. |
| 3 | `SignedOutPresenter` → `removeSession` | User taps *Sign in again* on the signed-out screen | removed | already gone (5) | User action. |
| 4 | `ClearCacheUseCase` | Developer settings / labs | kept | kept (only `cacheDir`, image and HTTP caches) | User action; never touches credentials or the crypto store. |
| 5 | `RustClientSessionDelegate.didReceiveAuthError` → `logout(userInitiated = false, ignoreSdkError = true)` | The homeserver reports the token invalid (hard or soft logout) | kept, `isTokenValid = false` | **deleted** | Server-initiated, not transient: the SDK raises this only on an authentication error, never on a transport failure. Still the one place a non-user event removes a crypto store; see *Open* below. |
| 6 | `RustMatrixAuthenticationService.rotateSessionPath()` | Every `setHomeserver()` and `loginWithQrCode()` | kept | **deleted the previous attempt's directory** | **Was a defect, fixed in this change.** The service kept pointing at the directory after the login succeeded and the session was stored, so any later `setHomeserver()` deleted a live crypto store while its row survived. |
| 7 | `ensureNotAlreadyLoggedIn` | Logging into an account already present | none | none | Logs the *new* client out; nothing local is removed. |
| 8 | `MatrixSessionCache.remove` / `removeAll` | Cache invalidation | none | none | In-memory only. |
| 9 | `DefaultSessionObserver.onSessionDeleted` listeners | Fires after a row is removed by 1–3 | — | session preferences, image loader, pusher, notification state, work manager | Cascade only; never a cause. |
| 10 | `DefaultCacheCleaner`, `DefaultBugReporter` | App start, bug report | none | `temp/media`, `temp/voice`, logs | Not session data. |

Restoration paths that fail without deleting anything: `RustMatrixAuthenticationService.restoreSession`
(throws `InvalidToken` / `MissingSession`), `RustMatrixClientFactory.create`
(a corrupt or locked store surfaces as a failed `Result`), `MatrixSessionCache.restore`
(logs), `RootFlowNode.tryToRestoreLatestSession` (routes to the not-logged-in flow),
`DefaultLogoutUseCase` (skips), `AppMigration09` (skips). Every notification path
(`DefaultPushHandler`, `DefaultNotifiableEventResolver`, the call and missed-call
resolvers) resolves the session through `getOrRestore` and returns a failure when
it cannot; none mutates the store.

## Transient conditions, specifically

- **Mesh transport unavailable / node fails to start.** `DefaultNeutrinoService.start`
  catches the native failure and logs; a later fatal error is read by
  `RootFlowNode` *before* `observeNavState`, which then returns. No session
  code runs at all. Cannot reach any row in the table.
- **No network.** Restoration does not need the network. The SDK's auth-error
  callback (5) is raised on an HTTP 401 with an unknown-token error, not on a
  connection failure.
- **Storage briefly locked.** `RustMatrixClientFactory.create` fails; nothing is
  deleted.
- **The embedded homeserver's auto-login firing twice** (`RootFlowNode.autoLoginToEmbeddedNeutrino`).
  This *was* reachable: each call ran `setHomeserver`, and through (6) the
  second call deleted the directory the first had just stored as a session.
  Fixed on both sides: the service forgets a directory once its session is
  stored, and the node keeps a single auto-login attempt in flight.

## Changed in this note's PR

- `RustMatrixAuthenticationService`: `sessionPaths` is cleared as soon as
  `sessionStore.addSession` succeeds, in every login flow. A directory that
  belongs to a stored session can no longer be rotated over. Tests:
  `RustMatrixAuthenticationServiceTest`, "setHomeserver after a login keeps the
  stored session's directories" (asserts on the files) and "setHomeserver
  replaces the directory of an attempt that never became a session".
- `RootFlowNode.autoLoginToEmbeddedNeutrino`: one attempt at a time.

## Open, deliberately not changed here

- **(5) still deletes the crypto store on a server auth error** with the row
  kept for the signed-out screen. Upstream Element X behaviour. The spec asks
  for an explicit attendee confirmation before a crypto store goes; that is a
  product change to the signed-out flow and belongs with the coordinator work
  under Chat #46, not with this fix.
- **An unrestorable latest session is a dead end**: `RootFlowNode` routes to
  the not-logged-in flow but the row stays, so `loggedInStateFlow` keeps
  reporting logged in and the auto-login never retries. Nothing is deleted, so
  it is safe, but it is stuck until the row is removed by hand. Owned by the
  coordinator's explicit error state (X-03 step 1).
- No per-account coordinator, no second concurrent session, no provisioning
  flow. Steps 1, 2 and 4–9 of X-03 are untouched.
