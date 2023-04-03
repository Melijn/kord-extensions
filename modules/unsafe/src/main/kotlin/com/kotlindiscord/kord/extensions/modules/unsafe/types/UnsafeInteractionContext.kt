/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("StringLiteralDuplication")

package com.kotlindiscord.kord.extensions.modules.unsafe.types

import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.pagination.BaseButtonPaginator
import com.kotlindiscord.kord.extensions.pagination.PublicFollowUpPaginator
import com.kotlindiscord.kord.extensions.pagination.PublicResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.*

/** Interface representing a generic, unsafe interaction action context. **/
@UnsafeAPI
public interface UnsafeInteractionContext {
    /** Response created by acknowledging the interaction. Generic. **/
    public var interactionResponse: InteractionHook?

    /** Original interaction event object, for manual acks. **/
    public val event: GenericCommandInteractionEvent
}

/** Send an ephemeral ack, if the interaction hasn't been acknowledged yet. **/
@UnsafeAPI
public suspend fun UnsafeInteractionContext.ackEphemeral(
    builder: (suspend InlineMessage<MessageCreateData>.() -> Unit)? = null,
): InteractionHook {
    if (interactionResponse != null) {
        error("The interaction has already been acknowledged.")
    }

    return if (builder == null) {
        event.interaction.deferReply(true)
    } else {
        event.interaction.reply(MessageCreate { builder() }).setEphemeral(true)
    }.await()
}

/** Send a public ack, if the interaction hasn't been acknowledged yet. **/
@UnsafeAPI
public suspend fun UnsafeInteractionContext.ackPublic(
    builder: (suspend InlineMessage<MessageCreateData>.() -> Unit)? = null,
): InteractionHook {
    if (interactionResponse != null) {
        error("The interaction has already been acknowledged.")
    }

    return if (builder == null) {
        event.interaction.deferReply(false)
    } else {
        event.interaction.reply(MessageCreate { builder() }).setEphemeral(false)
    }.await()
}

/** Respond to the current interaction with an ephemeral followup, or throw if it isn't ephemeral. **/
@UnsafeAPI
public suspend inline fun UnsafeInteractionContext.respondEphemeral(
    builder: InlineMessage<MessageCreateData>.() -> Unit,
): Message = respond(builder, true)

/** Respond to the current interaction with a public followup. **/
@UnsafeAPI
public suspend inline fun UnsafeInteractionContext.respondPublic(
    builder: InlineMessage<MessageCreateData>.() -> Unit,
): Message = respond(builder, false)

@OptIn(UnsafeAPI::class)
public suspend inline fun UnsafeInteractionContext.respond(
    builder: InlineMessage<MessageCreateData>.() -> Unit,
    ephemeral: Boolean,
): Message {
    return when (val interaction = interactionResponse) {
        null -> error("Acknowledge the interaction before trying to follow-up.")
        else -> interaction.sendMessage(MessageCreate { builder() }).setEphemeral(ephemeral).await()
    }
}

/**
 * Edit the current interaction's response, or throw if it isn't public.
 */
@Suppress("UseIfInsteadOfWhen")
@UnsafeAPI
public suspend inline fun UnsafeInteractionContext.edit(
    builder: InlineMessage<MessageEditData>.() -> Unit,
): Message {
    return when (val interaction = interactionResponse) {
        null -> error("Acknowledge the interaction before trying to edit it.")
        else -> interaction.editOriginal(MessageEdit { builder() }).await()
    }
}

/** Create a paginator that edits the original interaction. **/
@UnsafeAPI
public suspend inline fun UnsafeInteractionContext.editingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit,
): BaseButtonPaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return when (val interaction = interactionResponse) {
        is InteractionHook -> PublicResponsePaginator(pages, interaction)

        null -> error("Acknowledge the interaction before trying to edit it.")
        else -> error("Unsupported initial interaction response type - please report this.")
    }
}

/** Create a paginator that creates a follow-up message, and edits that. **/
@Suppress("UseIfInsteadOfWhen")
@UnsafeAPI
public suspend inline fun UnsafeInteractionContext.respondingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit,
): BaseButtonPaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return when (val interaction = interactionResponse) {
        is InteractionHook -> PublicFollowUpPaginator(pages, interaction)

        null -> error("Acknowledge the interaction before trying to follow-up.")
        else -> error("Initial interaction response was not public.")
    }
}
