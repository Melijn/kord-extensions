/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("RedundantSuspendModifier")

package com.kotlindiscord.kord.extensions.checks

import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import dev.minn.jda.ktx.generics.getChannel
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.Event

// region: Entity DSL versions

/**
 * Check asserting that an [Event] fired within a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inChannel(builder: suspend (T) -> Channel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inChannel")
    val eventChannel = channelFor(event)

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
 * Check asserting that an [Event] did **not** fire within a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInChannel(builder: suspend (T) -> Channel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInChannel")
    val eventChannel = channelFor(event)

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

/**
 * Check asserting that an [Event] fired within a given channel category.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the category to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inCategory(builder: suspend (T) -> Category) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inCategory")
    val eventChannel = topChannelFor(event)

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val category = builder(event)
        val channels = category.channels.toList().map { it.id }

        if (channels.contains(eventChannel.id)) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is not in category $category")

            fail(
                translate(
                    "checks.inCategory.failed",
                    replacements = arrayOf(category.name),
                )
            )
        }
    }
}

/**
 * Check asserting that an [Event] did **not** fire within a given channel category.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the category to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInCategory(builder: suspend (T) -> Category) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInCategory")
    val eventChannel = topChannelFor(event)

    if (eventChannel == null) {
        logger.nullChannel(event)

        pass()
    } else {
        val category = builder(event)
        val channels = category.channels.toList().map { it.id }

        if (channels.contains(eventChannel.id)) {
            logger.failed("Channel $eventChannel is in category $category")

            fail(
                translate(
                    "checks.notInCategory.failed",
                    replacements = arrayOf(category.name),
                )
            )
        } else {
            logger.passed()

            pass()
        }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is higher than a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelHigher(builder: suspend (T) -> GuildChannel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelHigher")
    val eventChannel = channelFor(event) as? GuildChannel

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val channel = builder(event)

        if (eventChannel > channel) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is lower than or equal to $channel")

            fail(
                translate(
                    "checks.channelHigher.failed",
                    replacements = arrayOf(channel.asMention),
                )
            )
        }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is lower than a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelLower(builder: suspend (T) -> GuildChannel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelLower")
    val eventChannel = channelFor(event) as? GuildChannel

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val channel = builder(event)

        if (eventChannel < channel) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is higher than or equal to $channel")

            fail(
                translate(
                    "checks.channelLower.failed",
                    replacements = arrayOf(channel.asMention),
                )
            )
        }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is higher than or equal to a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelHigherOrEqual(builder: suspend (T) -> GuildChannel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelHigherOrEqual")
    val eventChannel = channelFor(event) as? GuildChannel

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val channel = builder(event)

        if (eventChannel >= channel) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is lower than $channel")

            fail(
                translate(
                    "checks.channelHigherOrEqual.failed",
                    replacements = arrayOf(channel.asMention),
                )
            )
        }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is lower than or equal to a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param builder Lambda returning the channel to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelLowerOrEqual(builder: suspend (T) -> GuildChannel) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelLowerOrEqual")
    val eventChannel = channelFor(event) as? GuildChannel

    if (eventChannel == null) {
        logger.nullChannel(event)

        fail()
    } else {
        val channel = builder(event)

        if (eventChannel <= channel) {
            logger.passed()

            pass()
        } else {
            logger.failed("Channel $eventChannel is higher than $channel")

            fail(
                translate(

                    "checks.channelLowerOrEqual.failed",
                    replacements = arrayOf(channel.asMention),
                )
            )
        }
    }
}

// endregion

// region: Snowflake versions

/**
 * Check asserting that an [Event] fired within a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inChannel(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inChannel")
    val channel = event.jda.shardManager?.getGuildChannelById(id)
        ?: event.jda.shardManager?.getPrivateChannelById(id)

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        inChannel { channel }
    }
}

/**
 * Check asserting that an [Event] did **not** fire within a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInChannel(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInChannel")
    val channel = event.jda.shardManager?.getChannel(id)

    if (channel == null) {
        logger.noChannelId(id)

        pass()
    } else {
        notInChannel { channel }
    }
}

/**
 * Check asserting that an [Event] fired within a given channel category.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Category snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.inCategory(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inCategory")
    val category = event.jda.shardManager?.getCategoryById(id)

    if (category == null) {
        logger.noCategoryId(id)

        fail()
    } else {
        inCategory { category }
    }
}

/**
 * Check asserting that an [Event] did **not** fire within a given channel category.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Category snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.notInCategory(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notInCategory")
    val category = event.jda.shardManager?.getCategoryById(id)

    if (category == null) {
        logger.noCategoryId(id)

        pass()
    } else {
        notInCategory { category }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is higher than a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelHigher(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelHigher")
    val channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        channelHigher { channel }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is lower than a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelLower(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelLower")
    val channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        channelLower { channel }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is higher than or equal to a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelHigherOrEqual(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelHigherOrEqual")
    val channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        channelHigherOrEqual { channel }
    }
}

/**
 * Check asserting that the channel an [Event] fired in is lower than or equal to a given channel.
 *
 * Only events that can reasonably be associated with a single channel are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param id Channel snowflake to compare to.
 */
public suspend fun <T : Event> CheckContext<T>.channelLowerOrEqual(id: Long) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.channelLowerOrEqual")
    val channel = event.jda.shardManager?.getGuildChannelById(id)

    if (channel == null) {
        logger.noChannelId(id)

        fail()
    } else {
        channelLowerOrEqual { channel }
    }
}

// endregion
