/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.commands.CommandContext
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import io.ktor.http.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.messages.MessageCreateData

private val logger = KotlinLogging.logger {}

private const val DELETE_DELAY = 1000L * 30L  // 30 seconds
private const val DISCORD_CHANNEL_URI = "https://discord.com/channels"

/** Message author's ID. **/
public val Message.authorId: Long
    get() = author.idLong

/** Whether the message author is a bot. **/
public val Message.authorIsBot: Boolean
    get() = author.isBot

/**
 * Respond to a message in the channel it was sent to, mentioning the author.
 *
 * @param useReply Whether to use Discord's replies feature to respond, instead of a mention. Defaults to `true`.
 * @param pingInReply When [useReply] is true, whether to also ping the user in the reply. Ignored if [useReply] is
 * false.
 * @param content Message content.
 *
 * @return The newly-created response message.
 */
public suspend fun Message.respond(content: String, useReply: Boolean = true, pingInReply: Boolean = true): Message =
    respond(useReply, pingInReply) { this.content = content }

/**
 * Respond to a message in the channel it was sent to, mentioning the author.
 *
 * @param useReply Whether to use Discord's replies feature to respond, instead of a mention. Defaults to `true`.
 * @param pingInReply When [useReply] is true, whether to also ping the user in the reply. Ignored if [useReply] is
 * false.
 * @param builder Builder lambda for populating the message fields.
 *
 * @return The newly-created response message.
 */
public suspend fun Message.respond(
    useReply: Boolean = true,
    pingInReply: Boolean = true,
    builder: suspend InlineMessage<MessageCreateData>.() -> Unit,
): Message {
    val author = this.author
    val innerBuilder: suspend InlineMessage<MessageCreateData>.() -> Unit = {
        builder()
        mentions {
            when {
                // TODO: jda-ktx InlineMentions doesn't implement this
                //  useReply && pingInReply -> repliedUser = true
                !pingInReply -> users.add(author.idLong)
            }
        }

        val mention = if (!useReply && channel !is PrivateChannel) {
            author.asMention
        } else {
            ""
        }

        val contentWithMention = "$mention ${content ?: ""}".trim()

        if (contentWithMention.isNotEmpty()) {
            content = contentWithMention
        }
    }
    val msgData = MessageCreateBuilder {
        innerBuilder()
    }.build()
    return if (useReply) {
        this.reply(msgData).await()
    } else {
        this.channel.sendMessage(msgData).await()
    }
}


/**
 * Check that this message happened in either the given channel or a DM, or that the author is at least a given role.
 *
 * If none of those things are true, a response message will be created instructing the user to try again in
 * the given channel.
 *
 * @param channel Channel to require the message to have been sent in
 * @param role Minimum role required to bypass the channel requirement, or null to disallow any role bypass
 * @param delay How long (in milliseconds) to wait before deleting the response message (30 seconds by default)
 * @param allowDm Whether to treat a DM as an acceptable context
 * @param deleteOriginal Whether to delete the original message, using the given delay (true by default)
 * @param deleteResponse Whether to delete the response, using the given delay (true by default)
 *
 * @return true if the message was posted in an appropriate context, false otherwise
 */
public suspend fun Message.requireChannel(
    context: CommandContext,
    channel: GuildMessageChannel,
    role: Role? = null,
    delay: Long = DELETE_DELAY,
    allowDm: Boolean = true,
    deleteOriginal: Boolean = true,
    deleteResponse: Boolean = true,
): Boolean {
    val topRole = GlobalScope.async(Dispatchers.Default, start = CoroutineStart.LAZY) {
        if (isFromGuild) {
            guild.retrieveMemberById(authorId).await().getTopRole()
        } else {
            null
        }
    }

    val messageChannel = this.channel

    @Suppress("UnnecessaryParentheses")  // In this case, it feels more readable
    if (
        (allowDm && messageChannel is PrivateChannel) ||
        (role != null && topRole.await() != null && topRole.await() >= role) ||
        channelId == channel.id
    ) return true

    val response = respond(
        context.translate("utils.message.useThisChannel", replacements = arrayOf(channel.mention))
    )

    if (deleteResponse) response.delete(delay)
    if (deleteOriginal && messageChannel !is PrivateChannel) this.delete(delay)

    return false
}

/**
 * Check that this message happened in a guild channel.
 *
 * If it didn't, a response message will be created instructing the user that the current command can't be used via a
 * private message.
 *
 * @param role Minimum role required to bypass the channel requirement, or null to disallow any role bypass
 *
 * @return true if the message was posted in an appropriate context, false otherwise
 */
