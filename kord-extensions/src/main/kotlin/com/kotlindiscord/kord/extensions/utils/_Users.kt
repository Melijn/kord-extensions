/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.contracts.contract

private const val DISCORD_USERS_URI = "https://discord.com/users"

/**
 * The user's Discord profile URL.
 */
public val User.profileLink: String
    get() = "$DISCORD_USERS_URI/$id/"

/**
 * The user's creation timestamp.
 */
public val User.createdAt: Instant
    get() = this.timeCreated.toInstant().toKotlinInstant()

/**
 * Send a private message to a user, if they have their DMs enabled.
 *
 * @param builder Builder lambda for populating the message fields.
 * @return The sent message, or `null` if the user has their DMs disabled.
 */
public suspend inline fun User.dm(builder: InlineMessage<MessageCreateData>.() -> Unit): Message? {
    return this.openPrivateChannel().await().sendMessage(MessageCreate { builder() }).await()
}

/**
 * Send a private message to a user, if they have their DMs enabled.
 *
 * @param content Message content.
 * @return The sent message, or `null` if the user has their DMs disabled.
 */
public suspend fun User.dm(content: String): Message? = this.dm { this.content = content }

/**
 * Check whether the given user is `null` or a bot.
 *
 * @receiver Nullable [User] to check.
 * @return `true` if the user is `null` or a bot, `false` otherwise.
 */
public fun User?.isNullOrBot(): Boolean {
    contract {
        returns(false) implies (this@isNullOrBot !== null)
    }

    return this == null || isBot
}
