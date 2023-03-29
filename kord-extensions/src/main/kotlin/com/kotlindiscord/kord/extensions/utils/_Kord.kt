/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import dev.minn.jda.ktx.events.listener
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Return the first received event that matches the condition.
 *
 * @param T Event to wait for.
 * @param timeout Time before returning null, if no match can be done. Set to null to disable it.
 * @param condition Function return true if the event object is valid and should be returned.
 */
public suspend inline fun <reified T : Event> ShardManager.waitFor(
    timeout: Duration? = null,
    noinline condition: (suspend T.() -> Boolean) = { true },
): T? = withTimeoutOrNull(timeout ?: Duration.INFINITE) {
    suspendCoroutine { continuation ->
        this@waitFor.listener<T> {
            if (condition(it)) {
                continuation.resume(it)
                this.cancel()
            }
        }
    }
}

/**
 * Return the first received event that matches the condition.
 *
 * @param T Event to wait for.
 * @param timeout Time in millis before returning null, if no match can be done. Set to null to disable it.
 * @param condition Function return true if the event object is valid and should be returned.
 */
public suspend inline fun <reified T : Event> ShardManager.waitFor(
    timeout: Long? = null,
    noinline condition: (suspend T.() -> Boolean) = { true },
): T? = waitFor(timeout?.milliseconds, condition)