public suspend fun Message.requireGuildChannel(
    context: CommandContext,
    role: Role? = null,
): Boolean {
    val author = this.author
    val guild = if (isFromGuild) guild else null

    val topRole = if (author != null && guild != null) {
        guild.retrieveMember(author).await().getTopRole()
    } else {
        null
    }

    @Suppress("UnnecessaryParentheses")  // In this case, it feels more readable
    if (
        (role != null && topRole != null && topRole >= role) ||
        channel !is PrivateChannel
    ) return true

    respond(context.translate("utils.message.commandNotAvailableInDm"))
    return false
}

/**
 * Check that this message happened in a guild channel.
 *
 * If it didn't, a response message will be created instructing the user that the current command can't be used via a
 * private message.
 *
 * As DMs do not provide access to members and roles, you'll need to provide a lambda that can be used to retrieve
 * the user's top role if you wish to make use of the role bypass.
 *
 * @param role Minimum role required to bypass the channel requirement, omit to disallow a role bypass
 * @param guild Guild to check for the user's top role, omit to disallow a role bypass
 *
 * @return true if the message was posted in an appropriate context, false otherwise
 */
public suspend fun Message.requireGuildChannel(
    context: CommandContext,
    role: Role? = null,
    guild: Guild? = null,
): Boolean {
    val author = this.author
    val topRole = guild?.getMemberById(author.id)?.getTopRole()

    @Suppress("UnnecessaryParentheses")  // In this case, it feels more readable
    if (
        (role != null && topRole != null && topRole >= role) ||
        channel !is PrivateChannel
    ) return true

    respond(context.translate("utils.message.commandNotAvailableInDm"))
    return false
}

/** Whether this message was published to the guilds that are following its channel. **/
public val Message.isPublished: Boolean
    get() = this.flags.contains(Message.MessageFlag.CROSSPOSTED)


/** Whether this message was sent from a different guild's followed announcement channel. **/
public val Message.isCrossPost: Boolean
    get() = this.flags.contains(Message.MessageFlag.IS_CROSSPOST)

/** Whether this message's embeds should be serialized. **/
public val Message.suppressEmbeds: Boolean
    get() = this.flags.contains(Message.MessageFlag.EMBEDS_SUPPRESSED)

/** When [isCrossPost], whether the source message has been deleted from the original guild. **/
public val Message.originalMessageDeleted: Boolean
    get() = this.flags.contains(Message.MessageFlag.SOURCE_MESSAGE_DELETED)

/** Whether this message came from Discord's urgent message system. **/
public val Message.isUrgent: Boolean
    get() = this.flags.contains(Message.MessageFlag.URGENT)

/**
 * Wait for a message, using the given timeout (in milliseconds ) and filter function.
 *
 * Will return `null` if no message is found before the timeout.
 */
public suspend fun waitForMessage(
    timeout: Long,
    filter: (suspend (MessageReceivedEvent).() -> Boolean) = { true },
): Message? {
    val kord = getKoin().get<ShardManager>()
    val event = kord.waitFor(timeout, filter)

    return event?.message
}

/**
 * Wait for a message from a user, using the given timeout (in milliseconds) and extra filter function.
 *
 * Will return `null` if no message is found before the timeout.
 */
public suspend fun User.waitForMessage(
    timeout: Long,
    filter: (suspend (MessageReceivedEvent).() -> Boolean) = { true },
): Message? {
    val user = this
    return waitForMessage(timeout) { filter(this) && message.author == user }
}

/**
 * Wait for a message in this channel, using the given timeout (in milliseconds) and extra filter function.
 *
 * Will return `null` if no message is found before the timeout.
 */
public suspend fun Channel.waitForMessage(
    timeout: Long,
    filter: (suspend (MessageReceivedEvent).() -> Boolean) = { true },
): Message? {
    val channel = this
    return waitForMessage(timeout) { filter(this) && message.channel == channel }
}

/**
 * Wait for a message in reply to this one, using the given timeout (in milliseconds) and extra filter function.
 *
 * Will return `null` if no message is found before the timeout.
 */
public suspend fun MessageBehavior.waitForReply(
    timeout: Long,
    filter: (suspend (MessageCreateEvent).() -> Boolean) = { true },
): Message? {
    val kord = getKoin().get<Kord>()
    val event = kord.waitFor<MessageCreateEvent>(timeout) {
        message.messageReference?.message?.id == id &&
            filter()
    }

    return event?.message
}

/**
 * Wait for a message by the user that invoked this command, in the channel it was invoked in, using the given
 * timeout (in milliseconds) and extra filter function.
 *
 * Will return `null` if no message is found before the timeout.
 */
public suspend fun CommandContext.waitForResponse(
    timeout: Long,
    filter: (suspend (MessageCreateEvent).() -> Boolean) = { true },
): Message? {
    val kord = com.kotlindiscord.kord.extensions.utils.getKoin().get<Kord>()
    val event = kord.waitFor<MessageCreateEvent>(timeout) {
        message.author?.id == getUser()?.id &&
            message.channelId == getChannel().id &&
            filter()
    }

    return event?.message
}
