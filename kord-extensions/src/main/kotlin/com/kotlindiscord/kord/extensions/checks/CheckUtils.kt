/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.checks

import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.events.interfaces.*
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.GenericChannelEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.invite.GenericGuildInviteEvent
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.*
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.role.GenericRoleEvent
import net.dv8tion.jda.api.events.thread.GenericThreadEvent
import net.dv8tion.jda.api.events.thread.member.GenericThreadMemberEvent
import net.dv8tion.jda.api.events.user.GenericUserEvent
import net.dv8tion.jda.api.events.user.UserTypingEvent
import net.dv8tion.jda.api.events.user.update.GenericUserPresenceEvent
import net.dv8tion.jda.api.interactions.Interaction

/**
 * Retrieves a channel that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Channel] representing the channel, or null if there isn't one.
 */
public fun channelFor(event: Event): Channel? {
    return when (event) {
        is ChannelEvent -> event.channel

        is ChannelCreateEvent -> event.channel
        is ChannelDeleteEvent -> event.channel
        is GenericChannelEvent -> event.channel
        is GenericInteractionCreateEvent -> event.interaction.channel
        is GenericGuildInviteEvent -> event.channel
        is MessageBulkDeleteEvent -> event.channel
        is MessageReceivedEvent -> event.message.channel
        is MessageDeleteEvent -> event.channel
        is MessageUpdateEvent -> event.channel
        is GenericMessageReactionEvent -> event.channel
        is UserTypingEvent -> event.channel
        is GenericThreadEvent -> event.thread

        else -> null
    }
}

/**
 * Retrieves a channel that is the subject of a given event, if possible, returning the
 * parent if the channel is a thread.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Channel] representing the channel, or null if there isn't one.
 */
public fun topChannelFor(event: Event): Channel? {
    val channel = channelFor(event) ?: return null

    return if (channel is ThreadChannel) {
        channel.parentChannel
    } else {
        channel
    }
}

/**
 * Retrieves a channel ID representing a channel that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Long] representing the channel ID, or null if there isn't one.
 */
public fun channelIdFor(event: Event): Long? = channelFor(event)?.idLong

/**
 * Retrieves a guild that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Guild] representing the guild, or null if there isn't one.
 */
public fun guildFor(event: Event): Guild? {
    return when (event) {
        is GenericGuildEvent -> event.guild
        is MessageBulkDeleteEvent -> event.guild

        is MessageReceivedEvent ->  if (event.isFromGuild) event.guild else null
        is MessageDeleteEvent ->  if (event.isFromGuild) event.guild else null
        is MessageUpdateEvent ->  if (event.isFromGuild) event.guild else null
        is GenericChannelEvent ->  if (event.isFromGuild) event.guild else null
        is GenericMessageReactionEvent -> if (event.isFromGuild) event.guild else null
        is GenericInteractionCreateEvent -> if (event.isFromGuild) event.guild else null
        is UserTypingEvent -> event.guild

        else -> null
    }
}

/**
 * Retrieves a member that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Member] representing the member, or null if there isn't one.
 */
public fun memberFor(event: Event): Member? {
    return when (event) {
        is MemberEvent -> event.member

        is GenericInteractionCreateEvent -> event.member
        is GenericGuildMemberEvent -> event.member
        is MessageReceivedEvent -> event.member
        is MessageDeleteEvent -> null // TODO: could be fetched if a database is used
        is MessageUpdateEvent -> event.member
        is GenericMessageReactionEvent -> event.member
        is UserTypingEvent -> event.member
        is GenericThreadMemberEvent -> event.member

        else -> null
    }
}

/**
 * Retrieves a message that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Message] representing the message, or null if there isn't one.
 */
public fun messageFor(event: Event): Message? {
    return when (event) {
        is MessageEvent -> event.message

        is MessageReceivedEvent -> event.message
        is MessageDeleteEvent -> null
        is MessageUpdateEvent -> event.message
        is GenericComponentInteractionCreateEvent -> event.message
        is GenericMessageReactionEvent -> null

        else -> null
    }
}

/**
 * Retrieves a messageId that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Long] representing the messageId, or null if there isn't one.
 */
public fun messageIdFor(event: Event): Long? {
    return when (event) {
        is MessageEvent -> event.message?.idLong
        is GenericMessageEvent -> event.messageIdLong
        is GenericComponentInteractionCreateEvent -> event.message.idLong
        else -> null
    }
}

/**
 * Retrieves a role that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Role] representing the role, or null if there isn't one.
 */
public fun roleFor(event: Event): Role? {
    return when (event) {
        is RoleEvent -> event.role
        is GenericRoleEvent -> event.role

        else -> null
    }
}

/**
 * Retrieves a thread that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [ThreadChannel] representing the role, or null if there isn't one.
 */
public fun threadFor(event: Event): ThreadChannel? =
    channelFor(event) as? ThreadChannel

/**
 * Retrieves a user that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [Long] representing the user, or null if there isn't one.
 */
public fun userIdFor(event: Event): Long? {
    return when (event) {
        is UserEvent -> event.user?.idLong

        is GenericUserEvent -> event.user.idLong
        is GuildBanEvent -> event.user.idLong
        is GuildUnbanEvent -> event.user.idLong

        // We don't deal with selfbots, so we only want the first user - bots can't be in group DMs.
        is MessageReceivedEvent -> event.author.idLong
        is MessageUpdateEvent -> event.author.idLong

        is GenericInteractionCreateEvent -> event.interaction.user.idLong
        is GenericGuildMemberEvent -> event.user.idLong
        is GenericUserPresenceEvent -> event.member.user.idLong
        is GenericMessageReactionEvent -> event.userIdLong

        is GenericThreadMemberEvent -> event.threadMemberIdLong

        else -> null
    }
}

/**
 * Retrieves a user that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the channel to retrieve.
 * @return A [User] representing the user, or null if there isn't one.
 */
public fun userFor(event: Event): User? {
    return when (event) {
        is UserEvent -> event.user

        is GenericUserEvent -> event.user
        is GuildBanEvent -> event.user
        is GuildUnbanEvent -> event.user

        // We don't deal with selfbots, so we only want the first user - bots can't be in group DMs.
        is MessageReceivedEvent -> event.author
        is MessageUpdateEvent -> event.author

        is GenericInteractionCreateEvent -> event.interaction.user
        is GenericGuildMemberEvent -> event.user
        is GenericUserPresenceEvent -> event.member.user
        is GenericMessageReactionEvent -> event.user

        is GenericThreadMemberEvent -> event.member?.user

        else -> null
    }
}

/** Silence the current check by removing any message it may have set. **/
public fun CheckContext<*>.silence() {
    message = null
}

/**
 * Retrieves an interaction that is the subject of a given event, if possible.
 *
 * This function only supports a specific set of events - any unsupported events will
 * simply result in a `null` value. Please note that some events may support a
 * null value for this type of object, and this will also be reflected in the return
 * value.
 *
 * @param event The event concerning to the interaction to retrieve.
 * @return A [Interaction] representing the interaction, or null if there isn't one.
 */
public fun interactionFor(event: Event): Interaction? = (event as? GenericInteractionCreateEvent)?.interaction
