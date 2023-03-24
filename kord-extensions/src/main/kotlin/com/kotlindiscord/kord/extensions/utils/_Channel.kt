/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Ensure a webhook is created for the bot in a given channel, and return it.
 *
 * If a webhook already exists with the given name, it will be returned instead.
 *
 * @param channelObj Channel to create the webhook for.
 * @param name Name for the webhook
 * @param logoFormat Image.Format instance representing the format of the logo - defaults to PNG
 * @param logo Callable returning logo image data for the newly created webhook
 *
 * @return Webhook object for the newly created webhook, or the existing one if it's already there.
 */
public suspend fun ensureWebhook(
    channelObj: StandardGuildMessageChannel,
    name: String,
    logoFormat: Icon.IconType = Icon.IconType.PNG,
    logo: (suspend () -> ByteArray)? = null
): Webhook {
    val webhook = channelObj.retrieveWebhooks().await().firstOrNull { it.name == name }

    if (webhook != null) {
        return webhook
    }

    val guild = channelObj.guild

    logger.info { "Creating webhook for channel: #${channelObj.name} (Guild: ${guild.name}" }

    return channelObj.createWebhook(name)
        .setAvatar(logo?.let { Icon.from(it.invoke(), logoFormat) })
        .await()
}

/**
 * Given a guild channel, attempt to calculate the effective permissions for the member corresponding with
 * the given ID, checking the parent channel if this one happens to be a thread.
 *
 * @param memberId Member ID to calculate for
 */
public suspend fun GuildChannel.permissionsForMember(memberId: Long): EnumSet<Permission> = when (this) {
    is StandardGuildMessageChannel -> permissionsForMember(memberId)
    is ThreadChannel -> parentChannel.permissionsForMember(memberId)

    else -> error("Unsupported channel type for channel: $this")
}

/**
 * Given a guild channel, attempt to calculate the effective permissions for given user, checking the
 * parent channel if this one happens to be a thread.
 *
 * @param member Member to calculate for
 */
public suspend fun GuildChannel.permissionsForMember(member: Member): EnumSet<Permission> =
    permissionsForMember(member.idLong)

/**
 * Convenience function that returns the thread's parent message, if it was created from one.
 *
 * If it wasn't, or the parent channel can't be found, this function returns `null`.
 */
public suspend fun ThreadChannel.getParentMessage(): Message? {
    val parentChannel = parentMessageChannel ?: return null

    return parentChannel.retrieveMessageById(this.id).await()
}
