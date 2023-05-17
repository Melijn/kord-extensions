/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("RedundantSuspendModifier")

package com.kotlindiscord.kord.extensions.checks

import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.utils.translate
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.Event

/**
 * Check asserting that the user an [Event] fired for has a given permission, or the Administrator permission.
 *
 * Only events that can reasonably be associated with a guild member are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param perm The permission(s) to check for.
 */
public suspend fun CheckContext<*>.userHasPermission(vararg perm: Permission) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.hasPermission")
    val channel = channelFor(event) as? GuildChannel
    val member = memberFor(event)

    if (member == null) {
        logger.nullMember(event)

        fail()
    } else {
        val missing = perm.firstOrNull {
            when {
                member.hasPermission(Permission.ADMINISTRATOR) -> false
                channel != null -> !member.hasPermission(channel, *perm)
                else -> !member.hasPermission(*perm)
            }
        }

        if (missing == null) {
            logger.passed()

            pass()
        } else {
            logger.failed("Member $member does not have permission $perm")

            fail(
                translate(
                    "checks.hasPermission.failed",
                    replacements = arrayOf(missing.translate(locale))
                )
            )
        }
    }
}

/**
 * Check asserting that the user an [Event] fired for **does not have** a given permission **or** the Administrator
 * permission.
 *
 * Only events that can reasonably be associated with a guild member are supported. Please raise
 * an issue if an event you expected to be supported, isn't.
 *
 * @param perm The permission(s) to check for.
 */
public suspend fun CheckContext<*>.userNoHasPermission(vararg perm: Permission) {
    if (!passed) {
        return
    }

    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.notHasPermission")
    val channel = channelFor(event) as? GuildChannel
    val member = memberFor(event)

    if (member == null) {
        logger.nullMember(event)

        pass()
    } else {
        val having = perm.firstOrNull {
            when {
                channel != null -> member.hasPermission(channel, *perm)
                else -> member.hasPermission(*perm)
            }
        }

        if (having == null) {
            logger.passed()

            pass()
        } else {
            logger.failed("Member $member has permission $perm")

            fail(
                translate(
                    "checks.notHasPermission.failed",
                    replacements = arrayOf(having.translate(locale)),
                )
            )
        }
    }
}
