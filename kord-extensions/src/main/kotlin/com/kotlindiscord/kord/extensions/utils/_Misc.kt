/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Duration

/**
 * Run a block of code within a coroutine scope, defined by a given dispatcher.
 *
 * This is intended for use with code that normally isn't designed to be run within a coroutine, such as
 * database actions.
 *
 * @param dispatcher The dispatcher to use - defaults to [Dispatchers.IO].
 * @param body The block of code to be run.
 */
public suspend fun <T> runSuspended(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    body: suspend CoroutineScope.() -> T
): T = withContext(dispatcher, body)

/**
 * Returns `true` if any element in the `Flow` matches the given predicate. Consumes the `Flow`.
 */
public suspend inline fun <T : Any> Flow<T>.any(crossinline predicate: suspend (T) -> Boolean): Boolean =
    firstOrNull { predicate(it) } != null

/**
 * Convert the given [DateTimePeriod] to a [Duration] based on the given timezone, relative to the current system time.
 */
public fun DateTimePeriod.toDuration(timezone: TimeZone): Duration {
    val now = Clock.System.now()

    return now.plus(this, timezone) - now
}
