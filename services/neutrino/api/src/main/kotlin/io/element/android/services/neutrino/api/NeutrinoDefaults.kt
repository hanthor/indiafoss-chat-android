/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.api

/**
 * Canonical constants describing how the app reaches the embedded Neutrino
 * homeserver, shared by every module that needs to construct its loopback
 * URL or force its localpart, independently of whether that module drives
 * the server's lifecycle via [NeutrinoService].
 *
 * The client-server API port is defined here rather than in the `impl` module
 * because the `impl` module's own `NEUTRINO_PORT` is `internal` — appnav's
 * login flow and enterprise's homeserver allow-list both need the same value
 * but sit outside `services.neutrino.impl` and must not depend on its
 * internals. Before this object existed, both call sites re-derived the
 * identical `"http://localhost:8008"` literal independently; nothing enforced
 * that they would change together if the port ever did.
 */
object NeutrinoDefaults {
    /**
     * Port the embedded homeserver's client-server API listens on. Must match
     * `NEUTRINO_PORT` in `services.neutrino.impl.NeutrinoNetwork` — the two
     * are independent constants (that one is `internal` to its module) but
     * are the same bind port by construction and must be changed together.
     */
    const val HOMESERVER_PORT: Int = 8008

    /**
     * The client-server API's base URL, reached over loopback since Neutrino
     * runs in-process. Correct regardless of emulation: this is not a host the
     * emulator needs to reach, unlike a normal homeserver URL.
     */
    const val HOMESERVER_URL: String = "http://localhost:$HOMESERVER_PORT"

    /**
     * The single forced user's localpart. The login flow auto-logs-in as this
     * localpart (see `RootFlowNode`'s headless login), and the embedded server
     * is started with it (see `DefaultNeutrinoService.start`).
     */
    const val LOCALPART: String = "n"
}
