/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.api

/**
 * Outcome of asking the embedded node to change whether it advertises this
 * user over the BLE mesh ([NeutrinoService.setDiscoverable]).
 *
 * The UI must only claim "hidden" on [Applied]: the other two outcomes mean the
 * node is still advertising exactly as before.
 */
sealed interface DiscoverableResult {
    /** The native node accepted the new state. */
    data object Applied : DiscoverableResult

    /**
     * The Neutrino bindings compiled into this build have no
     * `set_discoverable` entry point, so there is no way to stop advertising.
     * The node advertises as it always has.
     *
     * The pinned bindings (see `services/neutrino/impl/build.gradle.kts`) carry
     * the entry point and the real service calls it directly, so it never
     * produces this today. It stays in the contract so the UI keeps an honest
     * "not available" path should a future pin lack the binding again, and so
     * test fakes can exercise that path.
     */
    data object Unavailable : DiscoverableResult

    /** The binding exists but the call did not go through — e.g. the node is not running, or the FFI threw. */
    data class Failed(val reason: String) : DiscoverableResult
}

/**
 * Whether, after this result, the node's advertising state matches
 * [discoverable]. [DiscoverableResult.Unavailable] satisfies a request to be
 * discoverable — advertising is the node's default and needs no binding — but
 * never a request to hide.
 */
fun DiscoverableResult.satisfies(discoverable: Boolean): Boolean = when (this) {
    DiscoverableResult.Applied -> true
    DiscoverableResult.Unavailable -> discoverable
    is DiscoverableResult.Failed -> false
}

/**
 * Change the node's advertising state and persist the choice only once the node
 * has accepted it.
 *
 * This is the single home of the rule [NeutrinoService.setDiscoverable]
 * documents: a result other than [DiscoverableResult.Applied] (as judged by
 * [satisfies]) means the node is still advertising exactly as before, so
 * nothing may record or display a "hidden" the node is not delivering. On such
 * a result this throws [DiscoverabilityNotAppliedException] and [persist] is
 * never reached, which lets a caller surface the failure through an
 * `AsyncAction` without restating the guard.
 *
 * [persist] receives [discoverable] and writes whatever that caller stores — the
 * ordering guarantee is shared, the list of things to store is not.
 */
suspend fun NeutrinoService.applyDiscoverable(
    discoverable: Boolean,
    persist: suspend (Boolean) -> Unit,
) {
    val result = setDiscoverable(discoverable)
    if (!result.satisfies(discoverable)) {
        throw DiscoverabilityNotAppliedException(result)
    }
    persist(discoverable)
}

/**
 * Thrown by callers that need a [DiscoverableResult] other than
 * [DiscoverableResult.Applied] to fail a suspending flow (an `AsyncAction`).
 * The message is the human-readable reason, suitable for an error dialog.
 */
class DiscoverabilityNotAppliedException(val result: DiscoverableResult) : Exception(
    when (result) {
        DiscoverableResult.Applied -> "applied"
        DiscoverableResult.Unavailable -> "set_discoverable is not available in this build"
        is DiscoverableResult.Failed -> result.reason
    }
)
