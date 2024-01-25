/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("RedundantSuspendModifier")

package com.kotlindiscord.kord.extensions.checks

import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.Event

// region: Entity DSL versions

/**
 * Check asserting that an [Event] fired within a given channel. If the event fired within a thread,
 * it checks the thread's parent channel instead.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inTopChannel(builder: suspend (T) -> Channel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inChannel")
    val eventChannel = topChannelFor(event)

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val channel = builder(event)

        if (eventChannel.id == channel.id) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is not the same as channel $channel")

            fail(
                translate(
                    "checks.inChannel.failed",
                    replacements = arrayOf(channel.asMention),
                )
            )
        }
    }
}

/**
 * Check asserting that an [Event] did **not** fire within a given channel. If the event fired within a thread,
 * it checks the thread's parent channel instead.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInTopChannel(builder: suspend (T) -> Channel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInChannel")
    val eventChannel = topChannelFor(event)

    if (eventChannel == null) {
        logger.nullChannel(event)

        pass()
    } else {
        val channel = builder(event)

        if (eventChannel.id != channel.id) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is the same as channel $channel")

            fail(
                translate(
                    "checks.notInChannel.failed",
                    replacements = arrayOf(channel.asMention)
                )
            )
        }
    }
}

// endregion

// region: Long versions

/**
 * Check asserting that an [Event] fired within a given channel. If the event fired within a thread,
 * it checks the thread's parent channel instead.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel Long to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inTopChannel(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inChannel")
    var channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel is ThreadChannel) {
        channel = channel.parentChannel
    }

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        inTopChannel { channel }
    }
}

/**
 * Check asserting that an [Event] did **not** fire within a given channel. If the event fired within a thread,
 * it checks the thread's parent channel instead.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel Long to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInTopChannel(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInChannel")
    var channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel is ThreadChannel) {
        channel = channel.parentChannel
    }

    if (channel == null) {
        logger.noChannelId(id)

        pass()
    } else {
        notInTopChannel { channel }
    }
}

// endregion
