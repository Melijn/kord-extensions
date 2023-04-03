/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.contexts.UnsafeMessageCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialMessageCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondPublic
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Like a standard message command, but with less safety features. **/
@UnsafeAPI
public class UnsafeMessageCommand(
    extension: Extension,
) : MessageCommand<UnsafeMessageCommandContext>(extension) {
    /** Initial response type. Change this to decide what happens when this message command action is executed. **/
    public var initialResponse: InitialMessageCommandResponse = InitialMessageCommandResponse.EphemeralAck

    override suspend fun call(event: MessageContextInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        emitEventAsync(UnsafeMessageCommandInvocationEvent(this, event))

        try {
            if (!runChecks(event, cache)) {
                emitEventAsync(
                    UnsafeMessageCommandFailedChecksEvent(
                        this,
                        event,
                        "Checks failed without a message."
                    )
                )
                return
            }
        } catch (e: DiscordRelayedException) {
            event.interaction.reply(
                MessageCreate {
                    settings.failureResponseBuilder(this, e.reason, FailureReason.ProvidedCheckFailure(e))
                }
            ).setEphemeral(true).await()

            emitEventAsync(UnsafeMessageCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        val response = when (val r = initialResponse) {
            is InitialMessageCommandResponse.EphemeralAck -> event.interaction.deferReply(true).await()
            is InitialMessageCommandResponse.PublicAck -> event.interaction.deferReply(false).await()

            is InitialMessageCommandResponse.EphemeralResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).setEphemeral(true).await()

            is InitialMessageCommandResponse.PublicResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).await()

            is InitialMessageCommandResponse.None -> null
        }

        val context = UnsafeMessageCommandContext(event, this, response, cache)

        firstSentryBreadcrumb(context)

        try {
            checkBotPerms(context)
        } catch (t: DiscordRelayedException) {
            emitEventAsync(UnsafeMessageCommandFailedChecksEvent(this, event, t.reason))
            respondText(context, t.reason, FailureReason.OwnPermissionsCheckFailure(t))

            return
        }

        try {
            body(context)
        } catch (t: Throwable) {
            if (t is DiscordRelayedException) {
                respondText(context, t.reason, FailureReason.RelayedFailure(t))
            }

            emitEventAsync(UnsafeMessageCommandFailedWithExceptionEvent(this, event, t))
            handleError(context, t)

            return
        }

        emitEventAsync(UnsafeMessageCommandSucceededEvent(this, event))
    }

    override suspend fun respondText(
        context: UnsafeMessageCommandContext,
        message: String,
        failureType: FailureReason<*>,
    ) {
        when (context.interactionResponse) {
            is InteractionHook -> context.respondPublic {
                settings.failureResponseBuilder(this, message, failureType)
            }

            else -> context.respondEphemeral {
                settings.failureResponseBuilder(this, message, failureType)
            }
        }
    }
}
