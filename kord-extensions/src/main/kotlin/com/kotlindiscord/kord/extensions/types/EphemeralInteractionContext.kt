/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.types

import com.kotlindiscord.kord.extensions.pagination.EphemeralResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.PublicFollowUpPaginator
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.*

/** Interface representing an ephemeral-only interaction action context. **/
public interface EphemeralInteractionContext {
    /** Response created by acknowledging the interaction ephemerally. **/
    public val interactionResponse: InteractionHook
}

/**
 * Respond to the current interaction with an ephemeral followup.
 *
 * **Note:** Calling this twice (or at all after [edit]) will result in a public followup!
 */
public suspend inline fun EphemeralInteractionContext.respond(
    builder: InlineMessage<MessageCreateData>.() -> Unit
): Message {
    return interactionResponse.sendMessage(MessageCreate { builder() }).setEphemeral(true).await()
}

/** Respond to the current interaction with a public followup. **/
public suspend inline fun EphemeralInteractionContext.respondPublic(
    builder: InlineMessage<MessageCreateData>.() -> Unit
): Message = interactionResponse.sendMessage(MessageCreate { builder() }).setEphemeral(false).await()

/**
 * Edit the current interaction's response.
 */
public suspend inline fun EphemeralInteractionContext.edit(
    builder: InlineMessage<MessageEditData>.() -> Unit
): Message = interactionResponse.editOriginal(MessageEdit { builder() }).await()

/**
 * Create a paginator that edits the original interaction. This is the only option for an ephemeral interaction, as
 * it's impossible to edit an ephemeral follow-up.
 */
public inline fun EphemeralInteractionContext.editingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit
): EphemeralResponsePaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return EphemeralResponsePaginator(pages, interactionResponse)
}

/** Create a paginator that creates a follow-up message, and edits that. **/
public inline fun EphemeralInteractionContext.publicRespondingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit
): PublicFollowUpPaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return PublicFollowUpPaginator(pages, interactionResponse)
}
