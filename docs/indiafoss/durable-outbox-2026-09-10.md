# Durable outbox — step 1 of #48 (10 September 2026)

Companion spec: `docs/tasks/X-02-chat-durable-outbox.md`; architecture:
companion #194, "Delivery and seamless continuation". This note records what
step 1 ("persist outgoing intent and origin account/room; expose queued,
locally accepted, delivered and read without conflating them; preserve state
after process death") ships, and what it deliberately does not.

## The record

`libraries/outbox/api` — `OutboxRecord`, one per logical send, written before
the SDK is asked to send:

| Field | Meaning |
| --- | --- |
| `id` | Client-generated UUID, stable across restarts and retries. |
| `sessionId` | The account that sends. Never changes for a record. |
| `roomId` | The destination room. |
| `contentRef` | Kind (`TEXT`, `REPLY`, `MEDIA`, `OTHER`) and an optional media reference. No message body is stored or logged. |
| `transactionIds` | One idempotency key per route. Only `MATRIX_SEND_QUEUE` exists; the SDK chooses its transaction id. |
| `createdAtMillis`, `updatedAtMillis` | Wall-clock times from `SystemClock`. |
| `attempts` | Initial dispatch plus every retry observed or requested. |
| `state` | See below. |

Storage: `libraries/outbox/impl`, SQLDelight `OutboxDatabase` encrypted with
SQLCipher exactly like the session database (`OutboxEntry` +
`OutboxRouteTransaction`). Every write is one transaction. Nothing is written
anywhere else, so force-stopping the app leaves the record with the last
observed state.

## States

`OutboxState` is a sealed interface; `OutboxStateMachine.next(state, signal)`
is the only place that moves between them and is pure.

| State | Set when | Means |
| --- | --- | --- |
| `Queued` | Record persisted, before `Timeline.sendMessage` | Intent recorded; nothing handed to any route. |
| `LocallyAccepted` | SDK `NewLocalEvent` for our echo | The SDK send queue holds it and will try. |
| `ServerAccepted(eventId)` | SDK `SentEvent`, or the echo shows `Sent` | The homeserver took custody. Not recipient decryption, not delivery. |
| `Delivered(eventId, recipient)` | A recipient acknowledgement | No route provides one today; never reached. |
| `Read(eventId, reader)` | Another user's read receipt positioned at the event | Optional and distinct; never promotes anything below `ServerAccepted`. |
| `Failed(reason, retryable)` | Dispatch threw (retryable), or the SDK refused locally: unverified device, verified-user identity change, invalid MIME type, missing media (not retryable) | The route never accepted the request. |
| `Uncertain` | SDK `SendError` (the SDK does not say whether the request reached the server), or reconciliation finds no echo for a pending record after a 10 s grace | The outcome is unknown. Shown in words, with a retry that re-uses the same transaction id. |
| `Cancelled` | SDK `CancelledLocalEvent` before acceptance | Stopped trying. After `ServerAccepted`, cancellation changes nothing. |

Rules exercised by `OutboxStateMachineTest`: a read receipt never promotes a
pending or uncertain record; server acceptance never becomes `Delivered`
without a recipient receipt; a late `SentEvent` after `Uncertain` or `Failed`
wins; `Cancelled` cannot retract `ServerAccepted`.

## Integration with the SDK send queue

- `MessageComposerPresenter` wraps text and reply sends in `Outbox.send`. The
  record is inserted, then `Timeline.sendMessage`/`replyMessage` runs.
- `DefaultOutboxRoomTracker` (session scope) subscribes to
  `JoinedRoom.subscribeToSendQueueUpdates()` for every room opened and, at
  session start, for every room that still has unsettled records.
  `NewLocalEvent` is claimed by the oldest `Queued` record created in the last
  30 s; `SentEvent`, `SendError`, `RetrySendingEvent`, `CancelledLocalEvent`
  are applied by transaction id.
- `TimelinePresenter` reconciles the live timeline (own events only:
  transaction id, event id, `LocalEventSendState`, other users' receipts)
  with `Outbox.reconcile`. It never resends: a record whose echo the SDK
  still holds keeps or regains `LocallyAccepted`; one the SDK has no trace of
  becomes `Uncertain`.
- Retry: `Outbox.retry(id, SendHandle)` calls the SDK's own
  `SendHandle.retry()`, which re-sends the queued request with its existing
  transaction id. When the SDK no longer holds the echo the retry is refused
  (`NoRouteHandle`) and the user is told a new send could deliver twice.

## UI

`TimelineItem.Event.outbox: OutboxSummary?` is attached by transaction id or
event id. `TimelineItemReadReceiptView` prefers it over
`LocalEventSendState`: queued/locally accepted → sending circle; server
accepted → outline tick, described as "Accepted by the server"; delivered and
read → solid tick; failed/cancelled → nothing (the timestamp is red).
`TimelineEventTimestampView` shows a distinct "Delivery unknown" mark on every
uncertain message and taps it to retry on the same route. Existing preview
states are untouched (their `outboxState` is null); the two new previews
(`TimelineEventTimestampViewOutboxPreview`,
`TimelineItemReadReceiptViewOutboxPreview`) have goldens adopted from CI, see
below.

## Known limits of step 1

- The SDK owns the Matrix transaction id and only reports it through
  `NewLocalEvent`; correlation is by order within a 30 s window.
- Remote echoes lose their transaction id in the FFI mapping. If `SentEvent`
  is missed (room untracked while the SDK sent), the record becomes
  `Uncertain` even though the server accepted it, and it cannot be retried
  without a new send. Surfacing the transaction id on remote events in the
  SDK mapping would close this.
- `Delivered` cannot be reached: Matrix has no delivery receipt. Read
  receipts are only observed while positioned at the event.
- Media sends, reactions, polls and edits do not create records yet.
- A record whose dispatch failed before the SDK took it has no timeline echo
  and is therefore not visible; an outbox list is not part of this step.
- Thread timelines are tracked but not reconciled (partial view).

## Steps 2–5 of #48 still need

2. Explicit continuation through the other verified identity: a sheet naming
   the sender account, the destination and the duplicate risk, and a defined
   fate for the original pending record before an alternative is dispatched
   (a second `OutboxRoute` entry on the same record). Cancellation with the
   "may still be delivered" disclosure after `ServerAccepted`. The StartDM
   guard wording (companion review finding 6).
3. Automatic routing only with an accepted identity proof (companion #188)
   and recipient-specific evidence; a policy for ambiguous delivery.
4. Person-level inbox merging, opt-in, provenance preserved, no double
   counting.
5. Merged DM timelines with account/provenance and independent crypto
   verification visible; all relations keep their original room/event
   identity.

## Screenshot goldens

The 22 goldens for the two new previews were taken from the `tests-and-screenshot-tests-results` artifact of [CI run 34459558162](https://github.com/hanthor/indiafoss-chat-android/actions/runs/34459558162) (head `ecd8450d`), reviewed, and copied byte-for-byte under Git LFS, following `snapshot-recovery-2026-09-08.md`.

| Image | SHA-256 |
| --- | --- |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_0_en.png` | `dbec6d74587e7e0897a7cb2e614b4e661d94c53f88c3177686aa6a3e582bacd5` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_1_en.png` | `dbec6d74587e7e0897a7cb2e614b4e661d94c53f88c3177686aa6a3e582bacd5` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_2_en.png` | `f10cdd8ca44d2ca20a4a2a99a9847480f6059db98c8c52104efb018866ffe12c` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_3_en.png` | `d4abad9da7585d3890ac98ab3c1ad3bd1225ed4e8328cad513d3a94a24ebe353` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_4_en.png` | `8c66879c286a7f88e4b150cb9c2437380c40be4a9aec7194b06f62335d24212c` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_5_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_6_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Day_7_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_0_en.png` | `074c6afc0f73a606b0c8dd6c202ba50164aa64f5a5be717f33b9c8d83dd28098` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_1_en.png` | `074c6afc0f73a606b0c8dd6c202ba50164aa64f5a5be717f33b9c8d83dd28098` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_2_en.png` | `fd2cec1edd939372e4ea272e24f12e63d0e410a3b956b6e076b9cf38165574d0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_3_en.png` | `59e1708bf79817706bd971d6272aa0e6ea9d626ec53b6417b7d29b8e23be575c` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_4_en.png` | `f450b2353ac5648c963375a8f973584be2d07ff3d09b36ca914fe8884fb6fd5f` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_5_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_6_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components.receipt_TimelineItemReadReceiptViewOutbox_Night_7_en.png` | `3764d8bd7dc2783a8af43aad65a217d7e533ed17c4d4367b7994470bf35b62b0` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Day_0_en.png` | `0dbb5b304fe682974a66804c0d50590746a96e0e3ec8c525462a042ad8980a0c` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Day_1_en.png` | `975d7df023076903834fdb422a3315a6dd812a27a409aa65f386ef115f14de12` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Day_2_en.png` | `4e89559b69d483ffab3da4623afcda3d85abece9090b617c06ef2e37db4a52e1` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Night_0_en.png` | `ed4e95fa334c5d009db0b34f51a89c93541bcb55ea0861bf7c3524b1d3eaeba8` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Night_1_en.png` | `0b811d35287a8317948194bd6ecf9f61403fbce3e9f5322296ca67ca53b742c2` |
| `features.messages.impl.timeline.components_TimelineEventTimestampViewOutbox_Night_2_en.png` | `0dee9fccafdf8a33c60dd5cec0bd758ce4cb97046941b4c50edf52c0867d99d7` |
