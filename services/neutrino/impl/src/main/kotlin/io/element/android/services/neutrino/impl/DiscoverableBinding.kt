/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.impl

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * The `set_discoverable(bool)` entry point of the Neutrino FFI, if this build's
 * bindings carry it.
 */
internal interface DiscoverableBinding {
    /** `true` when the bindings on the classpath expose the entry point. */
    val isAvailable: Boolean

    /**
     * Ask the node to advertise ([discoverable] = `true`) or hide. Throws
     * whatever the FFI throws; must only be called when [isAvailable].
     */
    fun setDiscoverable(discoverable: Boolean)
}

/**
 * Resolves the binding by name at runtime instead of calling it directly.
 *
 * `set_discoverable` was added to the FFI in hanthor/neutrino-iroh@15117e9 (PR
 * #15) as a top-level `#[uniffi::export]` in the `neutrino_ble` namespace, so
 * uniffi emits it as the static `io.element.neutrino.ble.Neutrino_bleKt
 * .setDiscoverable(boolean)`. The `.aar` this module is pinned to
 * (`neutrino-bindings-0.8.2-e2ee.2d85348`, see build.gradle.kts) predates that
 * commit and has no such method, so a direct call would not compile. Looking
 * it up reflectively lets the same code report an honest "not available in
 * this build" against the current `.aar` and start working the moment a
 * bindings release built from a ref containing 15117e9 is pinned. Once that
 * is the case, this class should be replaced by the direct call.
 */
internal class ReflectiveDiscoverableBinding(
    className: String = NEUTRINO_BLE_FACADE_CLASS,
) : DiscoverableBinding {
    private val method: Method? by lazy {
        try {
            Class.forName(className)
                .getMethod(METHOD_NAME, java.lang.Boolean.TYPE)
                .takeIf { Modifier.isStatic(it.modifiers) }
        } catch (ignored: ClassNotFoundException) {
            null
        } catch (ignored: NoSuchMethodException) {
            null
        }
    }

    override val isAvailable: Boolean
        get() = method != null

    override fun setDiscoverable(discoverable: Boolean) {
        val method = checkNotNull(method) { "set_discoverable is not available in this build" }
        try {
            method.invoke(null, discoverable)
        } catch (e: InvocationTargetException) {
            // Surface what the FFI threw, not the reflection wrapper.
            throw e.targetException ?: e
        }
    }

    companion object {
        const val NEUTRINO_BLE_FACADE_CLASS = "io.element.neutrino.ble.Neutrino_bleKt"
        const val METHOD_NAME = "setDiscoverable"
    }
}
