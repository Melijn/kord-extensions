/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.kotlindiscord.kord.extensions.checks

import mu.KLogger
import net.dv8tion.jda.api.events.Event

/** Convenience wrapper for a "failing check" log message. **/
public inline fun KLogger.failed(reason: String): Unit =
    debug { "Failing check: $reason" }

/** Convenience wrapper for a "passing check" log message. **/
public inline fun KLogger.passed(): Unit =
    debug { "Passing check." }

/** Convenience wrapper for a "passing check" log message. **/
public inline fun KLogger.passed(reason: String): Unit =
    debug { "Passing check: $reason" }

/** Convenience wrapper for a "channel for event is null" log message. **/
public inline fun KLogger.nullChannel(event: Event): Unit =
    debug { "Channel for event $event is null. This type of event may not be supported." }

/** Convenience wrapper for a "guild for event is null" log message. **/
public inline fun KLogger.nullGuild(event: Event): Unit =
    debug { "Guild for event $event is null. This type of event may not be supported." }

/** Convenience wrapper for a "member for event is null" log message. **/
public inline fun KLogger.nullMember(event: Event): Unit =
    debug { "Member for event $event is null. This type of event may not be supported." }

/** Convenience wrapper for a "message for event is null" log message. **/
public inline fun KLogger.nullMessage(event: Event): Unit =
    debug { "Message for event $event is null. This type of event may not be supported." }

/** Convenience wrapper for a "failing: no such channel" log message. **/
public inline fun KLogger.noChannelId(id: Long): Unit =
    failed("No such channel: $id")

/** Convenience wrapper for a "failing: no such category" log message. **/
public inline fun KLogger.noCategoryId(id: Long): Unit =
    failed("No such category: $id")

/** Convenience wrapper for a "failing: no such guild" log message. **/
public inline fun KLogger.noGuildId(id: Long): Unit =
    failed("No such guild: $id")

/** Convenience wrapper for a "failing: no such role" log message. **/
public inline fun KLogger.noRoleId(id: Long): Unit =
    failed("No such role: $id")
